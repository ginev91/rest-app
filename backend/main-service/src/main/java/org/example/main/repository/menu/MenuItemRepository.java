package org.example.main.repository.menu;

import org.example.main.model.menu.MenuItem;
import org.example.main.model.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    
    Optional<MenuItem> findByName(String name);
    
    List<MenuItem> findByCategory_ItemType(ItemType itemType);

    List<MenuItem> findByCategory_ItemTypeIn(Collection<ItemType> types);
    
    List<MenuItem> findByAvailableTrue();
}