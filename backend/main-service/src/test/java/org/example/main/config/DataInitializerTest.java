package org.example.main.config;

import org.example.main.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;


import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    CategoryRepository categoryRepository;
    @Mock
    MenuItemRepository menuItemRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    OrderItemRepository orderItemRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    RestaurantTableRepository tableRepository;
    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    ApplicationArguments args;

    @Test
    void run_skips_when_menu_items_table_missing() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("menu_items"))).thenReturn(0);

        DataInitializer di = new DataInitializer(categoryRepository, menuItemRepository, roleRepository,
                userRepository, orderItemRepository, passwordEncoder, tableRepository, jdbcTemplate);

        di.run(args);

        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("menu_items"));
        verifyNoInteractions(categoryRepository, menuItemRepository, roleRepository, userRepository, orderItemRepository, tableRepository);
    }

    @Test
    void run_skips_when_item_type_column_missing() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("menu_items"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class))).thenReturn(0);

        DataInitializer di = new DataInitializer(categoryRepository, menuItemRepository, roleRepository,
                userRepository, orderItem_repository_or_cast(orderItemRepository), passwordEncoder, table_repository_or_cast(tableRepository), jdbcTemplate);

        di.run(args);

        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("menu_items"));
        verify(jdbcTemplate).queryForObject(contains("information_schema.columns"), eq(Integer.class));
        verifyNoInteractions(roleRepository, userRepository, tableRepository);
    }

    @Test
    void run_reinitialize_orderItems_delete_falls_back_to_jdbc_when_repo_delete_throws() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("menu_items"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("order_items"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class))).thenReturn(1);

        doThrow(new RuntimeException("delete-fail")).when(orderItemRepository).deleteAll();
        when(categoryRepository.count()).thenReturn(1L);
        when(menuItemRepository.count()).thenReturn(1L);

        DataInitializer di = new DataInitializer(categoryRepository, menuItemRepository, role_repository_or_cast(roleRepository),
                user_repository_or_cast(userRepository), orderItemRepository, passwordEncoder, tableRepository, jdbcTemplate);

        Field f = DataInitializer.class.getDeclaredField("reinitialize");
        f.setAccessible(true);
        f.setBoolean(di, true);

        di.run(args);

        verify(orderItemRepository).deleteAll();
        verify(jdbcTemplate).update(eq("DELETE FROM order_items"));
    }

    private static OrderItemRepository orderItem_repository_or_cast(OrderItemRepository r) { return r; }
    private static RestaurantTableRepository table_repository_or_cast(RestaurantTableRepository r) { return r; }
    private static RoleRepository role_repository_or_cast(RoleRepository r) { return r; }
    private static UserRepository user_repository_or_cast(UserRepository r) { return r; }
}