package com.example.meshlab.inventory.api;

public record ServiceStatusResponse(
        String service,
        String version,
        String message
) {
}
