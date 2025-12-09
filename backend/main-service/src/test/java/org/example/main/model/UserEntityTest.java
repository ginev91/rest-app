package org.example.main.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserEntityTest {

    @Test
    void user_fields_and_sessionTableNumber() {
        User u = new User();
        u.setUsername("alice");
        u.setPasswordHash("pw");
        u.setFullName("Alice");
        u.setRole("ADMIN");
        u.setSessionTableNumber(12);

        assertThat(u.getUsername()).isEqualTo("alice");
        assertThat(u.getPasswordHash()).isEqualTo("pw");
        assertThat(u.getFullName()).isEqualTo("Alice");
        assertThat(u.getRole()).isEqualTo("ADMIN");
        assertThat(u.getSessionTableNumber()).isEqualTo(12);
    }
}