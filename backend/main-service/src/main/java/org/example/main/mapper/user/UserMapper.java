package org.example.main.mapper.user;

import jakarta.validation.Valid;
import lombok.experimental.UtilityClass;
import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.UserProfileResponseDto;
import org.example.main.model.role.Role;
import org.example.main.model.user.User;

@UtilityClass
public class UserMapper {

    public static User toEntity(@Valid RegisterRequestDto req) {
        if (req == null) return null;
        User u = new User();
        u.setUsername(req.getUsername());
        u.setFullName(req.getFullName());

        if (req.getRoleName() != null && !req.getRoleName().isBlank()) {
            Role r = new Role();
            r.setName(req.getRoleName().trim());
            u.setRole(r);
        }

        return u;
    }

    public static UserProfileResponseDto toProfile(User u) {
        if (u == null) return null;
        return UserProfileResponseDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .role(u.getRole() != null ? u.getRole().getName() : null)
                .blocked(u.getBlocked() != null ? u.getBlocked() : Boolean.FALSE)
                .build();
    }
}