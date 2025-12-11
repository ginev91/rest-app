package org.example.main.config;

import org.example.main.model.category.CategoryEntity;
import org.example.main.model.enums.Macros;
import org.example.main.model.menu.MenuItem;
import org.example.main.model.role.Role;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.user.User;
import org.example.main.repository.category.CategoryRepository;
import org.example.main.repository.menu.MenuItemRepository;
import org.example.main.repository.order.OrderItemRepository;
import org.example.main.repository.role.RoleRepository;
import org.example.main.repository.table.RestaurantTableRepository;
import org.example.main.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.example.main.model.enums.TableStatus;
import org.example.main.model.enums.ItemType;
import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestaurantTableRepository tableRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.reinitialize:false}")
    private boolean reinitialize;

    public DataInitializer(CategoryRepository categoryRepository,
                           MenuItemRepository menuItemRepository,
                           RoleRepository roleRepository,
                           UserRepository userRepository,
                           OrderItemRepository orderItemRepository,
                           PasswordEncoder passwordEncoder,
                           RestaurantTableRepository tableRepository,
                           JdbcTemplate jdbcTemplate) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.tableRepository = tableRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
                    Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception ex) {
            log.warn("tableExists check failed for {}: {}", tableName, ex.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!tableExists("menu_items")) {
            log.warn("DataInitializer: table 'menu_items' not present — skipping initialization.");
            return;
        }

        if (reinitialize) {
            try {
                log.info("DataInitializer: reinitialize requested — deleting dependent rows in correct order.");

                if (tableExists("order_items")) {
                    try {
                        log.info("DataInitializer: deleting all order_items...");
                        orderItemRepository.deleteAll();
                        log.info("DataInitializer: order_items cleared.");
                    } catch (Exception ex) {
                        log.warn("DataInitializer: orderItemRepository.deleteAll() failed: {}", ex.getMessage());
                        try {
                            jdbcTemplate.update("DELETE FROM order_items");
                            log.info("DataInitializer: order_items deleted via JDBC fallback.");
                        } catch (Exception sqlEx) {
                            log.warn("DataInitializer: JDBC delete order_items also failed: {}", sqlEx.getMessage());
                        }
                    }
                } else {
                    log.info("DataInitializer: table 'order_items' not present, skipping dependent deletion.");
                }

                try {
                    menuItemRepository.deleteAll();
                    log.info("DataInitializer: menu_items cleared.");
                } catch (Exception ex) {
                    log.warn("DataInitializer: menuItemRepository.deleteAll() failed: {}", ex.getMessage());
                }

                
                if (tableExists("categories")) {
                    try {
                        categoryRepository.deleteAll();
                        log.info("DataInitializer: categories cleared.");
                    } catch (Exception ex) {
                        log.warn("DataInitializer: categoryRepository.deleteAll() failed: {}", ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.warn("DataInitializer: reinitialize cleanup encountered error: {}", ex.getMessage());
            }
        }

        
        try {
            Integer cols = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'menu_items' AND column_name = 'item_type'",
                    Integer.class);
            if (cols == null || cols == 0) {
                log.warn("DataInitializer: required column menu_items.item_type missing — skipping seeding.");
                return;
            }
        } catch (Exception ex) {
            log.warn("DataInitializer: unable to verify columns: {}", ex.getMessage());
            return;
        }

        
        try {
            seedCategoriesAndMenuItems();
            seedRolesAndUsers();
            seedTablesIfNeeded();
            log.info("DataInitializer: seeding finished.");
        } catch (Exception ex) {
            log.warn("DataInitializer: seeding failed, skipping. Cause: {}", ex.getMessage());
        }
    }


    private void seedCategoriesAndMenuItems() {
        if (categoryRepository.count() == 0) {
            CategoryEntity mainCourse = categoryRepository.save(CategoryEntity.builder().name("Main Course").build());
            CategoryEntity salads = categoryRepository.save(CategoryEntity.builder().name("Salads").build());
            CategoryEntity drinks = categoryRepository.save(CategoryEntity.builder().name("Drinks").build());

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
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Beef Steak with Chimichurri")
                                .description("Seared sirloin steak with chimichurri sauce")
                                .price(BigDecimal.valueOf(26.50))
                                .category(mainCourse)
                                .calories(620)
                                .macros(new Macros(55, 12, 30))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Lemon Herb Salmon")
                                .description("Oven-roasted salmon with lemon and dill")
                                .price(BigDecimal.valueOf(24.99))
                                .category(mainCourse)
                                .calories(480)
                                .macros(new Macros(40, 8, 28))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Pasta Primavera")
                                .description("Penne with seasonal vegetables and light tomato sauce")
                                .price(BigDecimal.valueOf(16.99))
                                .category(mainCourse)
                                .calories(540)
                                .macros(new Macros(15, 80, 12))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("BBQ Ribs")
                                .description("Slow-cooked ribs with house BBQ glaze")
                                .price(BigDecimal.valueOf(22.50))
                                .category(mainCourse)
                                .calories(800)
                                .macros(new Macros(60, 30, 50))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Vegetable Stir-Fry with Tofu")
                                .description("Wok-tossed seasonal veggies and marinated tofu")
                                .price(BigDecimal.valueOf(14.99))
                                .category(mainCourse)
                                .calories(420)
                                .macros(new Macros(18, 60, 10))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Chicken Parmesan")
                                .description("Breaded chicken, marinara, melted cheese")
                                .price(BigDecimal.valueOf(19.99))
                                .category(mainCourse)
                                .calories(700)
                                .macros(new Macros(48, 50, 28))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Shrimp Scampi")
                                .description("Garlic butter shrimp with linguine")
                                .price(BigDecimal.valueOf(21.99))
                                .category(mainCourse)
                                .calories(560)
                                .macros(new Macros(30, 45, 22))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Stuffed Bell Peppers")
                                .description("Peppers stuffed with rice, veggies and cheese")
                                .price(BigDecimal.valueOf(15.50))
                                .category(mainCourse)
                                .calories(460)
                                .macros(new Macros(20, 55, 18))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Lamb Kofta")
                                .description("Spiced lamb skewers with yogurt sauce")
                                .price(BigDecimal.valueOf(23.00))
                                .category(mainCourse)
                                .calories(610)
                                .macros(new Macros(45, 20, 34))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),

                        
                        MenuItem.builder()
                                .name("Caesar Salad")
                                .description("Fresh romaine lettuce, parmesan, croutons")
                                .price(BigDecimal.valueOf(12.99))
                                .category(salads)
                                .calories(280)
                                .macros(new Macros(8, 18, 12))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Greek Salad")
                                .description("Tomatoes, cucumber, olives, feta cheese")
                                .price(BigDecimal.valueOf(11.99))
                                .category(salads)
                                .calories(180)
                                .macros(new Macros(6, 12, 10))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Quinoa & Roasted Veg Salad")
                                .description("Quinoa, roasted vegetables, lemon vinaigrette")
                                .price(BigDecimal.valueOf(13.50))
                                .category(salads)
                                .calories(350)
                                .macros(new Macros(12, 45, 9))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Caprese Salad")
                                .description("Tomato, fresh mozzarella, basil, olive oil")
                                .price(BigDecimal.valueOf(12.50))
                                .category(salads)
                                .calories(320)
                                .macros(new Macros(14, 8, 24))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Spinach & Strawberry Salad")
                                .description("Baby spinach, strawberries, nuts, light dressing")
                                .price(BigDecimal.valueOf(11.50))
                                .category(salads)
                                .calories(220)
                                .macros(new Macros(6, 28, 10))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Kale & Avocado Salad")
                                .description("Kale, avocado, sunflower seeds, citrus dressing")
                                .price(BigDecimal.valueOf(13.99))
                                .category(salads)
                                .calories(360)
                                .macros(new Macros(8, 20, 24))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Warm Goat Cheese Salad")
                                .description("Mixed greens, warm goat cheese, walnuts")
                                .price(BigDecimal.valueOf(14.99))
                                .category(salads)
                                .calories(330)
                                .macros(new Macros(10, 22, 18))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Nicoise Salad")
                                .description("Tuna, potatoes, green beans, olives")
                                .price(BigDecimal.valueOf(15.50))
                                .category(salads)
                                .calories(420)
                                .macros(new Macros(28, 30, 16))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Asian Sesame Chicken Salad")
                                .description("Grilled chicken, greens, sesame dressing")
                                .price(BigDecimal.valueOf(14.50))
                                .category(salads)
                                .calories(390)
                                .macros(new Macros(32, 24, 14))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),
                        MenuItem.builder()
                                .name("Mediterranean Chickpea Salad")
                                .description("Chickpeas, cucumber, tomato, herbs")
                                .price(BigDecimal.valueOf(12.00))
                                .category(salads)
                                .calories(300)
                                .macros(new Macros(12, 38, 8))
                                .available(true)
                                .itemType(ItemType.KITCHEN)
                                .build(),

                        
                        MenuItem.builder()
                                .name("Fresh Lemonade")
                                .description("House-made lemonade with fresh lemons")
                                .price(BigDecimal.valueOf(4.50))
                                .category(drinks)
                                .calories(120)
                                .macros(new Macros(0, 30, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("Iced Tea")
                                .description("Brewed iced tea, unsweetened or sweetened")
                                .price(BigDecimal.valueOf(3.50))
                                .category(drinks)
                                .calories(80)
                                .macros(new Macros(0, 20, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("Coca-Cola")
                                .description("Classic cola")
                                .price(BigDecimal.valueOf(3.00))
                                .category(drinks)
                                .calories(140)
                                .macros(new Macros(0, 39, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("Orange Juice")
                                .description("Freshly squeezed orange juice")
                                .price(BigDecimal.valueOf(4.00))
                                .category(drinks)
                                .calories(150)
                                .macros(new Macros(2, 34, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("Espresso")
                                .description("Single shot espresso")
                                .price(BigDecimal.valueOf(2.50))
                                .category(drinks)
                                .calories(5)
                                .macros(new Macros(0, 1, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("Cappuccino")
                                .description("Espresso with steamed milk and foam")
                                .price(BigDecimal.valueOf(4.25))
                                .category(drinks)
                                .calories(120)
                                .macros(new Macros(6, 10, 6))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("Mojito")
                                .description("Rum, lime, mint, soda")
                                .price(BigDecimal.valueOf(8.50))
                                .category(drinks)
                                .calories(200)
                                .macros(new Macros(0, 14, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("Red Wine (Glass)")
                                .description("House red wine, 150ml")
                                .price(BigDecimal.valueOf(7.00))
                                .category(drinks)
                                .calories(125)
                                .macros(new Macros(0, 4, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("White Wine (Glass)")
                                .description("House white wine, 150ml")
                                .price(BigDecimal.valueOf(7.00))
                                .category(drinks)
                                .calories(120)
                                .macros(new Macros(0, 3, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build(),
                        MenuItem.builder()
                                .name("Craft Beer")
                                .description("Local craft beer, 330ml")
                                .price(BigDecimal.valueOf(6.50))
                                .category(drinks)
                                .calories(180)
                                .macros(new Macros(0, 12, 0))
                                .available(true)
                                .itemType(ItemType.BAR)
                                .build()
                );

                menuItemRepository.saveAll(items);
            }
        } else {
            if (categoryRepository.findByName("Main Course").isEmpty()) categoryRepository.save(CategoryEntity.builder().name("Main Course").build());
            if (categoryRepository.findByName("Salads").isEmpty()) categoryRepository.save(CategoryEntity.builder().name("Salads").build());
            if (categoryRepository.findByName("Drinks").isEmpty()) categoryRepository.save(CategoryEntity.builder().name("Drinks").build());
        }
    }

    private void seedRolesAndUsers() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_EMPLOYEE").build()));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        userRepository.findByUsername("admin@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin@test.com");
            u.setPasswordHash(passwordEncoder.encode("adminpass"));
            u.setFullName("System Administrator");

            // assign the Role entity, not the role name string
            u.setRole(adminRole);
            return userRepository.save(u);
        });

        userRepository.findByUsername("employee@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("employee@test.com");
            u.setPasswordHash(passwordEncoder.encode("emppass"));
            u.setFullName("Employee User");

            // assign the Role entity, not the role name string
            u.setRole(employeeRole);
            return userRepository.save(u);
        });

        userRepository.findByUsername("user@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("user@test.com");
            u.setPasswordHash(passwordEncoder.encode("userpass"));
            u.setFullName("Regular User");

            // assign the Role entity, not the role name string
            u.setRole(userRole);
            return userRepository.save(u);
        });
    }
    private void seedTablesIfNeeded() {
        if (tableRepository.count() == 0) {
            List<RestaurantTable> tables = List.of(
                    RestaurantTable.builder().code("T1").seats(2).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("1111").build(),
                    RestaurantTable.builder().code("T2").seats(2).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("2222").build(),
                    RestaurantTable.builder().code("T3").seats(4).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("3333").build(),
                    RestaurantTable.builder().code("T4").seats(4).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("4444").build(),
                    RestaurantTable.builder().code("T5").seats(6).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("5555").build()
            );
            tableRepository.saveAll(tables);
        }
    }
}
