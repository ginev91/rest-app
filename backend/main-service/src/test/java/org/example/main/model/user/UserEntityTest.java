package org.example.main.model.user;

import org.example.main.model.role.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserEntityTest {

    @Test
    void user_fields_and_sessionTableNumber() {
        User u = new User();
        u.setUsername("alice");
        u.setPasswordHash("pw");
        u.setFullName("Alice");
        // create Role via setter (avoid non-existing single-arg ctor)
        Role role = new Role();
        role.setName("ADMIN");
        u.setRole(role);
        u.setSessionTableNumber(12);

        assertThat(u.getUsername()).isEqualTo("alice");
        assertThat(u.getPasswordHash()).isEqualTo("pw");
        assertThat(u.getFullName()).isEqualTo("Alice");
        // assert role name directly
        assertThat(u.getRole()).isNotNull();
        assertThat(u.getRole().getName()).isEqualTo("ADMIN");
        assertThat(u.getSessionTableNumber()).isEqualTo(12);
    }
}