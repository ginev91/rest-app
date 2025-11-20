package org.example.main.service;

import org.example.main.model.RestaurantTable;

import java.util.List;
import java.util.UUID;

public interface IRestaurantTableService {
    List<RestaurantTable> findAll();
    RestaurantTable findById(UUID id);
    RestaurantTable create(RestaurantTable table);
    RestaurantTable update(UUID id, RestaurantTable changes);
    void delete(UUID id);
}