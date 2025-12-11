package org.example.main.model.role;

import org.example.main.model.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RoleTest {

    @Test
    void users_list_defaults_and_setter() {
        Role r = new Role();
        assertThat(r.getUsers()).isNotNull();
        assertThat(r.getUsers()).isEmpty();

        User u = new User();
        u.setUsername("u1");
        r.getUsers().add(u);
        assertThat(r.getUsers()).hasSize(1);
    }
}