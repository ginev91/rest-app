package org.example.main.controller.user;

import org.example.main.dto.request.role.RoleChangeRequestDto;
import org.example.main.dto.request.user.BlockUserRequestDto;
import org.example.main.dto.response.user.UserProfileResponseDto;
import org.example.main.model.role.Role;
import org.example.main.model.user.User;
import org.example.main.service.role.IRoleService;
import org.example.main.service.user.IUserService;
import org.example.main.mapper.role.RoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    IUserService userService;

    @Mock
    IRoleService roleService;

    @Mock
    RoleMapper roleMapper;

    @InjectMocks
    AdminUserController controller;

    // helper to extract username from either UserProfileResponseDto or User (or via reflection)
    private String extractUsername(Object o) {
        if (o == null) return null;
        if (o instanceof UserProfileResponseDto) return ((UserProfileResponseDto) o).getUsername();
        if (o instanceof User) return ((User) o).getUsername();
        try {
            Method m = o.getClass().getMethod("getUsername");
            Object v = m.invoke(o);
            return v != null ? v.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractRole(Object o) {
        if (o == null) return null;
        if (o instanceof UserProfileResponseDto) return ((UserProfileResponseDto) o).getRole();
        if (o instanceof User) {
            Role r = ((User) o).getRole();
            return r != null ? r.getName() : null;
        }
        try {
            Method m = o.getClass().getMethod("getRole");
            Object v = m.invoke(o);
            if (v == null) return null;
            // if it's a Role object
            if (v instanceof Role) return ((Role) v).getName();
            return v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean extractBlocked(Object o) {
        if (o == null) return null;
        if (o instanceof UserProfileResponseDto) return ((UserProfileResponseDto) o).getBlocked();
        if (o instanceof User) return ((User) o).getBlocked();
        try {
            Method m = o.getClass().getMethod("getBlocked");
            Object v = m.invoke(o);
            return v == null ? null : Boolean.valueOf(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    void listUsers_returnsUsersFromService() {
        User u1 = new User(); u1.setId(UUID.randomUUID()); u1.setUsername("a");
        User u2 = new User(); u2.setId(UUID.randomUUID()); u2.setUsername("b");

        when(userService.findAll()).thenReturn(List.of(u1, u2));

        ResponseEntity<?> resp = controller.listUsers();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        List<?> body = (List<?>) resp.getBody();
        assertThat(body).hasSize(2);

        List<String> names = body.stream().map(this::extractUsername).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("a", "b");

        verify(userService).findAll();
    }

    @Test
    void changeRole_callsAssignRole_and_returnsUpdatedUser() {
        UUID uid = UUID.randomUUID();
        RoleChangeRequestDto req = new RoleChangeRequestDto();
        req.setRoleName("ROLE_EMPLOYEE");

        User updated = new User();
        updated.setId(uid);
        Role r = new Role(); r.setName("ROLE_EMPLOYEE");
        updated.setRole(r);

        when(userService.assignRole(eq(uid), eq("ROLE_EMPLOYEE"))).thenReturn(updated);

        ResponseEntity<?> resp = controller.changeRole(uid, req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Object body = resp.getBody();
        String roleName = extractRole(body);
        assertThat(roleName).isEqualTo("ROLE_EMPLOYEE");
        verify(userService).assignRole(uid, "ROLE_EMPLOYEE");
    }

    @Test
    void deleteUser_deletesUser() {
        UUID uid = UUID.randomUUID();

        ResponseEntity<Void> resp = controller.deleteUser(uid);
        assertThat(resp.getStatusCodeValue()).isEqualTo(204);

        verify(userService).delete(uid);
    }

    @Test
    void blockUser_setsBlockedFlag_and_returnsUpdatedUser() {
        UUID uid = UUID.randomUUID();
        BlockUserRequestDto req = new BlockUserRequestDto();
        req.setBlocked(true);

        User blocked = new User();
        blocked.setId(uid);
        blocked.setBlocked(true);

        when(userService.block(eq(uid), eq(true))).thenReturn(blocked);

        ResponseEntity<?> resp = controller.blockUser(uid, req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Object body = resp.getBody();
        Boolean blockedFlag = extractBlocked(body);
        assertThat(blockedFlag).isTrue();
        verify(userService).block(uid, true);
    }
}