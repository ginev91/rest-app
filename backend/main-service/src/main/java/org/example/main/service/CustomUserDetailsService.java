// src/main/java/org/example/main/service/CustomUserDetailsService.java
package org.example.main.service;

import org.example.main.model.User;
import org.example.main.repository.UserRepository;
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
        if (u.getRole() != null && !u.getRole().isBlank()) {
            authorities = List.of(new SimpleGrantedAuthority(u.getRole()));
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