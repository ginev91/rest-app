package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.Role;
import org.example.main.model.User;
import org.example.main.repository.RoleRepository;
import org.example.main.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UserService manages user creation and role assignment.
 * Requires a PasswordEncoder bean in the context.
 */
@Service
@Transactional
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public User create(User user, String rawPassword) {
        // encode password
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        // ensure roles collection exists
        if (user.getRoles() == null) user.setRoles(new HashSet<>());

        // optionally assign default role if none
        if (user.getRoles().isEmpty()) {
            Role defaultRole = roleRepository.findByName("USER")
                    .orElseGet(() -> roleRepository.save(new Role(null, "USER")));
            user.getRoles().add(defaultRole);
        }

        return userRepository.save(user);
    }

    @Override
    public User update(UUID id, User changes) {
        User existing = findById(id);
        if (changes.getFullName() != null) existing.setFullName(changes.getFullName());
        if (changes.getUsername() != null) existing.setUsername(changes.getUsername());
        // DO NOT copy passwordHash directly; use changePassword API instead (not implemented here)
        return userRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) throw new ResourceNotFoundException("User not found: " + id);
        userRepository.deleteById(id);
    }

    @Override
    public User assignRole(UUID userId, String roleName) {
        User user = findById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
        if (user.getRoles() == null) user.setRoles(new HashSet<>());
        user.getRoles().add(role);
        return userRepository.save(user);
    }
}