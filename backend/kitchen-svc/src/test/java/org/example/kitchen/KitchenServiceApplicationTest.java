package org.example.kitchen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = KitchenServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class KitchenServiceApplicationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void mainStartsAndStops() {
        ConfigurableApplicationContext ctx = null;
        try {
            ctx = SpringApplication.run(KitchenServiceApplication.class,
                    "--spring.main.web-application-type=none",
                    "--spring.profiles.active=test");
            assertThat(ctx).isNotNull();
            assertThat(ctx.isActive()).isTrue();
        } finally {
            if (ctx != null) {
                SpringApplication.exit(ctx, () -> 0);
            }
        }
    }
}