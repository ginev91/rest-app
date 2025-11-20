package org.example.main.service;

import org.example.main.model.Role;
import org.example.main.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserService {
    List<User> findAll();
    Optional<User> findByUsername(String username);
    User findById(UUID id);
    User create(User user, String rawPassword);
    User update(UUID id, User changes);
    void delete(UUID id);
    User assignRole(UUID userId, String roleName);
}