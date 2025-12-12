package org.example.main.service.user;

import org.example.main.model.user.User;
import org.example.main.repository.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities;

        if (u.getRole() != null && u.getRole().getName() != null && !u.getRole().getName().isBlank()) {

            // FIX: Spring Security requires roles like "ROLE_ADMIN"
            String roleName = u.getRole().getName().toUpperCase();

            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }

            authorities = List.of(new SimpleGrantedAuthority(roleName));

        } else {
            authorities = Collections.emptyList();
        }

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPasswordHash(),
                authorities
        );
    }
}
