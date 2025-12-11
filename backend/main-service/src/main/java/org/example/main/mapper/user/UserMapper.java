package org.example.main.mapper.user;

import jakarta.validation.Valid;
import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.UserProfileResponseDto;
import org.example.main.model.user.User;

import java.util.Collections;

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
        if (u.getRole() != null && u.getRole().getName() != null && !u.getRole().getName().isBlank()) {
            p.setRoles(Collections.singleton(u.getRole().getName()));
        } else {
            p.setRoles(Collections.emptySet());
        }
        return p;
    }
}