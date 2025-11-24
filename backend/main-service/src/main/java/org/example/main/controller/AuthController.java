package org.example.main.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.example.main.dto.request.LoginRequestDto;
import org.example.main.dto.request.RegisterRequestDto;
import org.example.main.dto.response.AuthResponseDto;
import org.example.main.model.Role;
import org.example.main.model.User;
import org.example.main.repository.RoleRepository;
import org.example.main.repository.UserRepository;
import org.example.main.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwt;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder, JwtUtils jwt) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        try {
            logger.debug("Handling /api/auth/me - authentication: {}", authentication);

            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthenticated"));
            }

            Object principal = authentication.getPrincipal();
            logger.debug("Principal class: {}, principal: {}", principal != null ? principal.getClass().getName() : "null", principal);

            Map<String, Object> dto = new LinkedHashMap<>();

            if (principal instanceof UserDetails) {
                UserDetails ud = (UserDetails) principal;
                dto.put("username", ud.getUsername());
                dto.put("authorities", ud.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));
                Optional<User> userOpt = users.findByUsername(ud.getUsername());
                userOpt.ifPresent(user -> dto.put("userId", user.getId()));
                userOpt.ifPresent(user -> dto.put("role", user.getRole()));
            } else if (principal instanceof Map) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) principal;
                    dto.putAll(map);
                } catch (ClassCastException cce) {
                    logger.warn("Principal is a Map but casting failed", cce);
                    dto.put("principal", principal.toString());
                }
            } else {
                dto.put("principal", principal != null ? principal.toString() : null);
            }

            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            logger.error("Error while handling /api/auth/me", ex);
            return ResponseEntity.status(500).body(Map.of("message", "Internal server error"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDto req) {
        Optional<User> existing = users.findByUsername(req.getUsername());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setFullName(req.getFullName());
        Role userRole = roles.findByName("ROLE_USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_USER");
            return roles.save(r);
        });
        u.setRole(userRole.getName());
        users.save(u);
        String token = jwt.generateToken(u.getUsername());
        return ResponseEntity.ok(new AuthResponseDto(token, u.getUsername(), u.getId(), u.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto req) {
        Optional<User> userOpt = users.findByUsername(req.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
            String token = jwt.generateToken(user.getUsername());
            return ResponseEntity.ok(new AuthResponseDto(token, user.getUsername(), user.getId(), user.getRole()));
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}