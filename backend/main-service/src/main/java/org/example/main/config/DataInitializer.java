package org.example.main.config;

import org.example.main.model.*;
import org.example.main.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.example.main.model.enums.TableStatus;



import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestaurantTableRepository tableRepository;


    public DataInitializer(CategoryRepository categoryRepository,
                           MenuItemRepository menuItemRepository,
                           RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           RestaurantTableRepository tableRepository) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tableRepository = tableRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedCategoriesAndMenuItems();
        seedRolesAndUsers();
        seedTablesIfNeeded();
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
                                .macros(new Macros(42, 10, 5))
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
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_EMPLOYEE").build()));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        // Use the same username when checking and creating the user
        userRepository.findByUsername("admin@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin@test.com");
            u.setPasswordHash(passwordEncoder.encode("adminpass"));
            u.setFullName("System Administrator");
            // store role as a single String (role name)
            u.setRole(adminRole.getName());
            return userRepository.save(u);
        });

        userRepository.findByUsername("employee@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("employee@test.com");
            u.setPasswordHash(passwordEncoder.encode("emppass"));
            u.setFullName("Employee User");
            u.setRole(employeeRole.getName());
            return userRepository.save(u);
        });

        userRepository.findByUsername("user@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("user@test.com");
            u.setPasswordHash(passwordEncoder.encode("userpass"));
            u.setFullName("Regular User");
            u.setRole(userRole.getName());
            return userRepository.save(u);
        });
    }

    private void seedTablesIfNeeded() {
        if (tableRepository.count() == 0) {
            List<RestaurantTable> tables = List.of(
                    RestaurantTable.builder().code("T1").seats(2).currentOccupancy(0).status(TableStatus.AVAILABLE).build(),
                    RestaurantTable.builder().code("T2").seats(2).currentOccupancy(0).status(TableStatus.AVAILABLE).build(),
                    RestaurantTable.builder().code("T3").seats(4).currentOccupancy(0).status(TableStatus.AVAILABLE).build(),
                    RestaurantTable.builder().code("T4").seats(4).currentOccupancy(0).status(TableStatus.AVAILABLE).build(),
                    RestaurantTable.builder().code("T5").seats(6).currentOccupancy(0).status(TableStatus.AVAILABLE).build()
            );
            tableRepository.saveAll(tables);
        }
    }
}