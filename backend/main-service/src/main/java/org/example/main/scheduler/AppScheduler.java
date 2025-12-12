package org.example.main.scheduler;

import org.example.main.model.menu.MenuItem;
import org.example.main.model.order.OrderEntity;
import org.example.main.model.order.OrderItem;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.enums.OrderStatus;
import org.example.main.model.recommendation.FavoriteRecommendation;
import org.example.main.repository.menu.MenuItemRepository;
import org.example.main.repository.order.OrderRepository;
import org.example.main.repository.table.RestaurantTableRepository;
import org.example.main.repository.recommendation.FavoriteRecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AppScheduler {
    private static final Logger log = LoggerFactory.getLogger(AppScheduler.class);

    private final OrderRepository orderRepository;
    private final RestaurantTableRepository tableRepository;
    private final MenuItemRepository menuItemRepository;
    private final FavoriteRecommendationRepository favoriteRepository;

    public AppScheduler(OrderRepository orderRepository,
                        RestaurantTableRepository tableRepository,
                        MenuItemRepository menuItemRepository,
                        FavoriteRecommendationRepository favoriteRepository) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.menuItemRepository = menuItemRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @Scheduled(fixedRateString = "${app.scheduled.rate:300000}")
    public void periodicJob() {
        try {
            List<String> openStatusNames = List.of("NEW", "PREPARING", "IN_PROGRESS", "SERVED", "PAID", "COMPLETED", "CANCELLED");

            List<OrderStatus> openStatuses = openStatusNames.stream()
                    .map(s -> {
                        try {
                            return OrderStatus.valueOf(s.trim().toUpperCase());
                        } catch (Exception ex) {
                            log.warn("periodicJob: ignoring unknown OrderStatus '{}'", s);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (openStatuses.isEmpty()) {
                log.info("periodicJob: no valid OrderStatus values to query; skipping at {}", ZonedDateTime.now());
                return;
            }

            List<OrderEntity> openOrders = orderRepository.findByStatusIn(openStatuses);

            Map<UUID, Long> activeTableCounts = openOrders.stream()
                    .filter(o -> o.getTableId() != null)
                    .collect(Collectors.groupingBy(OrderEntity::getTableId, Collectors.counting()));

            if (activeTableCounts.isEmpty()) {
                log.info("periodicJob: no active tables at {}", ZonedDateTime.now());
            } else {
                List<UUID> tableIds = new ArrayList<>(activeTableCounts.keySet());
                List<RestaurantTable> tables = tableRepository.findAllById(tableIds);
                Map<UUID, RestaurantTable> tableById = tables.stream()
                        .collect(Collectors.toMap(RestaurantTable::getId, Function.identity()));

                log.info("periodicJob: activeTablesCount={} timestamp={}",
                        activeTableCounts.size(),
                        ZonedDateTime.now());

                activeTableCounts.forEach((tableId, cnt) -> {
                    RestaurantTable t = tableById.get(tableId);
                    String code = (t != null) ? t.getCode() : "unknown";
                    log.info("periodicJob: tableId={} code={} openOrdersCount={}", tableId, code, cnt);
                });
            }

        } catch (Exception ex) {
            log.error("periodicJob: failed to compute active tables", ex);
        }
    }


    @Scheduled(cron = "0 0 14 * * ?")
    public void dailyReportJob() {
        try {
            ZoneId zone = ZoneId.systemDefault();
            LocalDate yesterday = LocalDate.now(zone).minusDays(1);
            ZonedDateTime startOfDay = yesterday.atStartOfDay(zone);
            ZonedDateTime endOfDay = startOfDay.plusDays(1);

            OffsetDateTime from = startOfDay.toOffsetDateTime();
            OffsetDateTime to = endOfDay.toOffsetDateTime();

            List<OrderEntity> orders = orderRepository.findWithItemsByCreatedAtBetween(from, to);

            int ordersCount = orders.size();

            BigDecimal totalRevenue = orders.stream()
                    .flatMap(o -> safeStream(o.getItems()).map(this::itemTotal))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgPerOrder = (ordersCount == 0)
                    ? BigDecimal.ZERO
                    : totalRevenue.divide(BigDecimal.valueOf(ordersCount), 2, BigDecimal.ROUND_HALF_UP);

            Map<UUID, ItemAgg> agg = new HashMap<>();
            for (OrderEntity o : orders) {
                for (OrderItem oi : safeCollection(o.getItems())) {
                    if (oi == null || oi.getMenuItem() == null) continue;
                    UUID mid = oi.getMenuItem().getId();
                    ItemAgg a = agg.computeIfAbsent(mid, k -> new ItemAgg(oi.getMenuItem(), 0L, BigDecimal.ZERO));
                    a.quantity += oi.getQuantity();
                    BigDecimal price = oi.getMenuItem().getPrice() != null ? oi.getMenuItem().getPrice() : BigDecimal.ZERO;
                    a.revenue = a.revenue.add(price.multiply(BigDecimal.valueOf(Math.max(0, oi.getQuantity()))));
                }
            }

            List<ItemAgg> topItems = agg.values().stream()
                    .sorted(Comparator.comparingLong((ItemAgg ia) -> ia.quantity).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            log.info("dailyReport: date={} ordersCount={} totalRevenue={} avgPerOrder={}",
                    yesterday, ordersCount, totalRevenue, avgPerOrder);

            if (topItems.isEmpty()) {
                log.info("dailyReport: no items sold on {}", yesterday);
            } else {
                log.info("dailyReport: topItems for {}:", yesterday);
                for (ItemAgg ia : topItems) {
                    MenuItem mi = ia.menuItem;
                    String name = (mi != null) ? mi.getName() : "unknown";
                    log.info("dailyReport: itemName='{}' itemId={} quantitySold={} revenue={}",
                            name, mi != null ? mi.getId() : null, ia.quantity, ia.revenue);
                }
            }

        } catch (Exception ex) {
            log.error("dailyReportJob: failed to generate daily report", ex);
        }
    }


    @Scheduled(cron = "3 0 13 * * ?")
    public void dailyRecommendationsReport() {
        try {
            List<FavoriteRecommendation> allFavorites = favoriteRepository.findAll();

            if (allFavorites.isEmpty()) {
                log.info("dailyRecommendationsReport: no AI recommendations to report today");
                return;
            }

            StringBuilder csv = new StringBuilder();
            csv.append("id,menu_item_id,menu_item_name,description,ingredients,calories,protein,fats,carbs,created_by,created_at\n");

            for (FavoriteRecommendation fav : allFavorites) {
                csv.append(fav.getId()).append(",")
                        .append(fav.getMenuItemId()).append(",")
                        .append("\"").append(fav.getMenuItemName()).append("\",")
                        .append("\"").append(fav.getDescription() != null ? fav.getDescription() : "").append("\",")
                        .append("\"").append(fav.getIngredients() != null ? fav.getIngredients() : "").append("\",")
                        .append(fav.getCalories() != null ? fav.getCalories() : "").append(",")
                        .append(fav.getProtein() != null ? fav.getProtein() : "").append(",")
                        .append(fav.getFats() != null ? fav.getFats() : "").append(",")
                        .append(fav.getCarbs() != null ? fav.getCarbs() : "").append(",")
                        .append(fav.getCreatedBy() != null ? fav.getCreatedBy() : "").append(",")
                        .append(fav.getCreatedAt())
                        .append("\n");
            }

            String filename = String.format("src/main/resources/reports/daily_ai_recommendations_%s.csv",
                    LocalDate.now());
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("src/main/resources/reports"));
            java.nio.file.Files.writeString(java.nio.file.Paths.get(filename), csv.toString());

            log.info("dailyRecommendationsReport: file created with {} recommendations -> {}", allFavorites.size(), filename);

        } catch (Exception ex) {
            log.error("dailyRecommendationsReport: failed to generate AI recommendations report", ex);
        }
    }


    private static class FavAgg {
        final MenuItem menuItem;
        long count;

        public FavAgg(MenuItem menuItem, long count) {
            this.menuItem = menuItem;
            this.count = count;
        }
    }



    private Stream<OrderItem> safeStream(Collection<?> items) {
        if (items == null) return Stream.empty();
        return items.stream()
                .filter(Objects::nonNull)
                .map(e -> (OrderItem) e);
    }

    private Collection<OrderItem> safeCollection(Collection<?> items) {
        return items == null ? Collections.emptyList() : (Collection<OrderItem>) items;
    }

    private BigDecimal itemTotal(OrderItem oi) {
        try {
            BigDecimal price = oi.getMenuItem() != null && oi.getMenuItem().getPrice() != null
                    ? oi.getMenuItem().getPrice()
                    : BigDecimal.ZERO;
            return price.multiply(BigDecimal.valueOf(Math.max(0, oi.getQuantity())));
        } catch (Exception ex) {
            log.warn("itemTotal: failed to compute for orderItem id={}, defaulting to 0", oi != null ? oi.getId() : null);
            return BigDecimal.ZERO;
        }
    }

    private static class ItemAgg {
        final MenuItem menuItem;
        long quantity;
        BigDecimal revenue;
        ItemAgg(MenuItem menuItem, long quantity, BigDecimal revenue) {
            this.menuItem = menuItem;
            this.quantity = quantity;
            this.revenue = revenue;
        }
    }
}
