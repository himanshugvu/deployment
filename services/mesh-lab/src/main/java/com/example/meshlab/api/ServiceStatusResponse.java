package com.example.meshlab.api;

public record ServiceStatusResponse(
        String service,
        String version,
        String message
) {
}
