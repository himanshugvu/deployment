package com.example.meshlab.inventory.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(
        @NotNull
        @Min(1)
        @Max(3)
        Integer itemId,

        @Min(-100)
        @Max(100)
        int delta
) {
}
