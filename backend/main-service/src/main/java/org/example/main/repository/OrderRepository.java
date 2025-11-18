package org.example.main.repository;

import lombok.AllArgsConstructor;
import org.example.main.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {}