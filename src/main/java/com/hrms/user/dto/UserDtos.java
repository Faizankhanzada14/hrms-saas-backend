package com.hrms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

public class UserDtos {

    @Getter
    @Setter
    public static class CreateUserRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        private String email;

        @NotBlank(message = "Initial password is required")
        private String password;

        private String fullName;

        @NotEmpty(message = "At least one role is required")
        private Set<String> roleNames;

        /** Optionally link this new login to an existing employee profile. */
        private Long employeeId;
    }

    @Getter
    @Setter
    public static class AssignRolesRequest {
        @NotEmpty(message = "At least one role is required")
        private Set<String> roleNames;
    }

    @Getter
    @Setter
    @Builder
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private boolean active;
        private boolean accountLocked;
        private Set<String> roles;
        private Long linkedEmployeeId;
        private String linkedEmployeeCode;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
    }
}
