package com.example.meshlab.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppControllerTest {

    private final AppController controller = new AppController("mesh-lab", "test");

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
}
