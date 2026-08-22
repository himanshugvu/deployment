package com.example.meshlab.inventory.api;

public record AdjustStockResponse(
        String service,
        int itemId,
        int quantity
) {
}
