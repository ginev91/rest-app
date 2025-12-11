package org.example.main.dto.response;

import org.example.main.dto.response.user.AuthResponseDto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AuthResponseDtoTest {

    @Test
    void builder_and_getters_work() {
        UUID uid = UUID.randomUUID();
        UUID tid = UUID.randomUUID();

        AuthResponseDto dto = AuthResponseDto.builder()
                .token("tok")
                .username("alice")
                .userId(uid)
                .role("ROLE_USER")
                .tableNumber(5)
                .tableId(tid)
                .build();

        assertThat(dto.getToken()).isEqualTo("tok");
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getUserId()).isEqualTo(uid);
        assertThat(dto.getRole()).isEqualTo("ROLE_USER");
        assertThat(dto.getTableNumber()).isEqualTo(5);
        assertThat(dto.getTableId()).isEqualTo(tid);
    }

    @Test
    void convenienceConstructor_setsExpectedFields() {
        UUID uid = UUID.randomUUID();
        AuthResponseDto dto = new AuthResponseDto("t", "bob", uid, "ROLE_ADMIN");

        assertThat(dto.getToken()).isEqualTo("t");
        assertThat(dto.getUsername()).isEqualTo("bob");
        assertThat(dto.getUserId()).isEqualTo(uid);
        assertThat(dto.getRole()).isEqualTo("ROLE_ADMIN");

        // optional fields remain null when not provided
        assertThat(dto.getTableNumber()).isNull();
        assertThat(dto.getTableId()).isNull();
    }

    @Test
    void equals_and_hashCode_considerFields() {
        UUID uid = UUID.randomUUID();

        AuthResponseDto a = AuthResponseDto.builder()
                .token("x")
                .username("u")
                .userId(uid)
                .role("R")
                .build();

        AuthResponseDto b = AuthResponseDto.builder()
                .token("x")
                .username("u")
                .userId(uid)
                .role("R")
                .build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        b.setToken("y");
        assertThat(a).isNotEqualTo(b);
    }
}