package org.example.main.controller.category;

import org.example.main.dto.request.category.CategoryRequestDto;
import org.example.main.dto.response.category.CategoryResponseDto;
import org.example.main.mapper.category.CategoryMapper;
import org.example.main.model.category.CategoryEntity;
import org.example.main.service.category.ICategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@Validated
public class CategoryController {

    private final ICategoryService service;

    public CategoryController(ICategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> list() {
        List<CategoryResponseDto> list = service.findAll().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> get(@PathVariable UUID id) {
        CategoryEntity e = service.findById(id);
        // call mapper directly (not using a method-reference 'apply' which is invalid)
        return ResponseEntity.ok(CategoryMapper.toResponse(e));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> create(@Valid @RequestBody CategoryRequestDto dto) {
        CategoryEntity e = CategoryMapper.toEntity(dto);
        CategoryEntity created = service.create(e);
        return ResponseEntity.created(URI.create("/api/categories/" + created.getId()))
                .body(CategoryMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequestDto dto) {
        CategoryEntity changes = CategoryMapper.toEntity(dto);
        CategoryEntity updated = service.update(id, changes);
        return ResponseEntity.ok(CategoryMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}