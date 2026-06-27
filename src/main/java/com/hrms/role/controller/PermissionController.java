package com.hrms.role.controller;

import com.hrms.exception.DuplicateResourceException;
import com.hrms.role.dto.RoleDtos.PermissionResponse;
import com.hrms.role.entity.Permission;
import com.hrms.role.repository.PermissionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Fine-grained permissions that can be attached to roles")
public class PermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    @Operation(summary = "List all permissions")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        var responses = permissionRepository.findAll().stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId()).name(p.getName()).description(p.getDescription()).module(p.getModule())
                        .build())
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new permission")
    public ResponseEntity<PermissionResponse> createPermission(@RequestBody CreatePermissionRequest request) {
        if (permissionRepository.findAll().stream().anyMatch(p -> p.getName().equalsIgnoreCase(request.getName()))) {
            throw new DuplicateResourceException("Permission '" + request.getName() + "' already exists");
        }
        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permission.setModule(request.getModule());
        Permission saved = permissionRepository.save(permission);

        return ResponseEntity.status(HttpStatus.CREATED).body(PermissionResponse.builder()
                .id(saved.getId()).name(saved.getName()).description(saved.getDescription()).module(saved.getModule())
                .build());
    }

    @Getter
    @Setter
    public static class CreatePermissionRequest {
        @NotBlank(message = "Permission name is required")
        private String name;
        private String description;
        private String module;
    }
}
