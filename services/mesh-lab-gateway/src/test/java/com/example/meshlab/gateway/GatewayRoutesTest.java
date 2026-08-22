package com.example.meshlab.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void contextLoadsAndRoutesAreConfigured() {
        Flux<RouteDefinition> definitions = routeDefinitionLocator.getRouteDefinitions();
        List<RouteDefinition> routes = definitions.collectList().block();

        assertEquals(2, routes.size());
        assertEquals("mesh-lab", routes.get(0).getId());
        assertEquals("mesh-lab-inventory", routes.get(1).getId());
    }
}
