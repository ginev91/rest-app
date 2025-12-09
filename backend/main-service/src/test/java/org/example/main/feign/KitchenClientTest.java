package org.example.main.feign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class KitchenClientTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void kitchenOrderRequest_serializesWithExpectedPropertyNames() throws Exception {
        KitchenClient.KitchenOrderRequest req = new KitchenClient.KitchenOrderRequest();
        UUID oid = UUID.randomUUID();
        req.orderId = oid;
        req.itemsJson = "[{\"x\":1}]";

        String json = om.writeValueAsString(req);
        JsonNode node = om.readTree(json);

        
        assertThat(node.has("orderId")).isTrue();
        assertThat(node.has("itemsJson")).isTrue();

        assertThat(node.get("orderId").asText()).isEqualTo(oid.toString());
        assertThat(node.get("itemsJson").asText()).isEqualTo("[{\"x\":1}]");
    }

    @Test
    void kitchenOrderResponse_deserializesBothOrderIdAndSourceOrderIdFields() throws Exception {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID sourceOrderAlias = UUID.randomUUID();

        
        ObjectNode root = om.createObjectNode();
        root.put("id", id.toString());
        root.put("orderId", orderId.toString());
        root.put("sourceOrderId", sourceOrderAlias.toString());
        root.put("itemsJson", "[{\"menuItemName\":\"X\"}]");
        root.put("status", "READY");
        root.put("note", "n");
        root.put("createdAt", "2025-01-01T00:00:00Z");
        root.put("updatedAt", "2025-01-01T01:00:00Z");

        String json = om.writeValueAsString(root);

        KitchenClient.KitchenOrderResponse resp = om.readValue(json, KitchenClient.KitchenOrderResponse.class);

        
        assertThat(resp.id).isEqualTo(id);
        
        assertThat(resp.sourceOrderId).isEqualTo(orderId);
        
        assertThat(resp.sourceOrderIdAlias).isEqualTo(sourceOrderAlias);

        assertThat(resp.itemsJson).contains("menuItemName");
        assertThat(resp.status).isEqualTo("READY");
        assertThat(resp.note).isEqualTo("n");
        assertThat(resp.createdAt).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(resp.updatedAt).isEqualTo("2025-01-01T01:00:00Z");
    }

    @Test
    void kitchenOrderResponse_serializesAnnotatedPropertyNames() throws Exception {
        KitchenClient.KitchenOrderResponse resp = new KitchenClient.KitchenOrderResponse();
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID sourceAlias = UUID.randomUUID();

        resp.id = id;
        resp.sourceOrderId = orderId;
        resp.sourceOrderIdAlias = sourceAlias;
        resp.itemsJson = "[]";
        resp.status = "CREATED";

        String json = om.writeValueAsString(resp);
        JsonNode node = om.readTree(json);

        
        assertThat(node.has("id")).isTrue();
        assertThat(node.has("orderId")).isTrue();
        assertThat(node.has("sourceOrderId")).isTrue();

        assertThat(node.get("id").asText()).isEqualTo(id.toString());
        
        assertThat(node.get("orderId").asText()).isEqualTo(orderId.toString());
        
        assertThat(node.get("sourceOrderId").asText()).isEqualTo(sourceAlias.toString());
    }

    @Test
    void kitchenOrderItem_serializesAndDeserializes() throws Exception {
        KitchenClient.KitchenOrderItem it = new KitchenClient.KitchenOrderItem();
        UUID mid = UUID.randomUUID();
        it.menuItemId = mid;
        it.menuItemName = "Name";
        it.quantity = 3;

        String json = om.writeValueAsString(it);
        KitchenClient.KitchenOrderItem parsed = om.readValue(json, KitchenClient.KitchenOrderItem.class);

        assertThat(parsed.menuItemId).isEqualTo(mid);
        assertThat(parsed.menuItemName).isEqualTo("Name");
        assertThat(parsed.quantity).isEqualTo(3);
    }

    @Test
    void kitchenOrderStatusUpdate_defaultAndParamConstructors_workAndSerialize() throws Exception {
        KitchenClient.KitchenOrderStatusUpdate u1 = new KitchenClient.KitchenOrderStatusUpdate();
        KitchenClient.KitchenOrderStatusUpdate u2 = new KitchenClient.KitchenOrderStatusUpdate("READY");

        String j1 = om.writeValueAsString(u1);
        String j2 = om.writeValueAsString(u2);

        JsonNode n1 = om.readTree(j1);
        JsonNode n2 = om.readTree(j2);

        
        assertThat(n2.get("status").asText()).isEqualTo("READY");
    }
}