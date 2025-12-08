package org.example.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootTest(classes = MainServiceApplicationTests.TestConfig.class)
class MainServiceApplicationTests {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
        
    }

    @Test
    void contextLoads() {
        
    }
}