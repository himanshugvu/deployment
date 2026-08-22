package com.example.meshlab.inventory.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryControllerTest {

    private final InventoryController controller = new InventoryController("mesh-lab-inventory", "test");

    @Test
    void statusEndpointReturnsServiceInfo() {
        ServiceStatusResponse response = controller.status();

        assertEquals("mesh-lab-inventory", response.service());
        assertEquals("test", response.version());
    }

    @Test
    void itemsEndpointReturnsStock() {
        List<InventoryItemResponse> items = controller.items();

        assertEquals(3, items.size());
        assertEquals(1, items.get(0).id());
        assertEquals("item-1", items.get(0).name());
    }

    @Test
    void adjustEndpointAppliesDelta() {
        AdjustStockResponse response = controller.adjust(new AdjustStockRequest(2, -5));

        assertEquals("mesh-lab-inventory", response.service());
        assertEquals(35, response.quantity());
    }

    @Test
    void adjustEndpointRejectsUnknownItem() {
        assertThrows(UnknownItemException.class,
                () -> controller.adjust(new AdjustStockRequest(99, 1)));
    }
}
