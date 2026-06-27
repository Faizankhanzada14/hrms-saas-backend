package com.hrms.auth.service;

import com.hrms.audit.AuditService;
import com.hrms.auth.dto.AuthRequests;
import com.hrms.auth.dto.AuthResponse;
import com.hrms.auth.dto.LoginRequest;
import com.hrms.exception.AccountLockedException;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.InvalidTokenException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.security.jwt.JwtProperties;
import com.hrms.security.jwt.JwtTokenProvider;
import com.hrms.user.entity.RefreshToken;
import com.hrms.user.entity.User;
import com.hrms.user.repository.RefreshTokenRepository;
import com.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> {
                    auditService.logFailure("LOGIN", "User", request.getUsername(), "User not found");
                    return new BadCredentialsException("Invalid username or password");
                });

        if (user.isAccountLocked()) {
            auditService.logFailure("LOGIN", "User", user.getUsername(), "Account is locked");
            throw new AccountLockedException("Account is locked due to too many failed login attempts. Contact an administrator.");
        }

        if (!user.isActive()) {
            auditService.logFailure("LOGIN", "User", user.getUsername(), "Account is inactive");
            throw new BadCredentialsException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.registerFailedLogin(jwtProperties.getMaxFailedAttempts());
            userRepository.save(user);
            auditService.logFailure("LOGIN", "User", user.getUsername(), "Invalid password");
            throw new BadCredentialsException("Invalid username or password");
        }

        user.resetFailedLogins();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = createRefreshToken(user);

        auditService.logSuccess("LOGIN", "User", user.getUsername(), "Login successful");

        return buildAuthResponse(user, accessToken, refreshTokenValue);
    }

    @Transactional
    public AuthResponse refresh(AuthRequests.RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or has been revoked"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired, please login again");
        }

        User user = token.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return buildAuthResponse(user, newAccessToken, token.getToken());
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    auditService.logSuccess("LOGOUT", "User", token.getUser().getUsername(), "Logout successful");
                });
    }

    @Transactional
    public void changePassword(String username, AuthRequests.ChangePasswordRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);

        auditService.logSuccess("CHANGE_PASSWORD", "User", username, "Password changed successfully");
    }

    @Transactional
    public void forgotPassword(AuthRequests.ForgotPasswordRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        auditService.logSuccess("FORGOT_PASSWORD", "User", user.getUsername(), "Password reset token generated");
        // Token delivery via email is handled by the notification module (out of scope here);
        // in production this token would be emailed, never returned in the API response.
    }

    @Transactional
    public void resetPassword(AuthRequests.ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (user.getPasswordResetTokenExpiry() == null
                || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUser(user);

        auditService.logSuccess("RESET_PASSWORD", "User", user.getUsername(), "Password reset successfully");
    }

    @Transactional
    public void lockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setAccountLocked(true);
        user.setLockedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.logSuccess("USER_LOCK", "User", user.getUsername(), "User locked by administrator");
    }

    @Transactional
    public void unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.unlock();
        userRepository.save(user);
        auditService.logSuccess("USER_UNLOCK", "User", user.getUsername(), "User unlocked by administrator");
    }

    private String createRefreshToken(User user) {
        String tokenValue = jwtTokenProvider.generateOpaqueRefreshToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000));
        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000)
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()))
                .build();
    }
}
