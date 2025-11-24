package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.Role;
import org.example.main.model.User;
import org.example.main.repository.RoleRepository;
import org.example.main.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found: " + username);
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public User create(User user, String rawPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        if (user.getRole() == null || user.getRole().isBlank()) {
            Role defaultRole = roleRepository.findByName("USER")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("USER");
                        return roleRepository.save(r);
                    });
            // store role as string on User
            user.setRole(defaultRole.getName());
        }

        return userRepository.save(user);
    }

    @Override
    public User update(UUID id, User changes) {
        User existing = findById(id);
        if (changes.getFullName() != null) existing.setFullName(changes.getFullName());
        if (changes.getUsername() != null) existing.setUsername(changes.getUsername());
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
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(roleName);
                    return roleRepository.save(r);
                });
        // set role name (String) on User to match current User model
        user.setRole(role.getName());
        return userRepository.save(user);
    }
}