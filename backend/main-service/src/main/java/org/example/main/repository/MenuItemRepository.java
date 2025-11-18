package org.example.main.repository;

import org.example.main.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {}