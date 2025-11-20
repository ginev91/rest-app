package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.Role;
import org.example.main.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RoleService implements IRoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        // assumes RoleRepository defines Optional<Role> findByName(String)
        return roleRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Role findById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    @Override
    public Role create(Role role) {
        return roleRepository.save(role);
    }
}