package org.example.main.controller;

import jakarta.servlet.http.HttpSession;
import org.example.main.dto.request.LoginRequestDto;
import org.example.main.dto.request.RegisterRequestDto;
import org.example.main.dto.response.AuthResponseDto;
import org.example.main.security.JwtUtils;
import org.example.main.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final IUserService userService;
    private final JwtUtils jwtUtils;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(IUserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
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

    /**
     * Login: returns AuthResponseDto in the body and sets an HttpOnly cookie with the JWT.
     * Cookie name: access_token
     *
     * Notes:
     * - For ROLE_USER clients the service requires tableNumber and tablePin; the service will
     *   validate them and throw IllegalArgumentException when missing/invalid.
     * - For ROLE_EMPLOYEE and ROLE_ADMIN those fields are ignored by the service.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto, HttpSession session) {
        AuthResponseDto resp = (AuthResponseDto) userService.login(dto, session);

        long maxAgeSeconds = Math.max(1, jwtUtils.getJwtExpirationMs() / 1000L);

        ResponseCookie cookie = ResponseCookie.from("access_token", resp.getToken())
                .httpOnly(true)
                .secure(false) // set to true in production (requires HTTPS)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(resp);
    }

    /**
     * Clear the access_token cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false) // set to true in production
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequestDto req) {
        AuthResponseDto resp = (AuthResponseDto) userService.register(req);
        return ResponseEntity.ok(resp);
    }
}