package org.example.main.service.menu;

import org.example.main.model.menu.MenuItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMenuItemService {
    List<MenuItem> findAll();
    Optional<MenuItem> findByName(String name);
    MenuItem findById(UUID id);
    MenuItem create(MenuItem menuItem);
    MenuItem update(UUID id, MenuItem changes);
    void delete(UUID id);
}