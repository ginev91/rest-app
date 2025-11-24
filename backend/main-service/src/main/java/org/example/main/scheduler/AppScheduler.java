package org.example.main.scheduler;

import org.example.main.model.MenuItem;
import org.example.main.model.OrderEntity;
import org.example.main.model.OrderItem;
import org.example.main.model.RestaurantTable;
import org.example.main.repository.MenuItemRepository;
import org.example.main.repository.OrderRepository;
import org.example.main.repository.RestaurantTableRepository;
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

    public AppScheduler(OrderRepository orderRepository,
                        RestaurantTableRepository tableRepository,
                        MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.menuItemRepository = menuItemRepository;
    }

    // Periodic: every 5 minutes - log active tables with brief details
    @Scheduled(fixedRateString = "${app.scheduled.rate:300000}")
    public void periodicJob() {
        try {
            // Adjust statuses to match your domain values
            List<String> openStatuses = List.of("NEW", "PREPARING", "IN_PROGRESS", "SERVED");

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

    // Daily: run at 01:05 to report on previous day
    @Scheduled(cron = "0 5 1 * * ?")
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

            // compute total revenue safely: flatMap items -> map to BigDecimal total -> reduce
            BigDecimal totalRevenue = orders.stream()
                    .flatMap(o -> safeStream(o.getItems()).map(this::itemTotal)) // Stream<BigDecimal>
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgPerOrder = (ordersCount == 0)
                    ? BigDecimal.ZERO
                    : totalRevenue.divide(BigDecimal.valueOf(ordersCount), 2, BigDecimal.ROUND_HALF_UP);

            // Build per-item aggregates (quantity and revenue)
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