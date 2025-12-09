package org.example.main.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;

class PasswordConfigTest {

    @Test
    void passwordEncoder_isBCrypt_and_matchesPasswords() {
        PasswordConfig cfg = new PasswordConfig();
        PasswordEncoder enc = cfg.passwordEncoder();
        assertThat(enc).isInstanceOf(BCryptPasswordEncoder.class);

        String raw = "s3cr3t!";
        String hashed = enc.encode(raw);
        assertThat(hashed).isNotNull();
        assertThat(enc.matches(raw, hashed)).isTrue();
    }
}