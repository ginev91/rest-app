package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.RestaurantTable;
import org.example.main.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RestaurantTableService implements IRestaurantTableService {

    private final RestaurantTableRepository tableRepository;

    public RestaurantTableService(RestaurantTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantTable> findAll() {
        return tableRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantTable findById(UUID id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable not found: " + id));
    }

    @Override
    public RestaurantTable create(RestaurantTable table) {
        return tableRepository.save(table);
    }

    @Override
    public RestaurantTable update(UUID id, RestaurantTable changes) {
        RestaurantTable existing = findById(id);
        if (changes.getCode() != null) existing.setCode(changes.getCode());
        if (changes.getSeats() != 0) existing.setSeats(changes.getSeats());
        return tableRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        if (!tableRepository.existsById(id)) {
            throw new ResourceNotFoundException("RestaurantTable not found: " + id);
        }
        tableRepository.deleteById(id);
    }
}