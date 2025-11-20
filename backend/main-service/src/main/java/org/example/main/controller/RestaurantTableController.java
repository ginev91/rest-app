package org.example.main.controller;

import org.example.main.model.RestaurantTable;
import org.example.main.service.IRestaurantTableService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
@Validated
public class RestaurantTableController {

    private final IRestaurantTableService tableService;

    public RestaurantTableController(IRestaurantTableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    public ResponseEntity<List<RestaurantTable>> list() {
        return ResponseEntity.ok(tableService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantTable> get(@PathVariable UUID id) {
        return ResponseEntity.ok(tableService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RestaurantTable> create(@Valid @RequestBody RestaurantTable request) {
        RestaurantTable created = tableService.create(request);
        return ResponseEntity.created(URI.create("/api/tables/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantTable> update(@PathVariable UUID id,
                                                  @Valid @RequestBody RestaurantTable changes) {
        return ResponseEntity.ok(tableService.update(id, changes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}