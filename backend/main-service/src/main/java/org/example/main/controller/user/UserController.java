package org.example.main.controller.user;

import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.UserProfileResponseDto;
import org.example.main.mapper.user.UserMapper;
import org.example.main.model.user.User;
import org.example.main.service.role.IRoleService;
import org.example.main.service.user.IUserService;
import org.example.main.model.role.Role;
import org.example.main.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final IUserService userService;
    private final IRoleService roleService;

    public UserController(IUserService userService, IRoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<UserProfileResponseDto>> list() {
        List<UserProfileResponseDto> list = userService.findAll().stream()
                .map(UserMapper::toProfile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponseDto> get(@PathVariable UUID id) {
        User u = userService.findById(id);
        return ResponseEntity.ok(UserMapper.toProfile(u));
    }

    @PostMapping
    public ResponseEntity<UserProfileResponseDto> create(@Valid @RequestBody RegisterRequestDto request) {
        User toCreate = UserMapper.toEntity(request);
        User created = userService.create(toCreate, request.getPassword());
        return ResponseEntity.created(URI.create("/api/users/" + created.getId()))
                .body(UserMapper.toProfile(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponseDto> update(@PathVariable UUID id,
                                                       @Valid @RequestBody RegisterRequestDto changes) {
        User u = new User();
        u.setFullName(changes.getFullName());
        u.setUsername(changes.getUsername());
        User updated = userService.update(id, u);
        return ResponseEntity.ok(UserMapper.toProfile(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<UserProfileResponseDto> assignRole(@PathVariable UUID id, @RequestParam String roleName) {
        Role role = roleService.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        User updated = userService.assignRole(id, role.getName());
        return ResponseEntity.ok(UserMapper.toProfile(updated));
    }
}