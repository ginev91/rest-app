package org.example.main.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enable Spring caching for the Main application.
 * You can keep the default cache manager or configure a specific one (EhCache/Redis) if you want a bonus.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}