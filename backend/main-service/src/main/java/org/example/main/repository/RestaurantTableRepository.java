package org.example.main.repository;

import org.example.main.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {
    Optional<RestaurantTable> findByCode(String code);
    Optional<RestaurantTable> findByTableNumber(Integer tableNumber);
}