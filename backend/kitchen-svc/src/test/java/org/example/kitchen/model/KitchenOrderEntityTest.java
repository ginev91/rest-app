package org.example.kitchen.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.kitchen.model.enums.KitchenOrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class KitchenOrderEntityTest {

    @Autowired
    private TestEntityManager em;

    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void persistValidEntity_generatesIdAndPersistsFields() {
        KitchenOrder order = KitchenOrder.builder()
                .orderId(UUID.randomUUID())
                .itemsJson("[{\"name\":\"Pizza\",\"qty\":2}]")
                .status(KitchenOrderStatus.NEW)
                .createdAt(Instant.now())
                .build();

        KitchenOrder saved = em.persistFlushFind(order);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(saved.getItemsJson()).isEqualTo(order.getItemsJson());
        assertThat(saved.getStatus()).isEqualTo(order.getStatus());
        assertThat(saved.getCreatedAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    void updatePersistedEntity_changesAreFlushedAndVisible() {
        KitchenOrder order = KitchenOrder.builder()
                .orderId(UUID.randomUUID())
                .itemsJson("[]")
                .status(KitchenOrderStatus.NEW)
                .createdAt(Instant.now())
                .build();

        KitchenOrder persisted = em.persistFlushFind(order);
        assertThat(persisted.getStatus()).isEqualTo(KitchenOrderStatus.NEW);

        
        em.detach(persisted);
        persisted.setStatus(KitchenOrderStatus.PREPARING);
        Instant updatedAt = Instant.now().plusSeconds(60);
        persisted.setUpdatedAt(updatedAt);

        KitchenOrder merged = em.merge(persisted);
        em.flush();

        KitchenOrder found = em.find(KitchenOrder.class, merged.getId());
        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo(KitchenOrderStatus.PREPARING);
        assertThat(found.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void persistMissingRequiredColumns_throwsException() {
        
        KitchenOrder invalid = new KitchenOrder();
        invalid.setItemsJson("[{}]");

        assertThatThrownBy(() -> em.persistAndFlush(invalid))
                .isInstanceOfAny(PersistenceException.class, DataIntegrityViolationException.class);
    }

    @Test
    void jacksonSerialization_roundTrip_preservesFields() throws Exception {
        UUID orderId = UUID.randomUUID();
        Instant created = Instant.parse("2025-01-01T12:00:00Z");

        KitchenOrder original = KitchenOrder.builder()
                .orderId(orderId)
                .itemsJson("[{\"name\":\"Salad\",\"qty\":1}]")
                .status(KitchenOrderStatus.READY)
                .createdAt(created)
                .build();

        
        String json = objectMapper.writeValueAsString(original);
        assertThat(json).isNotBlank();

        KitchenOrder deserialized = objectMapper.readValue(json, KitchenOrder.class);
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getOrderId()).isEqualTo(orderId);
        assertThat(deserialized.getItemsJson()).isEqualTo(original.getItemsJson());
        assertThat(deserialized.getStatus()).isEqualTo(original.getStatus());
        assertThat(deserialized.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }
}