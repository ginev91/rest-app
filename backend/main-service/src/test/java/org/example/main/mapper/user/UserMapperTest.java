package org.example.main.mapper.user;

import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.UserProfileResponseDto;
import org.example.main.model.role.Role;
import org.example.main.model.user.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class UserMapperTest {

    @Test
    void toEntity_and_toProfile_handlesNulls_and_roles() {

        assertThat(UserMapper.toEntity(null)).isNull();
        assertThat(UserMapper.toProfile(null)).isNull();

        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("alice");
        req.setFullName("Alice Doe");

        User u = UserMapper.toEntity(req);
        assertThat(u).isNotNull();
        assertThat(u.getUsername()).isEqualTo("alice");
        assertThat(u.getFullName()).isEqualTo("Alice Doe");


        User user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        user.setUsername("bob");
        user.setFullName("Bob Smith");
        // use Role entity instead of raw String
        Role role = new Role();
        role.setName("ADMIN");
        user.setRole(role);

        UserProfileResponseDto profile = UserMapper.toProfile(user);
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(profile.getUsername()).isEqualTo("bob");
        assertThat(profile.getRoles()).containsExactly("ADMIN");

        user.setRole(null);
        UserProfileResponseDto profile2 = UserMapper.toProfile(user);
        assertThat(profile2.getRoles()).isEmpty();
    }
}