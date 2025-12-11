package org.example.main.controller.user;

import org.example.main.model.role.Role;
import org.example.main.model.user.User;
import org.example.main.service.role.IRoleService;
import org.example.main.service.user.IUserService;
import org.example.main.mapper.role.RoleMapper;
import org.example.main.dto.request.role.RoleChangeRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

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

    @Test
    void listUsers_returnsUsersFromService() {
        User u1 = new User(); u1.setId(UUID.randomUUID()); u1.setUsername("a");
        User u2 = new User(); u2.setId(UUID.randomUUID()); u2.setUsername("b");

        when(userService.findAll()).thenReturn(List.of(u1, u2));

        ResponseEntity<List<User>> resp = controller.listUsers();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        List<User> body = resp.getBody();
        assertThat(body).hasSize(2).extracting(User::getUsername).containsExactlyInAnyOrder("a", "b");
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

        ResponseEntity<User> resp = controller.changeRole(uid, req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        User body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getRole()).isNotNull();
        assertThat(body.getRole().getName()).isEqualTo("ROLE_EMPLOYEE");
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
    void unblockUser_restoresGivenRole() {
        UUID uid = UUID.randomUUID();
        RoleChangeRequestDto req = new RoleChangeRequestDto();
        req.setRoleName("ROLE_USER");

        User restored = new User();
        restored.setId(uid);
        Role r = new Role(); r.setName("ROLE_USER");
        restored.setRole(r);

        when(userService.assignRole(eq(uid), eq("ROLE_USER"))).thenReturn(restored);

        ResponseEntity<User> resp = controller.unblockUser(uid, req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        User body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getRole().getName()).isEqualTo("ROLE_USER");
        verify(userService).assignRole(uid, "ROLE_USER");
    }
}