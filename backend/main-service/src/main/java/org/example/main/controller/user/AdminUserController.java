package org.example.main.controller.user;

import lombok.RequiredArgsConstructor;
import org.example.main.model.user.User;
import org.example.main.service.user.IUserService;
import org.example.main.service.role.IRoleService;
import org.example.main.mapper.role.RoleMapper;
import org.example.main.dto.request.role.RoleChangeRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {

    private final IUserService userService;
    private final IRoleService roleService;
    private final RoleMapper roleMapper;

    @GetMapping
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<User> changeRole(@PathVariable UUID userId, @Valid @RequestBody RoleChangeRequestDto req) {
        User updated = userService.assignRole(userId, req.getRoleName());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/block")
    public ResponseEntity<User> blockUser(@PathVariable UUID userId) {
        // assign special blocking role. Admin can later reassign a proper role.
        User updated = userService.assignRole(userId, "ROLE_BLOCKED");
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/unblock")
    public ResponseEntity<User> unblockUser(@PathVariable UUID userId, @Valid @RequestBody RoleChangeRequestDto req) {
        User updated = userService.assignRole(userId, req.getRoleName());
        return ResponseEntity.ok(updated);
    }
}