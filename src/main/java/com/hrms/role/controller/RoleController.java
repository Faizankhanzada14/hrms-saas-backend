package com.hrms.role.controller;

import com.hrms.role.dto.RoleDtos.AssignPermissionsRequest;
import com.hrms.role.dto.RoleDtos.AssignUsersRequest;
import com.hrms.role.dto.RoleDtos.RoleRequest;
import com.hrms.role.dto.RoleDtos.RoleResponse;
import com.hrms.role.service.RoleService;
import com.hrms.user.dto.UserDtos.UserResponse;
import com.hrms.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Role Management", description = "Create/update/delete roles, assign permissions, and assign users")
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new role")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a role's name/description")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a role")
    public ResponseEntity<Void> deleteRole(Authentication authentication, @PathVariable Long id) {
        roleService.deleteRole(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a role by id, including its permissions")
    public ResponseEntity<RoleResponse> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRole(id));
    }

    @GetMapping
    @Operation(summary = "List all active roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Replace the set of permissions attached to a role")
    public ResponseEntity<RoleResponse> assignPermissions(@PathVariable Long id,
                                                            @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(roleService.assignPermissions(id, request));
    }

    @PostMapping("/{id}/users")
    @Operation(summary = "Assign this role to one or more users")
    public ResponseEntity<Void> assignUsers(@PathVariable Long id, @Valid @RequestBody AssignUsersRequest request) {
        roleService.assignUsers(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/users/{userId}")
    @Operation(summary = "Remove this role from a specific user")
    public ResponseEntity<Void> removeUser(@PathVariable Long id, @PathVariable Long userId) {
        roleService.removeUserFromRole(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roleName}/users")
    @Operation(summary = "List all users currently holding the given role")
    public ResponseEntity<List<UserResponse>> getUsersWithRole(@PathVariable String roleName) {
        return ResponseEntity.ok(userService.getUsersByRole(roleName));
    }
}
