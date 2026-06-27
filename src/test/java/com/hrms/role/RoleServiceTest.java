package com.hrms.role;

import com.hrms.audit.AuditService;
import com.hrms.exception.DuplicateResourceException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.role.dto.RoleDtos.AssignPermissionsRequest;
import com.hrms.role.dto.RoleDtos.RoleRequest;
import com.hrms.role.dto.RoleDtos.RoleResponse;
import com.hrms.role.entity.Permission;
import com.hrms.role.entity.Role;
import com.hrms.role.repository.PermissionRepository;
import com.hrms.role.repository.RoleRepository;
import com.hrms.role.service.RoleService;
import com.hrms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private RoleService roleService;

    @Test
    void createRole_withDuplicateName_throwsDuplicateResourceException() {
        RoleRequest request = new RoleRequest();
        request.setName("TEAM_LEAD");

        when(roleRepository.existsByName("TEAM_LEAD")).thenReturn(true);

        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(roleRepository, never()).save(any());
    }

    @Test
    void createRole_withUniqueName_savesSuccessfully() {
        RoleRequest request = new RoleRequest();
        request.setName("TEAM_LEAD");
        request.setDescription("Leads a small team");

        when(roleRepository.existsByName("TEAM_LEAD")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
            Role r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });

        RoleResponse response = roleService.createRole(request);

        assertThat(response.getName()).isEqualTo("TEAM_LEAD");
        verify(auditService).logSuccess(eq("ROLE_CREATE"), eq("Role"), eq("TEAM_LEAD"), any());
    }

    @Test
    void assignPermissions_withUnknownPermissionId_throwsResourceNotFoundException() {
        Role role = new Role();
        role.setId(5L);
        role.setName("TEAM_LEAD");

        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setPermissionIds(Set.of(100L));

        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.assignPermissions(5L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignPermissions_withValidIds_attachesPermissionsToRole() {
        Role role = new Role();
        role.setId(5L);
        role.setName("TEAM_LEAD");

        Permission permission = new Permission();
        permission.setId(100L);
        permission.setName("LEAVE_APPROVE");

        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setPermissionIds(Set.of(100L));

        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(100L)).thenReturn(Optional.of(permission));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleResponse response = roleService.assignPermissions(5L, request);

        assertThat(response.getPermissions()).hasSize(1);
        assertThat(response.getPermissions().get(0).getName()).isEqualTo("LEAVE_APPROVE");
    }
}
