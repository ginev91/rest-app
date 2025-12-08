package org.example.main.service;

import org.example.main.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRoleService {
    List<Role> findAll();
    Optional<Role> findByName(String name);
    Role findById(UUID id);
}