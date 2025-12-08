
package org.example.main.service;

import jakarta.servlet.http.HttpSession;
import org.example.main.dto.request.LoginRequestDto;
import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.RestaurantTable;
import org.example.main.model.Role;
import org.example.main.model.User;
import org.example.main.repository.RestaurantTableRepository;
import org.example.main.repository.RoleRepository;
import org.example.main.repository.UserRepository;
import org.example.main.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtUtils jwtUtils;

    @Mock
    RestaurantTableRepository restaurantTableRepository;

    @InjectMocks
    UserService userService;

    @Test
    void findAll_returnsList() {
        User u1 = new User(); u1.setUsername("a");
        User u2 = new User(); u2.setUsername("b");
        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        List<User> res = userService.findAll();
        assertThat(res).hasSize(2).extracting(User::getUsername).containsExactly("a", "b");
    }

    @Test
    void findByUsername_notFound_throws() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findByUsername("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void update_appliesChanges_and_saves() {
        UUID id = UUID.randomUUID();
        User existing = new User();
        existing.setId(id);
        existing.setUsername("old");
        existing.setFullName("Old Name");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User changes = new User();
        changes.setUsername("new");
        changes.setFullName("New Name");

        User out = userService.update(id, changes);
        assertThat(out.getUsername()).isEqualTo("new");
        assertThat(out.getFullName()).isEqualTo("New Name");
        verify(userRepository).save(existing);
    }

    @Test
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);
        assertThatThrownBy(() -> userService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void delete_exists_deletes() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);
        userService.delete(id);
        verify(userRepository).deleteById(id);
    }

    @Test
    void assignRole_existingRole_assigns() {
        UUID id = UUID.randomUUID();
        User u = new User(); u.setId(id); u.setRole("ROLE_USER");
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        Role r = new Role(); r.setName("ROLE_ADMIN");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(r));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User out = userService.assignRole(id, "ROLE_ADMIN");
        assertThat(out.getRole()).isEqualTo("ROLE_ADMIN");
        verify(roleRepository).findByName("ROLE_ADMIN");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void assignRole_createsRole_whenMissing() {
        UUID id = UUID.randomUUID();
        User u = new User(); u.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("ROLE_NEW")).thenReturn(Optional.empty());

        Role created = new Role(); created.setName("ROLE_NEW");
        when(roleRepository.save(any(Role.class))).thenReturn(created);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User out = userService.assignRole(id, "ROLE_NEW");
        assertThat(out.getRole()).isEqualTo("ROLE_NEW");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void me_unauthenticated_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        assertThatThrownBy(() -> userService.me(auth, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthenticated");
    }

    @Test
    void me_userdetails_and_session_and_tableResolution() {
        String username = "user@test.com";

        
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);

        org.springframework.security.core.userdetails.UserDetails ud =
                Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);

        when(ud.getUsername()).thenReturn(username);

        GrantedAuthority ga = new SimpleGrantedAuthority("ROLE_USER");
        when(ud.getAuthorities()).thenReturn((Collection) List.of(ga));

        when(auth.getPrincipal()).thenReturn(ud);

        
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setRole("ROLE_USER");
        u.setSessionTableNumber(3);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(u));

        
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("sess-1");
        when(session.getAttribute("tableNumber")).thenReturn("2");
        when(session.getAttributeNames())
                .thenReturn(Collections.enumeration(Collections.emptyList()));

        
        RestaurantTable t = new RestaurantTable();
        t.setId(UUID.randomUUID());
        t.setCode("T2");

        when(restaurantTableRepository.findByCode("T2")).thenReturn(Optional.of(t));

        
        Map<String, Object> dto = userService.me(auth, session);

        
        assertThat(dto).containsEntry("username", username);
        assertThat(dto).containsEntry("role", "ROLE_USER");

        Object returnedUserId = dto.get("userId");
        if (returnedUserId instanceof UUID) {
            assertThat(returnedUserId).isEqualTo(u.getId());
        } else {
            assertThat(returnedUserId).isEqualTo(u.getId().toString());
        }

        assertThat(dto).containsEntry("tableNumber", 2);
        assertThat(dto).containsKey("tableId");
    }

}