package org.example.main.controller;

import org.example.main.dto.response.RoleResponseDto;
import org.example.main.mapper.RoleMapper;
import org.example.main.service.IRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@Validated
public class RoleController {

    private final IRoleService roleService;
    private final RoleMapper mapper;

    
    public RoleController(IRoleService roleService, RoleMapper mapper) {
        this.roleService = roleService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<RoleResponseDto>> list() {
        return ResponseEntity.ok(mapper.toDtoList(roleService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponseDto> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(mapper.toDto(roleService.findById(id)));
    }
}