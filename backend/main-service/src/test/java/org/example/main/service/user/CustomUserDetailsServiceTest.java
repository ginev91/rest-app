package org.example.main.service.user;

import org.example.main.model.user.User;
import org.example.main.model.role.Role;
import org.example.main.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService svc;

    @Test
    void loadUserByUsername_returnsUserDetails_withAuthorityFromRole() {
        String username = "alice";
        String passwordHash = "secretHash";
        String roleName = "ROLE_EMPLOYEE";

        // create simple User and Role mocks (these are likely POJOs in main code)
        User user = mock(User.class);
        Role role = mock(Role.class);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(user.getUsername()).thenReturn(username);
        when(user.getPasswordHash()).thenReturn(passwordHash);
        when(user.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(roleName);

        UserDetails details = svc.loadUserByUsername(username);

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo(username);
        assertThat(details.getPassword()).isEqualTo(passwordHash);
        // assert authority by extracting the 'authority' property to avoid generic type mismatch
        assertThat(details.getAuthorities()).extracting("authority").containsExactly(roleName);

        verify(userRepository).findByUsername(username);
    }

    @Test
    void loadUserByUsername_returnsUserDetails_withNoAuthorities_whenRoleMissingOrBlank() {
        String username = "bob";
        String passwordHash = "passHash";

        User user = mock(User.class);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(user.getUsername()).thenReturn(username);
        when(user.getPasswordHash()).thenReturn(passwordHash);

        // Case A: role == null
        when(user.getRole()).thenReturn(null);

        UserDetails detailsA = svc.loadUserByUsername(username);
        assertThat(detailsA.getUsername()).isEqualTo(username);
        assertThat(detailsA.getPassword()).isEqualTo(passwordHash);
        assertThat(detailsA.getAuthorities()).isEmpty();

        // Case B: role present but name blank -> still no authorities
        Role blankRole = mock(Role.class);
        when(user.getRole()).thenReturn(blankRole);
        when(blankRole.getName()).thenReturn("   "); // blank

        UserDetails detailsB = svc.loadUserByUsername(username);
        assertThat(detailsB.getAuthorities()).isEmpty();

        verify(userRepository, times(2)).findByUsername(username);
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenUserMissing() {
        String username = "missing";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(username);

        verify(userRepository).findByUsername(username);
    }
}