package com.hrms.auth;

import com.hrms.audit.AuditService;
import com.hrms.auth.dto.AuthResponse;
import com.hrms.auth.dto.LoginRequest;
import com.hrms.auth.service.AuthService;
import com.hrms.exception.AccountLockedException;
import com.hrms.security.jwt.JwtProperties;
import com.hrms.security.jwt.JwtTokenProvider;
import com.hrms.user.entity.User;
import com.hrms.user.repository.RefreshTokenRepository;
import com.hrms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuditService auditService;

    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setMaxFailedAttempts(5);
        jwtProperties.setAccessTokenExpirationMs(900_000);
        jwtProperties.setRefreshTokenExpirationMs(604_800_000);

        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtTokenProvider, jwtProperties, auditService);

        user = new User();
        user.setUsername("jdoe");
        user.setPassword("hashed-password");
        user.setActive(true);
        user.setAccountLocked(false);
    }

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest("jdoe", "plainPassword");

        when(userRepository.findByUsernameAndDeletedFalse("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPassword", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token-value");
        when(jwtTokenProvider.generateOpaqueRefreshToken()).thenReturn("refresh-token-value");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token-value");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.getUsername()).isEqualTo("jdoe");
        verify(auditService).logSuccess(eq("LOGIN"), eq("User"), eq("jdoe"), any());
    }

    @Test
    void login_withWrongPassword_throwsBadCredentialsAndRegistersFailure() {
        LoginRequest request = new LoginRequest("jdoe", "wrongPassword");

        when(userRepository.findByUsernameAndDeletedFalse("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository).save(user);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_withLockedAccount_throwsAccountLockedException() {
        user.setAccountLocked(true);
        LoginRequest request = new LoginRequest("jdoe", "plainPassword");

        when(userRepository.findByUsernameAndDeletedFalse("jdoe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void login_withUnknownUsername_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("ghost", "anyPassword");
        when(userRepository.findByUsernameAndDeletedFalse("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
