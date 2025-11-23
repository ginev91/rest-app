package org.example.main.repository;

import org.example.main.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByCategory_Id(UUID categoryId);

    List<MenuItem> findByCategory_Name(String categoryName);

    List<MenuItem> findByAvailableTrue();

    Optional<MenuItem> findByName(String name);
}