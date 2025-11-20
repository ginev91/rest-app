package org.example.main.repository;

import org.example.main.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    /**
     * Convenience finder by name. Useful for tests or seeding.
     */
    Optional<MenuItem> findByName(String name);
}