package com.hrms.user.service;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.DuplicateResourceException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.role.entity.Role;
import com.hrms.role.repository.RoleRepository;
import com.hrms.user.dto.UserDtos.AssignRolesRequest;
import com.hrms.user.dto.UserDtos.CreateUserRequest;
import com.hrms.user.dto.UserDtos.UserResponse;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setActive(true);
        user.setRoles(resolveRoles(request.getRoleNames()));

        User saved = userRepository.save(user);

        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdAndDeletedFalse(request.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));
            employee.setUser(saved);
            employeeRepository.save(employee);
        }

        auditService.logSuccess("USER_CREATE", "User", saved.getUsername(), "User account created");
        return toResponse(saved);
    }

    @Transactional
    public UserResponse assignRoles(Long userId, AssignRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setRoles(resolveRoles(request.getRoleNames()));
        User saved = userRepository.save(user);

        auditService.logSuccess("USER_ASSIGN_ROLES", "User", saved.getUsername(),
                "Roles set to " + request.getRoleNames());
        return toResponse(saved);
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = getUserOrThrow(userId);
        user.setActive(true);
        userRepository.save(user);
        auditService.logSuccess("USER_ACTIVATE", "User", user.getUsername(), "User activated");
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = getUserOrThrow(userId);
        user.setActive(false);
        userRepository.save(user);
        auditService.logSuccess("USER_DEACTIVATE", "User", user.getUsername(), "User deactivated");
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(getUserOrThrow(userId));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserResponse> getUsersByRole(String roleName) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName)))
                .map(this::toResponse)
                .toList();
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BadRequestException("At least one role must be specified");
        }
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            roles.add(role);
        }
        return roles;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserResponse toResponse(User user) {
        Employee linkedEmployee = employeeRepository.findByUser_IdAndDeletedFalse(user.getId()).orElse(null);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.isActive())
                .accountLocked(user.isAccountLocked())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .linkedEmployeeId(linkedEmployee != null ? linkedEmployee.getId() : null)
                .linkedEmployeeCode(linkedEmployee != null ? linkedEmployee.getEmployeeCode() : null)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
