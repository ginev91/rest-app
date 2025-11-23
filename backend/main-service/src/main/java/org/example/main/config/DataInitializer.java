package org.example.main.config;

import org.example.main.model.CategoryEntity;
import org.example.main.model.MenuItem;
import org.example.main.model.Macros;
import org.example.main.model.Role;
import org.example.main.model.User;
import org.example.main.repository.CategoryRepository;
import org.example.main.repository.MenuItemRepository;
import org.example.main.repository.RoleRepository;
import org.example.main.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategoryRepository categoryRepository,
                           MenuItemRepository menuItemRepository,
                           RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedCategoriesAndMenuItems();
        seedRolesAndUsers();
    }

    private void seedCategoriesAndMenuItems() {
        if (categoryRepository.count() == 0) {
            CategoryEntity mainCourse = categoryRepository.save(CategoryEntity.builder().name("Main Course").build());
            CategoryEntity salads = categoryRepository.save(CategoryEntity.builder().name("Salads").build());

            if (menuItemRepository.count() == 0) {
                List<MenuItem> items = List.of(
                        MenuItem.builder()
                                .name("Grilled Chicken Breast")
                                .description("Tender grilled chicken with herbs and lemon")
                                .price(BigDecimal.valueOf(18.99))
                                .category(mainCourse)
                                .calories(350)
                                .macros(new Macros(42, 10, 5)) // protein, fat, carbs
                                .available(true)
                                .build(),
                        MenuItem.builder()
                                .name("Caesar Salad")
                                .description("Fresh romaine lettuce, parmesan, croutons")
                                .price(BigDecimal.valueOf(12.99))
                                .category(salads)
                                .calories(280)
                                .macros(new Macros(8, 18, 12))
                                .available(true)
                                .build(),
                        MenuItem.builder()
                                .name("Salmon Fillet")
                                .description("Pan-seared salmon with seasonal vegetables")
                                .price(BigDecimal.valueOf(24.99))
                                .category(mainCourse)
                                .calories(420)
                                .macros(new Macros(38, 25, 2))
                                .available(true)
                                .build(),
                        MenuItem.builder()
                                .name("Greek Salad")
                                .description("Tomatoes, cucumber, olives, feta cheese")
                                .price(BigDecimal.valueOf(11.99))
                                .category(salads)
                                .calories(180)
                                .macros(new Macros(6, 12, 10))
                                .available(true)
                                .build()
                );

                menuItemRepository.saveAll(items);
            }
        } else {
            if (categoryRepository.findByName("Main Course").isEmpty()) categoryRepository.save(CategoryEntity.builder().name("Main Course").build());
            if (categoryRepository.findByName("Salads").isEmpty()) categoryRepository.save(CategoryEntity.builder().name("Salads").build());
        }
    }

    private void seedRolesAndUsers() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_EMPLOYEE").build()));
        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setPasswordHash(passwordEncoder.encode("adminpass")); // for now using default pass
            u.setFullName("System Administrator");
            u.setRoles(Set.of(adminRole));
            return userRepository.save(u);
        });

        userRepository.findByUsername("employee").orElseGet(() -> {
            User u = new User();
            u.setUsername("employee");
            u.setPasswordHash(passwordEncoder.encode("emppass")); // for now using default pass
            u.setFullName("Employee User");
            u.setRoles(Set.of(employeeRole));
            return userRepository.save(u);
        });
    }
}