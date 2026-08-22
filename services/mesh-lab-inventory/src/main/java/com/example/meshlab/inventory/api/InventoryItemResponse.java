package com.example.meshlab.inventory.api;

public record InventoryItemResponse(
        int id,
        String name,
        int quantity
) {
}
