package org.example.main.controller.user;

import lombok.RequiredArgsConstructor;
import org.example.main.model.user.User;
import org.example.main.service.user.IUserService;
import org.example.main.dto.request.user.UpdateProfileRequestDto;
import org.example.main.dto.request.user.ChangePasswordRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

/**
 * Endpoints for authenticated users to view and edit their own profile.
 * - GET /api/users/me
 * - PUT /api/users/me (update fullname or username)
 * - PUT /api/users/me/password (change password)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserProfileController {

    private final IUserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return ResponseEntity.ok(userService.me(authentication, null));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateMe(Authentication authentication, @Valid @RequestBody UpdateProfileRequestDto req) {
        // fetch current user id via userService.me / repository
        var dto = userService.me(authentication, null);
        Object uid = dto.get("userId");
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id missing");
        }
        java.util.UUID id = uid instanceof java.util.UUID ? (java.util.UUID) uid : java.util.UUID.fromString(uid.toString());

        User changes = new User();
        changes.setFullName(req.getFullName());
        changes.setUsername(req.getUsername());
        User updated = userService.update(id, changes);

        // NOTE: prefer returning a response DTO rather than entity to avoid leaking fields.
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequestDto req) {
        var dto = userService.me(authentication, null);
        Object uid = dto.get("userId");
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id missing");
        }
        java.util.UUID id = uid instanceof java.util.UUID ? (java.util.UUID) uid : java.util.UUID.fromString(uid.toString());

        User u = userService.findById(id);
        if (!userService.verifyPassword(u, req.getOldPassword())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid current password");
        }
        userService.setPassword(id, req.getNewPassword());
        return ResponseEntity.ok().build();
    }
}