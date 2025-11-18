package org.example.main.controller;

import org.example.main.dto.request.LoginRequestDto;
import org.example.main.dto.request.RegisterRequestDto;
import org.example.main.dto.response.AuthResponseDto;
import org.example.main.model.Role;
import org.example.main.model.User;
import org.example.main.repository.RoleRepository;
import org.example.main.repository.UserRepository;
import org.example.main.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final RoleRepository roles;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwt;

    public AuthController(UserRepository users, RoleRepository roles, BCryptPasswordEncoder passwordEncoder, JwtUtils jwt) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDto req) {
        if (users.findByUsername(req.getUsername()).isPresent()) {
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
        u.setRoles(Collections.singleton(userRole));
        users.save(u);
        String token = jwt.generateToken(u.getUsername());
        return ResponseEntity.ok(new AuthResponseDto(token, u.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto req) {
        var userOpt = users.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("Invalid credentials");
        var user = userOpt.get();
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        String token = jwt.generateToken(user.getUsername());
        return ResponseEntity.ok(new AuthResponseDto(token, user.getUsername()));
    }
}