package org.example.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.model.KitchenOrderStatus;
import org.example.kitchen.repository.KitchenOrderRepository;
import org.example.kitchen.service.KitchenOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenOrderService implements IKitchenOrderService {

    private final KitchenOrderRepository repository;

    @Override
    @Transactional
    public KitchenOrder createOrder(UUID orderId, String itemsJson) {
        KitchenOrder o = KitchenOrder.builder()
                .orderId(orderId)
                .itemsJson(itemsJson)
                .status(KitchenOrderStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        return repository.save(o);
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
}