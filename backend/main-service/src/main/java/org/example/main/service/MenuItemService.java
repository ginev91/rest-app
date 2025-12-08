package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.MenuItem;
import org.example.main.repository.MenuItemRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MenuItemService implements IMenuItemService {

    private final MenuItemRepository menuItemRepository;

    public MenuItemService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "menuItems")
    public List<MenuItem> findAll() {
        return menuItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "menuItemByName", key = "#name")
    public Optional<MenuItem> findByName(String name) {
        return menuItemRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "menuItemById", key = "#id")
    public MenuItem findById(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuItems", allEntries = true),
            @CacheEvict(value = "menuItemById", allEntries = true),
            @CacheEvict(value = "menuItemByName", allEntries = true)
    })
    public MenuItem create(MenuItem menuItem) {
        return menuItemRepository.save(menuItem);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuItems", allEntries = true),
            @CacheEvict(value = "menuItemById", key = "#id", beforeInvocation = false),
            @CacheEvict(value = "menuItemByName", allEntries = true)
    })
    public MenuItem update(UUID id, MenuItem changes) {
        MenuItem existing = findById(id);
        if (changes.getName() != null) existing.setName(changes.getName());
        existing.setDescription(changes.getDescription());
        if (changes.getPrice() != null) existing.setPrice(changes.getPrice());
        return menuItemRepository.save(existing);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuItems", allEntries = true),
            @CacheEvict(value = "menuItemById", key = "#id"),
            @CacheEvict(value = "menuItemByName", allEntries = true)
    })
    public void delete(UUID id) {
        if (!menuItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("MenuItem not found: " + id);
        }
        menuItemRepository.deleteById(id);
    }
}