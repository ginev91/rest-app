package org.example.main.service.user;

import jakarta.servlet.http.HttpSession;
import org.example.main.dto.request.user.LoginRequestDto;
import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.AuthResponseDto;
import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.role.Role;
import org.example.main.model.user.User;
import org.example.main.repository.table.RestaurantTableRepository;
import org.example.main.repository.role.RoleRepository;
import org.example.main.repository.user.UserRepository;
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

/**
 * Consolidated tests for UserService.
 *
 * Adjusted to account for User.role being a Role entity.
 */
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
        User u = new User(); u.setId(id);
        Role current = new Role(); current.setName("ROLE_USER");
        u.setRole(current);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        Role r = new Role(); r.setName("ROLE_ADMIN");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(r));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User out = userService.assignRole(id, "ROLE_ADMIN");
        assertThat(out.getRole()).isNotNull();
        assertThat(out.getRole().getName()).isEqualTo("ROLE_ADMIN");
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
        assertThat(out.getRole()).isNotNull();
        assertThat(out.getRole().getName()).isEqualTo("ROLE_NEW");
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

        UserDetails ud = Mockito.mock(UserDetails.class);
        when(ud.getUsername()).thenReturn(username);
        GrantedAuthority ga = new SimpleGrantedAuthority("ROLE_USER");
        doReturn(List.of(ga)).when(ud).getAuthorities();

        when(auth.getPrincipal()).thenReturn(ud);

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        Role roleObj = new Role(); roleObj.setName("ROLE_USER");
        u.setRole(roleObj);
        u.setSessionTableNumber(3);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(u));

        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("sess-1");
        when(session.getAttribute("tableNumber")).thenReturn("2");
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

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

    @Test
    void login_nonUserRole_success_removesSessionAttrs_and_returnsAuth() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("admin");
        dto.setPassword("pw");

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("admin");
        u.setPasswordHash("h");
        Role role = new Role(); role.setName("ROLE_ADMIN");
        u.setRole(role);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "h")).thenReturn(true);
        when(jwtUtils.generateToken(eq("admin"), anyList())).thenReturn("tok");

        HttpSession session = mock(HttpSession.class);

        AuthResponseDto resp = userService.login(dto, session);
        assertThat(resp.getToken()).isEqualTo("tok");
        assertThat(resp.getUsername()).isEqualTo("admin");
        assertThat(resp.getTableNumber()).isNull();
        verify(session).removeAttribute("tableNumber");
        verify(session).removeAttribute("tableId");
    }

    @Test
    void login_userRole_requiresTableNumber_and_tablePin_and_setsSession() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("user1");
        dto.setPassword("pw");
        dto.setTableNumber(5);
        dto.setTablePin("1234");

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("user1");
        u.setPasswordHash("h");
        Role rUser = new Role(); rUser.setName("ROLE_USER");
        u.setRole(rUser);

        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        table.setCode("T5");
        table.setPinCode("1234");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "h")).thenReturn(true);
        when(restaurantTableRepository.findByCode("T5")).thenReturn(Optional.of(table));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateToken(eq("user1"), anyList())).thenReturn("tt");

        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("sess-42");

        AuthResponseDto resp = userService.login(dto, session);
        assertThat(resp.getToken()).isEqualTo("tt");
        assertThat(resp.getTableNumber()).isEqualTo(5);
        assertThat(resp.getTableId()).isEqualTo(table.getId());
        verify(session).setAttribute("tableNumber", 5);
        verify(session).setAttribute("tableId", table.getId());
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void login_userRole_missingTableNumber_throws() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("user2");
        dto.setPassword("pw");
        dto.setTablePin("p");

        User u = new User();
        u.setUsername("user2");
        u.setPasswordHash("h");
        Role rUser = new Role(); rUser.setName("ROLE_USER");
        u.setRole(rUser);

        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "h")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableNumber required");
    }

    @Test
    void login_userRole_invalidTablePin_throws() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("user3");
        dto.setPassword("pw");
        dto.setTableNumber(7);
        dto.setTablePin("wrong");

        User u = new User();
        u.setUsername("user3");
        u.setPasswordHash("h");
        Role rUser = new Role(); rUser.setName("ROLE_USER");
        u.setRole(rUser);

        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        table.setCode("T7");
        table.setPinCode("ok");

        when(userRepository.findByUsername("user3")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "h")).thenReturn(true);
        when(restaurantTableRepository.findByCode("T7")).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> userService.login(dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid table pin");
    }

    @Test
    void login_invalidCredentials_throws() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("no");
        dto.setPassword("x");

        when(userRepository.findByUsername("no")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.login(dto, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void register_creates_user_and_returns_token() {
        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("ruser");
        req.setPassword("pw");
        req.setFullName("Full");

        when(userRepository.findByUsername("ruser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("h");
        Role role = new Role(); role.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User uu = inv.getArgument(0);
            uu.setId(UUID.randomUUID());
            return uu;
        });
        when(jwtUtils.generateToken(eq("ruser"), anyList())).thenReturn("tok-r");

        AuthResponseDto out = userService.register(req);
        assertThat(out.getUsername()).isEqualTo("ruser");
        assertThat(out.getToken()).isEqualTo("tok-r");
        assertThat(out.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void me_principal_string_and_session_handling() {
        String username = "strUser";
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(username);

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        Role rUser = new Role(); rUser.setName("ROLE_USER");
        u.setRole(rUser);
        u.setSessionTableNumber(4);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(u));

        HttpSession session = mock(HttpSession.class);
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        Map<String, Object> dto = userService.me(auth, session);
        assertThat(dto).containsEntry("username", username);
        assertThat(dto).containsEntry("role", "ROLE_USER");
        assertThat(dto).containsKey("userId");
        assertThat(dto).containsKey("tableNumber");
    }

    @Test
    void me_principal_map_merges_values() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);

        Map<String, Object> principalMap = new LinkedHashMap<>();
        principalMap.put("username", "muser");
        principalMap.put("extra", "x");

        when(auth.getPrincipal()).thenReturn(principalMap);

        // DO NOT stub userRepository.findByUsername here — the service does not call it for Map principals,
        // so stubbing it would be unnecessary and lead to UnnecessaryStubbingException.

        Map<String, Object> dto = userService.me(auth, null);
        assertThat(dto).containsEntry("username", "muser");
        assertThat(dto).containsEntry("extra", "x");
    }

    @Test
    void me_principal_userdetails_populates_authorities() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);

        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("uduser");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(ud).getAuthorities();

        when(auth.getPrincipal()).thenReturn(ud);

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("uduser");
        Role rUser = new Role(); rUser.setName("ROLE_USER");
        u.setRole(rUser);

        when(userRepository.findByUsername("uduser")).thenReturn(Optional.of(u));

        Map<String, Object> dto = userService.me(auth, null);
        assertThat(dto).containsEntry("username", "uduser");
        assertThat(dto).containsKey("authorities");
        assertThat(dto).containsEntry("role", "ROLE_USER");
    }

    @Test
    void create_assignsEncodedPassword_and_createsDefaultRole_whenMissing() {
        User u = new User();
        u.setUsername("newu");
        u.setRole(null); // missing role

        when(passwordEncoder.encode("raw")).thenReturn("encoded");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());
        Role savedRole = new Role(); savedRole.setName("ROLE_USER");
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User uu = inv.getArgument(0);
            uu.setId(UUID.randomUUID());
            return uu;
        });

        User out = userService.create(u, "raw");

        assertThat(out.getPasswordHash()).isEqualTo("encoded");
        assertThat(out.getRole()).isNotNull();
        assertThat(out.getRole().getName()).isEqualTo("ROLE_USER");
        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findByUsername_found_returnsOptional() {
        User u = new User();
        u.setUsername("exists");
        when(userRepository.findByUsername("exists")).thenReturn(Optional.of(u));

        Optional<User> out = userService.findByUsername("exists");
        assertThat(out).isPresent().contains(u);
    }

    @Test
    void findById_found_returnsUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));

        User out = userService.findById(u.getId());
        assertThat(out).isSameAs(u);
    }

    @Test
    void login_nullDto_throwsIllegalArgument() {
        assertThatThrownBy(() -> userService.login(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username and password are required");
    }

    @Test
    void login_passwordMismatch_throwsInvalidCredentials() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("bob");
        dto.setPassword("pw");

        User u = new User();
        u.setUsername("bob");
        u.setPasswordHash("hash");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(dto, null))
                .isInstanceOf(RuntimeException.class) // ResourceNotFoundException extends RuntimeException
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_userRole_withNullSession_persistsSessionTableNumber_and_returnsAuth() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setUsername("u1");
        dto.setPassword("pw");
        dto.setTableNumber(9);
        dto.setTablePin("pin");

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("u1");
        u.setPasswordHash("h");
        Role rUser = new Role(); rUser.setName("ROLE_USER");
        u.setRole(rUser);

        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        table.setCode("T9");
        table.setPinCode("pin");

        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "h")).thenReturn(true);
        when(restaurantTableRepository.findByCode("T9")).thenReturn(Optional.of(table));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateToken(eq("u1"), anyList())).thenReturn("jwt-xyz");

        // session is null — service should still persist sessionTableNumber on the user
        AuthResponseDto resp = userService.login(dto, null);

        assertThat(resp.getToken()).isEqualTo("jwt-xyz");
        assertThat(resp.getTableNumber()).isEqualTo(9);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_usernameTaken_throwsIllegalArgument() {
        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("taken");
        req.setPassword("p");
        req.setFullName("F");

        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void me_principal_otherObject_putsPrincipalToString() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);

        Object principal = new Object() {
            @Override
            public String toString() { return "custom-principal"; }
        };
        when(auth.getPrincipal()).thenReturn(principal);

        Map<String, Object> dto = userService.me(auth, null);

        assertThat(dto).containsEntry("principal", "custom-principal");
    }

    @Test
    void me_sessionTableNumber_asNumber_is_used_directly() {
        String username = "numUser";
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);

        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn(username);
        when(auth.getPrincipal()).thenReturn(ud);
        doReturn(List.of()).when(ud).getAuthorities(); // authorities empty

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        Role rUser = new Role(); rUser.setName("ROLE_USER");
        u.setRole(rUser);
        u.setSessionTableNumber(null);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(u));

        HttpSession session = mock(HttpSession.class);
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
        when(session.getAttribute("tableNumber")).thenReturn(42);

        Map<String, Object> dto = userService.me(auth, session);

        assertThat(dto).containsEntry("tableNumber", 42);
    }

    @Test
    void verifyPassword_returnsFalse_whenUserOrPasswordNull() {
        // null user
        assertThat(userService.verifyPassword(null, "raw")).isFalse();

        // null password
        User u = new User();
        u.setPasswordHash("encoded");
        assertThat(userService.verifyPassword(u, null)).isFalse();
    }

    @Test
    void verifyPassword_delegatesToPasswordEncoder_matches() {
        User u = new User();
        u.setPasswordHash("encoded-hash");

        when(passwordEncoder.matches("raw-pass", "encoded-hash")).thenReturn(true);
        boolean ok = userService.verifyPassword(u, "raw-pass");
        assertThat(ok).isTrue();
        verify(passwordEncoder).matches("raw-pass", "encoded-hash");
    }

    @Test
    void setPassword_encodes_and_savesUser() {
        UUID id = UUID.randomUUID();
        User u = new User();
        u.setId(id);
        u.setPasswordHash("old-encoded");

        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("newpass")).thenReturn("new-encoded");

        userService.setPassword(id, "newpass");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getPasswordHash()).isEqualTo("new-encoded");
    }

    @Test
    void setPassword_missingUser_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setPassword(id, "x"))
                .isInstanceOf(RuntimeException.class); // ResourceNotFoundException extends RuntimeException
        verify(userRepository, never()).save(any());
    }
}