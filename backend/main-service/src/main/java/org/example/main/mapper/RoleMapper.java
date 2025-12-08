package org.example.main.mapper;

import org.example.main.dto.response.RoleResponseDto;
import org.example.main.model.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple hand-written mapper for Role <-> RoleResponseDto.
 */
@Component
public class RoleMapper {

    public RoleMapper() {
    }

    public RoleResponseDto toDto(Role role) {
        if (role == null) return null;
        return RoleResponseDto.builder()
                .id(role.getId())
                .name(role.getName())
                .build();
    }

    public List<RoleResponseDto> toDtoList(List<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream().map(this::toDto).collect(Collectors.toList());
    }
}