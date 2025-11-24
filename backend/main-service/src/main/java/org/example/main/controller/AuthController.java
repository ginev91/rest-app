package org.example.main.controller;

import jakarta.servlet.http.HttpSession;
import org.example.main.dto.request.LoginRequestDto;
import org.example.main.dto.request.RegisterRequestDto;
import org.example.main.dto.response.AuthResponseDto;
import org.example.main.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final IUserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication, HttpSession session) {
        try {
            Map<String, Object> dto = userService.me(authentication, session);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthenticated"));
        } catch (Exception ex) {
            logger.error("Error while handling /api/auth/me", ex);
            return ResponseEntity.status(500).body(Map.of("message", "Internal server error"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto, HttpSession session) {
        AuthResponseDto resp = (AuthResponseDto) userService.login(dto, session); // keep existing contract
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDto req) {
        AuthResponseDto resp = (AuthResponseDto) userService.register(req);
        return ResponseEntity.ok(resp);
    }
}