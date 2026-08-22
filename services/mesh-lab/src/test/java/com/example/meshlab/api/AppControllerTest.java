package com.example.meshlab.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppControllerTest {

    private final AppController controller = new AppController("mesh-lab", "test", 0);
    private final AppController alwaysFailing = new AppController("mesh-lab", "test", 100);

    @Test
    void statusEndpointReturnsServiceInfo() {
        ServiceStatusResponse response = controller.status();

        assertEquals("mesh-lab", response.service());
        assertEquals("test", response.version());
    }

    @Test
    void echoEndpointReturnsPayload() {
        EchoResponse response = controller.echo(new EchoRequest("hello"));

        assertEquals("mesh-lab", response.service());
        assertEquals("hello", response.echo());
    }

    @Test
    void chaosDisabledByDefault() {
        assertDoesNotThrow(() -> controller.status());
    }

    @Test
    void chaosRateHundredAlwaysFails() {
        assertThrows(ChaosException.class, () -> alwaysFailing.status());
        assertThrows(ChaosException.class, () -> alwaysFailing.echo(new EchoRequest("hi")));
    }
}
