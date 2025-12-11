package org.example.main.mapper.role;

import org.example.main.dto.response.role.RoleResponseDto;
import org.example.main.model.role.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RoleMapperTest {

    @Test
    void toDto_and_toDtoList_handlesNulls() {
        RoleMapper mapper = new RoleMapper();

        assertThat(mapper.toDto(null)).isNull();

        Role role = new Role();
        role.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        role.setName("ADMIN");

        RoleResponseDto dto = mapper.toDto(role);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(dto.getName()).isEqualTo("ADMIN");

        assertThat(mapper.toDtoList(null)).isNotNull().isEmpty();

        List<RoleResponseDto> list = mapper.toDtoList(List.of(role));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("ADMIN");
    }
}