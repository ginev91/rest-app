package org.example.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Minimal configuration for tests so SpringBootTest can start a context.
 *
 * The previous version imported SpringBootConfiguration from
 * org.springframework.boot.test.context which caused the "cannot find symbol"
 * compile error in your environment. Using @Configuration + @EnableAutoConfiguration
 * avoids that unresolved import while still allowing the test context to start.
 *
 * If you have a real @SpringBootApplication main class (for example:
 *   org.example.main.MainServiceApplication
 * ), prefer replacing the @SpringBootTest(...) classes value with that class:
 *   @SpringBootTest(classes = org.example.main.MainServiceApplication.class)
 */
@SpringBootTest(classes = MainServiceApplicationTests.TestConfig.class)
class MainServiceApplicationTests {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
        // empty - only used to satisfy Spring's need for a configuration class
    }

    @Test
    void contextLoads() {
        // verifies the test ApplicationContext starts successfully
    }
}