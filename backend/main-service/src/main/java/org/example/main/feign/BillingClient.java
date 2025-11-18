package org.example.main.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@FeignClient(name = "billing-svc", url = "${billing.service.url:http://localhost:8082}")
public interface BillingClient {
    @PostMapping("/api/billing/bills")
    UUID createBill(@RequestBody Object billRequest);

    @PutMapping("/api/billing/bills/{id}/split")
    void splitBill(@PathVariable UUID id, @RequestParam int parts);

    @GetMapping("/api/billing/bills/{id}")
    Object getBill(@PathVariable UUID id);
}