package org.example.main.mapper;

import jakarta.validation.Valid;
import org.example.main.dto.request.RegisterRequestDto;
import org.example.main.dto.response.UserProfileResponseDto;
import org.example.main.model.Role;
import org.example.main.model.User;

import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static User toEntity(@Valid RegisterRequestDto req) {
        if (req == null) return null;
        User u = new User();
        u.setUsername(req.getUsername());
        u.setFullName(req.getFullName());
        return u;
    }

    public static UserProfileResponseDto toProfile(User u) {
        if (u == null) return null;
        UserProfileResponseDto p = new UserProfileResponseDto();
        p.setId(u.getId());
        p.setUsername(u.getUsername());
        p.setFullName(u.getFullName());
        if (u.getRoles() != null) {
            p.setRoles(u.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet()));
        }
        return p;
    }
}