package org.example.main.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConfigTest {

    @Test
    void passwordEncoderBeanProvided() {
        PasswordConfig cfg = new PasswordConfig();
        PasswordEncoder encoder = cfg.passwordEncoder();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String encoded = encoder.encode("password");
        assertThat(encoder.matches("password", encoded)).isTrue();
    }
}