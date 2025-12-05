package org.example.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.model.KitchenOrderStatus;
import org.example.kitchen.repository.KitchenOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * KitchenOrderService — persists kitchen orders and schedules transition to READY after a randomized delay.
 *
 * Configuration (application.yml / env):
 *  kitchen.prep.min-seconds
 *  kitchen.prep.max-seconds
 *  kitchen.callback.enabled
 *  kitchen.callback.url   (use placeholders {orderId} and {kitchenOrderId})
 *  kitchen.callback.secret (optional)
 */
@Service
public class KitchenOrderService implements IKitchenOrderService {

    private static final Logger log = LoggerFactory.getLogger(KitchenOrderService.class);

    private final KitchenOrderRepository repository;

    // Scheduler
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Random delay bounds (seconds)
    private final int minPrepSeconds;
    private final int maxPrepSeconds;

    // Optional callback to main service
    private final boolean callbackEnabled;
    private final String callbackUrl;
    private final String callbackSecret; // optional secret sent as header

    private final Random random = new Random();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public KitchenOrderService(KitchenOrderRepository repository,
                               @Value("${kitchen.prep.min-seconds:5}") int minPrepSeconds,
                               @Value("${kitchen.prep.max-seconds:20}") int maxPrepSeconds,
                               @Value("${kitchen.callback.enabled:true}") boolean callbackEnabled,
                               @Value("${kitchen.callback.url:}") String callbackUrl,
                               @Value("${kitchen.callback.secret:}") String callbackSecret) {
        this.repository = repository;
        this.minPrepSeconds = Math.max(1, minPrepSeconds);
        this.maxPrepSeconds = Math.max(this.minPrepSeconds, maxPrepSeconds);
        this.callbackEnabled = callbackEnabled;
        this.callbackUrl = callbackUrl == null ? "" : callbackUrl.trim();
        this.callbackSecret = callbackSecret == null ? "" : callbackSecret.trim();
    }

    @Override
    @Transactional
    public KitchenOrder createOrder(UUID orderId, String itemsJson) {
        KitchenOrder o = KitchenOrder.builder()
                .orderId(orderId)
                .itemsJson(itemsJson)
                .status(KitchenOrderStatus.PREPARING)
                .createdAt(Instant.now())
                .build();

        KitchenOrder saved = repository.save(o);
        log.info("Kitchen order created: {} for source order {}", saved.getId(), orderId);

        scheduleCompletion(saved);
        return saved;
    }

    private void scheduleCompletion(KitchenOrder ko) {
        int delay = minPrepSeconds + random.nextInt(maxPrepSeconds - minPrepSeconds + 1);
        log.info("Scheduling kitchen order {} to become READY in {}s", ko.getId(), delay);

        scheduler.schedule(() -> {
            try {
                KitchenOrder current = repository.findById(ko.getId()).orElse(null);
                if (current == null) {
                    log.warn("Kitchen order {} not found when completing", ko.getId());
                    return;
                }

                // If already CANCELLED/COMPLETED/READY, skip updating
                if (current.getStatus() == KitchenOrderStatus.CANCELLED
                        || current.getStatus() == KitchenOrderStatus.READY
                        || current.getStatus() == KitchenOrderStatus.COMPLETED) {
                    log.info("Kitchen order {} in status {}, skipping completion", ko.getId(), current.getStatus());
                    return;
                }

                current.setStatus(KitchenOrderStatus.READY);
                current.setUpdatedAt(Instant.now());
                repository.save(current);
                log.info("Kitchen order {} marked READY", current.getId());

                if (callbackEnabled && callbackUrl != null && !callbackUrl.isBlank()) {
                    try {
                        String target = callbackUrl
                                .replace("{orderId}", current.getOrderId().toString())
                                .replace("{kitchenOrderId}", current.getId().toString());
                        HttpRequest.Builder builder = HttpRequest.newBuilder()
                                .uri(URI.create(target))
                                .timeout(java.time.Duration.ofSeconds(10))
                                .POST(HttpRequest.BodyPublishers.noBody());

                        // add optional secret header
                        if (callbackSecret != null && !callbackSecret.isBlank()) {
                            builder.header("X-Callback-Secret", callbackSecret);
                        }

                        HttpRequest req = builder.build();
                        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                        log.info("Callback to main-service returned status {} for kitchenOrder {}", resp.statusCode(), current.getId());
                    } catch (Exception cbEx) {
                        log.warn("Callback to main-service failed for kitchenOrder {}: {}", current.getId(), cbEx.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("Error while completing kitchen order {}: {}", ko.getId(), e.getMessage(), e);
            }
        }, delay, TimeUnit.SECONDS);
    }

    @Override
    @Transactional
    public KitchenOrder updateStatus(UUID id, KitchenOrderStatus status) {
        KitchenOrder o = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        o.setStatus(status);
        o.setUpdatedAt(Instant.now());
        return repository.save(o);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenOrder> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @PreDestroy
    public void shutdown() {
        try {
            scheduler.shutdownNow();
        } catch (Exception e) {
            log.warn("Error shutting down kitchen scheduler: {}", e.getMessage());
        }
    }
}