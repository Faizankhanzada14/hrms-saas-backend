package com.hrms.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

public class RoleDtos {

    @Getter
    @Setter
    public static class RoleRequest {
        @NotBlank(message = "Role name is required")
        private String name;

        private String description;
    }

    @Getter
    @Setter
    public static class AssignPermissionsRequest {
        @NotEmpty(message = "At least one permission id is required")
        private Set<Long> permissionIds;
    }

    @Getter
    @Setter
    public static class AssignUsersRequest {
        @NotEmpty(message = "At least one user id is required")
        private Set<Long> userIds;
    }

    @Getter
    @Setter
    @Builder
    public static class RoleResponse {
        private Long id;
        private String name;
        private String description;
        private List<PermissionResponse> permissions;
    }

    @Getter
    @Setter
    @Builder
    public static class PermissionResponse {
        private Long id;
        private String name;
        private String description;
        private String module;
    }
}
