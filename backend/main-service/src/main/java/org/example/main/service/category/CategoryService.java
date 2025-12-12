package org.example.main.service.category;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.category.CategoryEntity;
import org.example.main.model.enums.ItemType;
import org.example.main.repository.category.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CategoryService implements ICategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryEntity> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Override
    public CategoryEntity create(CategoryEntity entity) {
        if (entity == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category payload required");

        // Ensure itemType is provided
        if (entity.getItemType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemType is required");
        }

        // Only allow supported ItemType values (enum restricts this already)
        Optional<CategoryEntity> existing = repository.findByItemType(entity.getItemType());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category for itemType already exists: " + entity.getItemType());
        }

        return repository.save(entity);
    }

    @Override
    public CategoryEntity update(UUID id, CategoryEntity changes) {
        CategoryEntity existing = findById(id);
        if (changes.getItemType() != null && changes.getItemType() != existing.getItemType()) {
            Optional<CategoryEntity> byType = repository.findByItemType(changes.getItemType());
            if (byType.isPresent() && !byType.get().getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Category for itemType already exists: " + changes.getItemType());
            }
            existing.setItemType(changes.getItemType());
        }
        return repository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        CategoryEntity c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        repository.deleteById(id);
    }
}