package com.hrms.role.service;

import com.hrms.audit.AuditService;
import com.hrms.exception.DuplicateResourceException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.role.dto.RoleDtos.AssignPermissionsRequest;
import com.hrms.role.dto.RoleDtos.AssignUsersRequest;
import com.hrms.role.dto.RoleDtos.PermissionResponse;
import com.hrms.role.dto.RoleDtos.RoleRequest;
import com.hrms.role.dto.RoleDtos.RoleResponse;
import com.hrms.role.entity.Permission;
import com.hrms.role.entity.Role;
import com.hrms.role.repository.PermissionRepository;
import com.hrms.role.repository.RoleRepository;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("A role named '" + request.getName() + "' already exists");
        }
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        auditService.logSuccess("ROLE_CREATE", "Role", saved.getName(), "Role created");
        return toResponse(saved);
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = getRoleOrThrow(id);

        if (!role.getName().equalsIgnoreCase(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("A role named '" + request.getName() + "' already exists");
        }
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        auditService.logSuccess("ROLE_UPDATE", "Role", saved.getName(), "Role updated");
        return toResponse(saved);
    }

    @Transactional
    public void deleteRole(String performedBy, Long id) {
        Role role = getRoleOrThrow(id);
        role.markDeleted(performedBy);
        roleRepository.save(role);
        auditService.logSuccess("ROLE_DELETE", "Role", role.getName(), "Role soft-deleted by " + performedBy);
    }

    @Transactional
    public RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = getRoleOrThrow(roleId);

        Set<Permission> permissions = new HashSet<>();
        for (Long permissionId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));
            permissions.add(permission);
        }
        role.setPermissions(permissions);

        Role saved = roleRepository.save(role);
        auditService.logSuccess("ROLE_ASSIGN_PERMISSIONS", "Role", saved.getName(),
                "Permissions set to " + request.getPermissionIds());
        return toResponse(saved);
    }

    @Transactional
    public void assignUsers(Long roleId, AssignUsersRequest request) {
        Role role = getRoleOrThrow(roleId);

        for (Long userId : request.getUserIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            user.getRoles().add(role);
            userRepository.save(user);
        }

        auditService.logSuccess("ROLE_ASSIGN_USERS", "Role", role.getName(),
                "Assigned to users " + request.getUserIds());
    }

    @Transactional
    public void removeUserFromRole(Long roleId, Long userId) {
        Role role = getRoleOrThrow(roleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.getRoles().removeIf(r -> r.getId().equals(role.getId()));
        userRepository.save(user);
        auditService.logSuccess("ROLE_REMOVE_USER", "Role", role.getName(), "Removed from user id " + userId);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(Long id) {
        return toResponse(getRoleOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .filter(r -> !r.isDeleted())
                .map(this::toResponse)
                .toList();
    }

    private Role getRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(p -> PermissionResponse.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .description(p.getDescription())
                                .module(p.getModule())
                                .build())
                        .toList())
                .build();
    }
}
