package com.example.meshlab.inventory.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {

    private final Map<Integer, AtomicInteger> stock = new ConcurrentHashMap<>(Map.of(
            1, new AtomicInteger(25),
            2, new AtomicInteger(40),
            3, new AtomicInteger(12)
    ));

    private final String appName;
    private final String appVersion;
    private final int chaosErrorRate;

    public InventoryController(
            @Value("${app.name}") String appName,
            @Value("${app.version}") String appVersion,
            @Value("${chaos.error-rate:0}") int chaosErrorRate
    ) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.chaosErrorRate = chaosErrorRate;
    }

    @GetMapping("/status")
    public ServiceStatusResponse status() {
        maybeInjectFailure();
        return new ServiceStatusResponse(
                appName,
                appVersion,
                "inventory service ready behind the gateway"
        );
    }

    @GetMapping("/items")
    public List<InventoryItemResponse> items() {
        maybeInjectFailure();
        return stock.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    @PostMapping("/adjust")
    @ResponseStatus(HttpStatus.CREATED)
    public AdjustStockResponse adjust(@RequestBody @Valid AdjustStockRequest request) {
        maybeInjectFailure();
        AtomicInteger quantity = stock.get(request.itemId());
        if (quantity == null) {
            throw new UnknownItemException(request.itemId());
        }
        return new AdjustStockResponse(
                appName,
                request.itemId(),
                quantity.addAndGet(request.delta())
        );
    }

    void maybeInjectFailure() {
        if (chaosErrorRate > 0 && ThreadLocalRandom.current().nextInt(100) < chaosErrorRate) {
            throw new ChaosException();
        }
    }

    private static InventoryItemResponse toItem(int id, AtomicInteger quantity) {
        return new InventoryItemResponse(id, "item-" + id, quantity.get());
    }
}
