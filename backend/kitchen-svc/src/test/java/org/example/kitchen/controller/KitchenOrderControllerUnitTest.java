package org.example.kitchen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kitchen.dto.request.CreateKitchenOrderRequest;
import org.example.kitchen.dto.request.UpdateStatusRequest;
import org.example.kitchen.dto.response.KitchenOrderResponse;
import org.example.kitchen.mapper.KitchenOrderMapper;
import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.model.enums.KitchenOrderStatus;
import org.example.kitchen.service.IKitchenOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class KitchenOrderControllerUnitTest {

    @Mock
    private IKitchenOrderService service;

    @InjectMocks
    private KitchenOrderController controller;

    private MockMvc mvc;
    private ObjectMapper objectMapper;

    private UUID orderId;
    private UUID kitchenOrderId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        orderId = UUID.randomUUID();
        kitchenOrderId = UUID.randomUUID();
    }

    @Test
    void create_returnsCreatedAndLocation() throws Exception {
        CreateKitchenOrderRequest req = new CreateKitchenOrderRequest();
        req.setOrderId(orderId);
        req.setItemsJson("[{\"name\":\"pizza\",\"qty\":1}]");

        KitchenOrder saved = KitchenOrder.builder()
                .id(kitchenOrderId)
                .orderId(orderId)
                .itemsJson(req.getItemsJson())
                .status(KitchenOrderStatus.PREPARING)
                .createdAt(Instant.now())
                .build();

        KitchenOrderResponse resp = new KitchenOrderResponse();
        resp.setId(kitchenOrderId);
        resp.setOrderId(orderId);
        resp.setItemsJson(req.getItemsJson());
        resp.setStatus(KitchenOrderStatus.PREPARING);

        when(service.createOrder(orderId, req.getItemsJson())).thenReturn(saved);

        try (MockedStatic<KitchenOrderMapper> mapper = Mockito.mockStatic(KitchenOrderMapper.class)) {
            mapper.when(() -> KitchenOrderMapper.toResponse(saved)).thenReturn(resp);

            mvc.perform(post("/api/kitchen/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/kitchen/orders/" + kitchenOrderId))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(kitchenOrderId.toString()))
                    .andExpect(jsonPath("$.orderId").value(orderId.toString()));
        }
    }

    @Test
    void getByOrder_returnsList() throws Exception {
        KitchenOrder k1 = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .itemsJson("[]")
                .status(KitchenOrderStatus.PREPARING)
                .createdAt(Instant.now())
                .build();

        KitchenOrderResponse r1 = new KitchenOrderResponse();
        r1.setId(k1.getId());
        r1.setOrderId(orderId);
        r1.setItemsJson(k1.getItemsJson());
        r1.setStatus(k1.getStatus());

        when(service.findByOrderId(orderId)).thenReturn(List.of(k1));
        try (MockedStatic<KitchenOrderMapper> mapper = Mockito.mockStatic(KitchenOrderMapper.class)) {
            mapper.when(() -> KitchenOrderMapper.toResponse(k1)).thenReturn(r1);

            mvc.perform(get("/api/kitchen/orders/by-order/{orderId}", orderId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(k1.getId().toString()))
                    .andExpect(jsonPath("$[0].orderId").value(orderId.toString()));
        }
    }

    @Test
    void updateStatus_and_cancel_paths() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(KitchenOrderStatus.IN_PROGRESS);

        KitchenOrder after = KitchenOrder.builder()
                .id(id)
                .orderId(orderId)
                .status(KitchenOrderStatus.IN_PROGRESS)
                .build();

        when(service.updateStatus(id, KitchenOrderStatus.IN_PROGRESS)).thenReturn(after);

        KitchenOrderResponse resp = new KitchenOrderResponse();
        resp.setId(id);
        resp.setOrderId(orderId);
        resp.setStatus(after.getStatus());

        try (MockedStatic<KitchenOrderMapper> mapper = Mockito.mockStatic(KitchenOrderMapper.class)) {
            mapper.when(() -> KitchenOrderMapper.toResponse(after)).thenReturn(resp);

            mvc.perform(put("/api/kitchen/orders/{id}/status", id.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        
        mvc.perform(post("/api/kitchen/orders/{id}/cancel", id.toString()))
                .andExpect(status().isOk());
        Mockito.verify(service).cancelOrder(id);
    }
}
