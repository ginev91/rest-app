package org.example.main.controller;

import org.example.main.dto.request.MenuItemRequestDto;
import org.example.main.dto.response.MenuItemResponseDto;
import org.example.main.mapper.MenuItemMapper;
import org.example.main.model.MenuItem;
import org.example.main.service.IMenuItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/menu")
@Validated
public class MenuItemController {

    private final IMenuItemService menuItemService;

    public MenuItemController(IMenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping
    public ResponseEntity<List<MenuItemResponseDto>> list() {
        List<MenuItemResponseDto> list = menuItemService.findAll().stream()
                .map(MenuItemMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponseDto> get(@PathVariable UUID id) {
        MenuItem item = menuItemService.findById(id);
        return ResponseEntity.ok(MenuItemMapper.toResponse(item));
    }

    @PostMapping
    public ResponseEntity<MenuItemResponseDto> create(@Valid @RequestBody MenuItemRequestDto request) {
        MenuItem entity = MenuItemMapper.toEntity(request);
        MenuItem created = menuItemService.create(entity);
        return ResponseEntity.created(URI.create("/api/menu/" + created.getId()))
                .body(MenuItemMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemResponseDto> update(@PathVariable UUID id,
                                                   @Valid @RequestBody MenuItemRequestDto changes) {
        
        MenuItem existing = menuItemService.findById(id);
        MenuItemMapper.copyToEntity(changes, existing);
        MenuItem updated = menuItemService.update(id, existing);
        return ResponseEntity.ok(MenuItemMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        menuItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}