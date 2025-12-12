package org.example.main.scheduler;

import org.example.main.model.menu.MenuItem;
import org.example.main.model.order.OrderEntity;
import org.example.main.model.order.OrderItem;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.recommendation.FavoriteRecommendation;
import org.example.main.model.enums.OrderStatus;
import org.example.main.repository.menu.MenuItemRepository;
import org.example.main.repository.order.OrderRepository;
import org.example.main.repository.table.RestaurantTableRepository;
import org.example.main.repository.recommendation.FavoriteRecommendationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppSchedulerTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    RestaurantTableRepository tableRepository;

    @Mock
    MenuItemRepository menuItemRepository;

    @Mock
    FavoriteRecommendationRepository favoriteRepository;

    @InjectMocks
    AppScheduler scheduler;

    private static final Path REPORT_DIR = Paths.get("src/main/resources/reports");

    @BeforeEach
    void beforeEach() throws IOException {
        
        Files.createDirectories(REPORT_DIR);
    }

    @AfterEach
    void afterEach() throws IOException {
        
        if (Files.exists(REPORT_DIR)) {
            try (var s = Files.list(REPORT_DIR)) {
                s.forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        }
    }

    @Test
    void periodicJob_noActiveOrders_logsAndReturnsWhenEmpty() {
        
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of());

        
        scheduler.periodicJob();

        verify(orderRepository).findByStatusIn(anyList());
        
        verifyNoInteractions(tableRepository);
    }

    @Test
    void periodicJob_withActiveOrders_groupsByTableAndLogs() {
        UUID tableId = UUID.randomUUID();
        OrderEntity o1 = new OrderEntity();
        o1.setId(UUID.randomUUID());
        o1.setTableId(tableId);
        o1.setStatus(OrderStatus.NEW);

        
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(o1));

        
        RestaurantTable rt = new RestaurantTable();
        rt.setId(tableId);
        rt.setCode("T42");
        when(tableRepository.findAllById(argThat((Iterable<UUID> iterable) -> {
            if (iterable == null) return false;
            for (UUID u : iterable) {
                if (tableId.equals(u)) return true;
            }
            return false;
        }))).thenReturn(List.of(rt));

        scheduler.periodicJob();

        verify(orderRepository).findByStatusIn(anyList());
        
        verify(tableRepository).findAllById(any());
    }

    @Test
    void dailyReportJob_noOrders_logsAndHandlesZeroDivision() {
        
        when(orderRepository.findWithItemsByCreatedAtBetween(any(), any())).thenReturn(List.of());

        
        scheduler.dailyReportJob();

        verify(orderRepository).findWithItemsByCreatedAtBetween(any(), any());
    }

    @Test
    void dailyReportJob_withOrders_aggregatesAndLogs_topItems() {
        
        MenuItem m1 = new MenuItem();
        m1.setId(UUID.randomUUID());
        m1.setName("A");
        m1.setPrice(BigDecimal.valueOf(2.50));

        MenuItem m2 = new MenuItem();
        m2.setId(UUID.randomUUID());
        m2.setName("B");
        
        m2.setPrice(null);

        OrderItem oi1 = new OrderItem();
        oi1.setId(UUID.randomUUID());
        oi1.setMenuItem(m1);
        oi1.setQuantity(3);

        OrderItem oi2 = new OrderItem();
        oi2.setId(UUID.randomUUID());
        oi2.setMenuItem(m2);
        oi2.setQuantity(2);

        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setItems(List.of(oi1, oi2));
        order.setCreatedAt(OffsetDateTime.now());

        
        when(orderRepository.findWithItemsByCreatedAtBetween(any(), any())).thenReturn(List.of(order));

        
        scheduler.dailyReportJob();

        verify(orderRepository).findWithItemsByCreatedAtBetween(any(), any());
    }

    @Test
    void dailyReportJob_itemTotal_handlesException_and_continues() {
        
        MenuItem mBad = new MenuItem() {
            @Override
            public BigDecimal getPrice() {
                throw new RuntimeException("boom");
            }
        };
        mBad.setId(UUID.randomUUID());
        mBad.setName("BadPrice");

        OrderItem oi = new OrderItem();
        oi.setId(UUID.randomUUID());
        oi.setMenuItem(mBad);
        oi.setQuantity(1);

        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setItems(List.of(oi));

        when(orderRepository.findWithItemsByCreatedAtBetween(any(), any())).thenReturn(List.of(order));

        
        scheduler.dailyReportJob();

        verify(orderRepository).findWithItemsByCreatedAtBetween(any(), any());
    }

    @Test
    void dailyRecommendationsReport_emptySkipsAndDoesNotCreateFile() throws Exception {
        when(favoriteRepository.findAll()).thenReturn(List.of());

        scheduler.dailyRecommendationsReport();

        
        try (var s = Files.list(REPORT_DIR)) {
            assertThat(s.findAny()).isEmpty();
        }
    }

    @Test
    void dailyRecommendationsReport_writesCsvFile_forFavorites() throws Exception {
        FavoriteRecommendation fav = new FavoriteRecommendation();
        fav.setId(UUID.randomUUID());
        fav.setMenuItemId(UUID.randomUUID());
        fav.setMenuItemName("DishX");
        fav.setDescription("Yummy");
        fav.setIngredients("a,b");
        fav.setCalories(123);
        fav.setProtein(4);
        fav.setFats(5);
        fav.setCarbs(6);
        fav.setCreatedBy(UUID.randomUUID());
        fav.setCreatedAt(OffsetDateTime.now());

        when(favoriteRepository.findAll()).thenReturn(List.of(fav));

        
        scheduler.dailyRecommendationsReport();

        
        String expectedPrefix = "daily_ai_recommendations_";
        try (var s = Files.list(REPORT_DIR)) {
            Optional<Path> anyFile = s.filter(p -> p.getFileName().toString().startsWith(expectedPrefix)).findFirst();
            assertThat(anyFile).isPresent();
            String content = Files.readString(anyFile.get());
            assertThat(content).contains("menu_item_name");
            assertThat(content).contains("DishX");
        }
    }
}