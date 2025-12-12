package org.example.main.config;

import org.example.main.model.category.CategoryEntity;
import org.example.main.model.menu.MenuItem;
import org.example.main.model.role.Role;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.user.User;
import org.example.main.model.enums.ItemType;
import org.example.main.repository.category.CategoryRepository;
import org.example.main.repository.menu.MenuItemRepository;
import org.example.main.repository.order.OrderItemRepository;
import org.example.main.repository.role.RoleRepository;
import org.example.main.repository.table.RestaurantTableRepository;
import org.example.main.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTests {

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

    @InjectMocks
    DataInitializer initializer;

    @Mock
    ApplicationArguments args;

    private void setReinitialize(boolean val) throws Exception {
        Field f = DataInitializer.class.getDeclaredField("reinitialize");
        f.setAccessible(true);
        f.setBoolean(initializer, val);
    }

    @Test
    void run_skips_when_menu_items_table_missing() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(0);

        initializer.run(args);

        verifyNoInteractions(menuItemRepository, categoryRepository, roleRepository, userRepository, tableRepository);
    }

    @Test
    void run_reinitialize_deletes_order_items_and_menu_items() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(inv -> {
                    Object[] a = inv.getArguments();
                    if (a.length == 3) {
                        String tbl = String.valueOf(a[2]);
                        if ("menu_items".equals(tbl)) return 1;
                        if ("order_items".equals(tbl)) return 1;
                        return 0;
                    }
                    return 1;
                });

        setReinitialize(true);

        
        doNothing().when(orderItemRepository).deleteAll();
        doNothing().when(menuItemRepository).deleteAll();

        
        when(categoryRepository.findByItemType(ItemType.KITCHEN)).thenReturn(Optional.of(new CategoryEntity()));
        when(categoryRepository.findByItemType(ItemType.BAR)).thenReturn(Optional.of(new CategoryEntity()));

        
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class)))
                .thenReturn(1);

        initializer.run(args);

        verify(orderItemRepository).deleteAll();
        verify(menuItemRepository).deleteAll();
        verify(categoryRepository, never()).deleteAll();
    }

    @Test
    void run_reinitialize_order_items_delete_falls_back_to_jdbc() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(inv -> {
                    Object[] a = inv.getArguments();
                    if (a.length == 3) {
                        String tbl = String.valueOf(a[2]);
                        if ("menu_items".equals(tbl) || "order_items".equals(tbl)) return 1;
                    }
                    return 1;
                });

        setReinitialize(true);

        
        doThrow(new RuntimeException("repo-fail")).when(orderItemRepository).deleteAll();
        when(jdbcTemplate.update(startsWith("DELETE FROM order_items"))).thenReturn(1);

        doNothing().when(menuItemRepository).deleteAll();

        
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class)))
                .thenReturn(1);

        initializer.run(args);

        verify(orderItemRepository).deleteAll();
        verify(jdbcTemplate).update(contains("order_items"));
        verify(menuItemRepository).deleteAll();
    }

    @Test
    void run_skips_when_columns_missing() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(inv -> {
                    Object[] a = inv.getArguments();
                    if (a.length == 3 && "menu_items".equals(String.valueOf(a[2]))) return 1;
                    return 0;
                });

        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class))).thenReturn(0);

        initializer.run(args);

        verify(menuItemRepository, never()).saveAll(anyList());
        verify(roleRepository, never()).save(any(Role.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_full_seed_path_creates_categories_menuitems_roles_users_and_tables() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(inv -> {
                    Object[] a = inv.getArguments();
                    if (a.length == 3 && "menu_items".equals(String.valueOf(a[2]))) return 1;
                    return 0;
                });

        
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class))).thenReturn(1);

        
        when(categoryRepository.findByItemType(ItemType.KITCHEN)).thenReturn(Optional.empty());
        when(categoryRepository.findByItemType(ItemType.BAR)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(CategoryEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        
        when(menuItemRepository.count()).thenReturn(0L);
        when(menuItemRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "ENC(" + inv.getArgument(0) + ")");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        
        when(tableRepository.count()).thenReturn(0L);
        when(tableRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        initializer.run(args);

        verify(categoryRepository, atLeastOnce()).save(any(CategoryEntity.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MenuItem>> menuCaptor = ArgumentCaptor.forClass(List.class);
        verify(menuItemRepository).saveAll(menuCaptor.capture());
        List<MenuItem> savedItems = menuCaptor.getValue();
        assertThat(savedItems).isNotEmpty();

        verify(roleRepository, atLeast(1)).save(any(Role.class));
        verify(userRepository, atLeast(1)).save(any(User.class));

        verify(tableRepository).saveAll(anyList());
    }

    

    @Test
    void run_skips_when_tableExists_throws_and_logs() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenThrow(new RuntimeException("db-broken"));

        
        initializer.run(args);

        verifyNoInteractions(menuItemRepository, categoryRepository, roleRepository, userRepository, tableRepository, orderItemRepository);
    }

    @Test
    void run_reinitialize_skips_order_items_deletion_when_table_missing() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(inv -> {
                    Object[] a = inv.getArguments();
                    if (a.length == 3) {
                        String tbl = String.valueOf(a[2]);
                        if ("menu_items".equals(tbl)) return 1;
                        if ("order_items".equals(tbl)) return 0; 
                    }
                    return 1;
                });

        setReinitialize(true);

        doNothing().when(menuItemRepository).deleteAll();

        
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class))).thenReturn(1);

        initializer.run(args);

        
        verify(orderItemRepository, never()).deleteAll();
        
        verify(menuItemRepository).deleteAll();
    }

    @Test
    void run_skips_when_columnCheck_throws_exception() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(inv -> {
                    Object[] a = inv.getArguments();
                    if (a.length == 3 && "menu_items".equals(String.valueOf(a[2]))) return 1;
                    return 0;
                });

        
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class)))
                .thenThrow(new RuntimeException("columns-problem"));

        initializer.run(args);

        
        verifyNoInteractions(categoryRepository, menuItemRepository, roleRepository, userRepository, tableRepository);
    }

    @Test
    void run_seedRolesAndUsers_skips_saving_when_roles_and_users_exist() throws Exception {
        
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(inv -> {
                    Object[] a = inv.getArguments();
                    if (a.length == 3 && "menu_items".equals(String.valueOf(a[2]))) return 1;
                    return 0;
                });
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class))).thenReturn(1);

        
        when(categoryRepository.findByItemType(ItemType.KITCHEN)).thenReturn(Optional.of(new CategoryEntity()));
        when(categoryRepository.findByItemType(ItemType.BAR)).thenReturn(Optional.of(new CategoryEntity()));

        
        when(menuItemRepository.count()).thenReturn(5L);

        
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(Role.builder().name("ROLE_ADMIN").build()));
        when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(Role.builder().name("ROLE_EMPLOYEE").build()));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(Role.builder().name("ROLE_USER").build()));

        
        when(userRepository.findByUsername("admin@test.com")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("employee@test.com")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("user@test.com")).thenReturn(Optional.of(new User()));

        
        when(tableRepository.count()).thenReturn(3L);

        initializer.run(args);

        
        verify(roleRepository, never()).save(any(Role.class));
        verify(userRepository, never()).save(any(User.class));

        
        verify(menuItemRepository, never()).saveAll(anyList());

        
        verify(tableRepository, never()).saveAll(anyList());
    }
}