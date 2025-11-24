package org.example.main.mapper;

import jakarta.validation.Valid;
import org.example.main.dto.request.MenuItemRequestDto;
import org.example.main.dto.request.MenuItemRequestDto;
import org.example.main.dto.response.MenuItemResponseDto;
import org.example.main.dto.response.MenuItemResponseDto;
import org.example.main.model.MenuItem;

import java.util.Objects;

public final class MenuItemMapper {

    private MenuItemMapper() {}

    public static MenuItem toEntity(@Valid MenuItemRequestDto req) {
        if (req == null) return null;
        MenuItem mi = new MenuItem();
        mi.setName(req.getName());
        mi.setDescription(req.getDescription());
        mi.setPrice(req.getPrice());
        return mi;
    }

    public static MenuItemResponseDto toResponse(MenuItem mi) {
        if (mi == null) return null;
        MenuItemResponseDto r = new MenuItemResponseDto();
        r.setId(mi.getId());
        r.setName(mi.getName());
        r.setDescription(mi.getDescription());
        r.setPrice(mi.getPrice());
        return r;
    }

    public static void copyToEntity(@Valid MenuItemRequestDto req, MenuItem target) {
        if (req == null || target == null) return;
        if (Objects.nonNull(req.getName())) target.setName(req.getName());
        target.setDescription(req.getDescription());
        if (Objects.nonNull(req.getPrice())) target.setPrice(req.getPrice());
    }
}