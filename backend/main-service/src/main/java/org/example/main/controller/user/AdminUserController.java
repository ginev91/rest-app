package org.example.main.controller.user;

import lombok.RequiredArgsConstructor;
import org.example.main.dto.request.user.BlockUserRequestDto;
import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.UserProfileResponseDto;
import org.example.main.mapper.user.UserMapper;
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

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {

    private final IUserService userService;
    private final IRoleService roleService;
    private final RoleMapper roleMapper;

    // RETURN DTOs (not JPA entities)
    @GetMapping
    public ResponseEntity<List<UserProfileResponseDto>> listUsers() {
        List<User> users = userService.findAll();
        List<UserProfileResponseDto> dtos = users.stream()
                .map(UserMapper::toProfile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<UserProfileResponseDto>  createUser(@Valid @RequestBody RegisterRequestDto request) {
        User toCreate = UserMapper.toEntity(request);
        User created = userService.create(toCreate, request.getPassword());
        return ResponseEntity.created(URI.create("/api/admin/users/" + created.getId()))
                .body(UserMapper.toProfile(created));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserProfileResponseDto> changeRole(@PathVariable UUID userId, @Valid @RequestBody RoleChangeRequestDto req) {
        User updated = userService.assignRole(userId, req.getRoleName());
        return ResponseEntity.ok(UserMapper.toProfile(updated));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser (@PathVariable UUID userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/block")
    public ResponseEntity<UserProfileResponseDto> blockUser(@PathVariable UUID userId, @Valid @RequestBody BlockUserRequestDto req) {
        User updated = userService.block(userId, req.getBlocked()); // accepts true or false
        return ResponseEntity.ok(UserMapper.toProfile(updated));
    }
}