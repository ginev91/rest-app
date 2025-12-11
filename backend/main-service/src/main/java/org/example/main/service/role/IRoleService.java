package org.example.main.service.role;

import org.example.main.model.role.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRoleService {
    List<Role> findAll();
    Optional<Role> findByName(String name);
    Role findById(UUID id);
}