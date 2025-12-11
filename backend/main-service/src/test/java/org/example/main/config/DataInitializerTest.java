package org.example.main.config;

import org.example.main.model.category.CategoryEntity;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

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

    @InjectMocks
    DataInitializer dataInitializer;

    @BeforeEach
    void setup() {
        
        ReflectionTestUtils.setField(dataInitializer, "reinitialize", false);
    }

    @Test
    void run_skips_when_menu_items_table_missing() throws Exception {
        
        when(jdbcTemplate.queryForObject(
                contains("information_schema.tables"),
                eq(Integer.class),
                eq("menu_items")
        )).thenReturn(0);

        dataInitializer.run(mock(ApplicationArguments.class));

        
        verify(categoryRepository, never()).count();
        verify(menuItemRepository, never()).count();
        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).findByUsername(anyString());
        verify(tableRepository, never()).count();
    }

    @Test
    void run_reinitialize_deletes_and_seeds_success_with_jdbc_fallback() throws Exception {
        
        ReflectionTestUtils.setField(dataInitializer, "reinitialize", true);

        
        when(jdbcTemplate.queryForObject(
                contains("information_schema.tables"),
                eq(Integer.class),
                eq("menu_items")
        )).thenReturn(1);

        
        when(jdbcTemplate.queryForObject(
                contains("information_schema.tables"),
                eq(Integer.class),
                eq("order_items")
        )).thenReturn(1);

        
        when(jdbcTemplate.queryForObject(
                contains("information_schema.columns"),
                eq(Integer.class)
        )).thenReturn(1);

        
        doThrow(new RuntimeException("deleteAll fail")).when(orderItemRepository).deleteAll();
        when(jdbcTemplate.update(startsWith("DELETE FROM order_items"))).thenReturn(1);

        
        doNothing().when(menuItemRepository).deleteAll();

        
        when(categoryRepository.count()).thenReturn(0L);
        when(menuItemRepository.count()).thenReturn(0L);

        
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        when(userRepository.findByUsername("admin@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("employee@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user@test.com")).thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        
        when(tableRepository.count()).thenReturn(0L);
        when(tableRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        dataInitializer.run(mock(ApplicationArguments.class));

        
        verify(orderItemRepository).deleteAll();
        verify(jdbcTemplate).update(startsWith("DELETE FROM order_items"));

        
        verify(menuItemRepository).saveAll(anyList());

        
        verify(categoryRepository, atLeastOnce()).save(argThat(c -> c instanceof CategoryEntity));

        
        verify(roleRepository, atLeast(1)).save(argThat(r -> r instanceof Role));

        
        verify(userRepository, atLeast(1)).save(argThat(u -> u instanceof User));

        
        verify(tableRepository).saveAll(anyList());
    }

    @Test
    void run_handles_missing_columns_gracefully() throws Exception {
        
        when(jdbcTemplate.queryForObject(
                contains("information_schema.tables"),
                eq(Integer.class),
                eq("menu_items")
        )).thenReturn(1);

        
        when(jdbcTemplate.queryForObject(
                contains("information_schema.columns"),
                eq(Integer.class),
                anyString()
        )).thenReturn(0);

        dataInitializer.run(mock(ApplicationArguments.class));

        
        verify(menuItemRepository, never()).saveAll(anyList());
        verify(roleRepository, never()).save(any());
    }
}