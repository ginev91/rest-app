package org.example.main.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisCacheConfigTest {

    @Test
    void cacheManagerProvidesCacheInstances() {
        RedisCacheConfig cfg = new RedisCacheConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        CacheManager manager = cfg.cacheManager(connectionFactory);
        assertThat(manager).isInstanceOf(RedisCacheManager.class);

        assertThat(manager.getCache("recommendations")).isNotNull();
        assertThat(manager.getCache("anyCacheName")).isNotNull();
    }
}