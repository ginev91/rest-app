package org.example.main.controller;

import org.example.main.dto.request.RegisterRequestDto;
import org.example.main.dto.response.UserProfileResponseDto;
import org.example.main.model.Role;
import org.example.main.model.User;
import org.example.main.service.IRoleService;
import org.example.main.service.IUserService;
import org.example.main.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController covering all endpoints:
 * - list()
 * - get(id)
 * - create(request)
 * - update(id, changes)
 * - delete(id)
 * - assignRole(id, roleName) (success and role-not-found)
 *
 * NOTE: The UserProfileResponseDto in your project does not expose a getRole() accessor,
 * so assertions that previously called body.getRole() were failing. This version avoids
 * calling getRole() and instead asserts on other visible fields (id/username/fullName).
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private IUserService userService;

    @Mock
    private IRoleService roleService;

    @InjectMocks
    private UserController userController;

    private UUID id1;
    private User user1;

    @BeforeEach
    void setUp() {
        id1 = UUID.randomUUID();
        user1 = new User();
        user1.setId(id1);
        user1.setUsername("u1@example.com");
        user1.setFullName("User One");
        user1.setRole("ROLE_USER");
    }

    @Test
    void list_returnsMappedProfiles() {
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setUsername("u2@example.com");
        user2.setFullName("User Two");
        user2.setRole("ROLE_EMPLOYEE");

        when(userService.findAll()).thenReturn(List.of(user1, user2));

        ResponseEntity<List<UserProfileResponseDto>> resp = userController.list();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        List<UserProfileResponseDto> body = resp.getBody();
        assertThat(body).hasSize(2);
        assertThat(body).extracting(UserProfileResponseDto::getUsername)
                .containsExactlyInAnyOrder("u1@example.com", "u2@example.com");
    }

    @Test
    void get_returnsProfile() {
        when(userService.findById(id1)).thenReturn(user1);

        ResponseEntity<UserProfileResponseDto> resp = userController.get(id1);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        UserProfileResponseDto profile = resp.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.getUsername()).isEqualTo("u1@example.com");
        assertThat(profile.getFullName()).isEqualTo("User One");
        assertThat(profile.getId()).isEqualTo(id1);
    }

    @Test
    void create_callsService_and_returnsCreated() {
        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("new@example.com");
        req.setPassword("pw");
        req.setFullName("New User");

        // The controller transforms request -> entity via UserMapper.toEntity(request).
        // We mock the service create(...) to return a User with an id.
        User created = new User();
        UUID createdId = UUID.randomUUID();
        created.setId(createdId);
        created.setUsername(req.getUsername());
        created.setFullName(req.getFullName());
        created.setRole("ROLE_USER");

        when(userService.create(any(User.class), eq(req.getPassword()))).thenReturn(created);

        ResponseEntity<UserProfileResponseDto> resp = userController.create(req);

        assertThat(resp.getStatusCodeValue()).isEqualTo(201);
        assertThat(resp.getHeaders().getLocation()).isEqualTo(URI.create("/api/users/" + createdId));
        UserProfileResponseDto body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getUsername()).isEqualTo("new@example.com");
        assertThat(body.getId()).isEqualTo(createdId);
        verify(userService).create(any(User.class), eq("pw"));
    }

    @Test
    void update_callsService_and_returnsOk() {
        RegisterRequestDto changes = new RegisterRequestDto();
        changes.setUsername("updated@example.com");
        changes.setFullName("Updated Name");
        UUID id = id1;

        User updated = new User();
        updated.setId(id);
        updated.setUsername(changes.getUsername());
        updated.setFullName(changes.getFullName());
        updated.setRole("ROLE_USER");

        when(userService.update(eq(id), any(User.class))).thenReturn(updated);

        ResponseEntity<UserProfileResponseDto> resp = userController.update(id, changes);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        UserProfileResponseDto body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getUsername()).isEqualTo("updated@example.com");
        assertThat(body.getFullName()).isEqualTo("Updated Name");
        verify(userService).update(eq(id), any(User.class));
    }

    @Test
    void delete_callsService_and_returnsNoContent() {
        UUID id = id1;
        // ensure no exception is thrown by mocked service
        doNothing().when(userService).delete(id);

        ResponseEntity<Void> resp = userController.delete(id);

        assertThat(resp.getStatusCodeValue()).isEqualTo(204);
        verify(userService).delete(id);
    }

    @Test
    void assignRole_success_returnsUpdatedProfile() {
        UUID id = id1;
        String roleName = "ROLE_EMPLOYEE";
        Role role = new Role();
        role.setName(roleName);

        User updated = new User();
        updated.setId(id);
        updated.setUsername("emp@example.com");
        updated.setFullName("Emp");
        updated.setRole(roleName);

        when(roleService.findByName(roleName)).thenReturn(Optional.of(role));
        when(userService.assignRole(id, roleName)).thenReturn(updated);

        ResponseEntity<UserProfileResponseDto> resp = userController.assignRole(id, roleName);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        UserProfileResponseDto body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(id);
        assertThat(body.getUsername()).isEqualTo("emp@example.com");
        // Don't call getRole() — UserProfileResponseDto does not expose role in this codebase
        verify(roleService).findByName(roleName);
        verify(userService).assignRole(id, roleName);
    }

    @Test
    void assignRole_roleNotFound_throws() {
        UUID id = id1;
        String roleName = "ROLE_UNKNOWN";

        when(roleService.findByName(roleName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.assignRole(id, roleName))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found: " + roleName);

        verify(roleService).findByName(roleName);
        verify(userService, never()).assignRole(any(), anyString());
    }
}