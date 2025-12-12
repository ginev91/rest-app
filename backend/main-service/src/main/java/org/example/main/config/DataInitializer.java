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
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.example.main.model.enums.TableStatus;
import org.example.main.model.enums.ItemType;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("!test")
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
        
        boolean needKitchen = categoryRepository.findByItemType(ItemType.KITCHEN).isEmpty();
        boolean needBar = categoryRepository.findByItemType(ItemType.BAR).isEmpty();

        CategoryEntity kitchenCat = null;
        CategoryEntity barCat = null;

        if (needKitchen) {
            kitchenCat = categoryRepository.save(CategoryEntity.builder().itemType(ItemType.KITCHEN).build());
        } else {
            kitchenCat = categoryRepository.findByItemType(ItemType.KITCHEN).orElse(null);
        }

        if (needBar) {
            barCat = categoryRepository.save(CategoryEntity.builder().itemType(ItemType.BAR).build());
        } else {
            barCat = categoryRepository.findByItemType(ItemType.BAR).orElse(null);
        }

        if (menuItemRepository.count() == 0) {
            List<MenuItem> items = List.of(
                    MenuItem.builder()
                            .name("Grilled Chicken Breast")
                            .description("Tender grilled chicken with herbs and lemon")
                            .price(BigDecimal.valueOf(18.99))
                            .category(kitchenCat)
                            .calories(350)
                            .macros(new Macros(42, 10, 5))
                            .available(true)
                            .itemType(ItemType.KITCHEN)
                            .build(),
                    MenuItem.builder()
                            .name("Beef Steak with Chimichurri")
                            .description("Seared sirloin steak with chimichurri sauce")
                            .price(BigDecimal.valueOf(26.50))
                            .category(kitchenCat)
                            .calories(620)
                            .macros(new Macros(55, 12, 30))
                            .available(true)
                            .itemType(ItemType.KITCHEN)
                            .build(),
                    MenuItem.builder()
                            .name("Caesar Salad")
                            .description("Fresh romaine lettuce, parmesan, croutons")
                            .price(BigDecimal.valueOf(12.99))
                            .category(kitchenCat)
                            .calories(280)
                            .macros(new Macros(8, 18, 12))
                            .available(true)
                            .itemType(ItemType.KITCHEN)
                            .build(),
                    MenuItem.builder()
                            .name("Fresh Lemonade")
                            .description("House-made lemonade with fresh lemons")
                            .price(BigDecimal.valueOf(4.50))
                            .category(barCat)
                            .calories(120)
                            .macros(new Macros(0, 30, 0))
                            .available(true)
                            .itemType(ItemType.BAR)
                            .build(),
                    MenuItem.builder()
                            .name("Iced Tea")
                            .description("Brewed iced tea, unsweetened or sweetened")
                            .price(BigDecimal.valueOf(3.50))
                            .category(barCat)
                            .calories(80)
                            .macros(new Macros(0, 20, 0))
                            .available(true)
                            .itemType(ItemType.BAR)
                            .build(),
                    MenuItem.builder()
                            .name("Craft Beer")
                            .description("Local craft beer, 330ml")
                            .price(BigDecimal.valueOf(6.50))
                            .category(barCat)
                            .calories(180)
                            .macros(new Macros(0, 12, 0))
                            .available(true)
                            .itemType(ItemType.BAR)
                            .build()
            );

            menuItemRepository.saveAll(items);
            log.info("DataInitializer: seeded {} menu items.", items.size());
        } else {
            
            if (kitchenCat == null && categoryRepository.findByItemType(ItemType.KITCHEN).isEmpty()) {
                categoryRepository.save(CategoryEntity.builder().itemType(ItemType.KITCHEN).build());
            }
            if (barCat == null && categoryRepository.findByItemType(ItemType.BAR).isEmpty()) {
                categoryRepository.save(CategoryEntity.builder().itemType(ItemType.BAR).build());
            }
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
            u.setRole(adminRole);
            return userRepository.save(u);
        });

        userRepository.findByUsername("employee@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("employee@test.com");
            u.setPasswordHash(passwordEncoder.encode("emppass"));
            u.setFullName("Employee User");
            u.setRole(employeeRole);
            return userRepository.save(u);
        });

        userRepository.findByUsername("user@test.com").orElseGet(() -> {
            User u = new User();
            u.setUsername("user@test.com");
            u.setPasswordHash(passwordEncoder.encode("userpass"));
            u.setFullName("Regular User");
            u.setRole(userRole);
            return userRepository.save(u);
        });
    }

    private void seedTablesIfNeeded() {
        if (tableRepository.count() == 0) {
            List<RestaurantTable> tables = List.of(
                    RestaurantTable.builder().code("T1").tableNumber(1).seats(2).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("1111").build(),
                    RestaurantTable.builder().code("T2").tableNumber(2).seats(2).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("2222").build(),
                    RestaurantTable.builder().code("T3").tableNumber(3).seats(4).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("3333").build(),
                    RestaurantTable.builder().code("T4").tableNumber(4).seats(4).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("4444").build(),
                    RestaurantTable.builder().code("T5").tableNumber(5).seats(6).currentOccupancy(0).status(TableStatus.AVAILABLE).pinCode("5555").build()
            );
            tableRepository.saveAll(tables);
        }
    }
}