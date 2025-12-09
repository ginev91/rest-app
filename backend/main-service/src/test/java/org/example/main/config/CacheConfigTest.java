package org.example.main.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CacheConfigTest {

    @Test
    void cacheConfig_instantiable() {
        CacheConfig cfg = new CacheConfig();
        assertThat(cfg).isNotNull();
    }
}