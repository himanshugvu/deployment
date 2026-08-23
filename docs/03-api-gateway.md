# 03 - The API Gateway (Spring Cloud Gateway)

`services/mesh-lab-gateway` is the single public entrypoint for all `/api/*`
traffic. It uses the reactive Spring Cloud Gateway ("Server WebFlux" flavor).

## Version pairing lesson

Spring Cloud and Spring Boot release in lockstep. We pinned:

```xml
<parent>spring-boot-starter-parent 4.1.0</parent>
<dependencyManagement>
  <artifactId>spring-cloud-dependencies</artifactId>
  <version>2025.1.3</version>   <!-- BOM -->
</dependencyManagement>
<dependency>
  <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
</dependency>
```

Gotcha: the old artifact `spring-cloud-starter-gateway` is deprecated; with
the new starter, route properties MUST live under
`spring.cloud.gateway.server.webflux.routes` (not `spring.cloud.gateway.routes`
- routes silently don't register otherwise; we verified this against docs
before building).

## Route table

```yaml
routes:
  - id: mesh-lab
    uri: ${MESH_LAB_URL:http://localhost:8081}
    predicates:
      - Path=/api/status,/api/echo
  - id: mesh-lab-inventory
    uri: ${MESH_LAB_INVENTORY_URL:http://localhost:8082}
    predicates:
      - Path=/api/inventory/**
```

Design choice: pass-through by path namespace (no StripPrefix games). In
Kubernetes the Helm chart injects env vars pointing at cluster DNS:

```
http://mesh-lab-mesh-lab.mesh-lab.svc.cluster.local:80
http://mesh-lab-mesh-lab-inventory.mesh-lab.svc.cluster.local:80
```

## Timeouts

```yaml
spring.cloud.gateway.server.webflux.httpclient:
  connect-timeout: 2000     # fail fast when a backend is gone
  response-timeout: 5s      # surface as 503/504 instead of hanging clients
```

## Observability endpoints

- `/actuator/gateway/routes` - what actually registered (read-only access)
- `/actuator/prometheus` - metrics incl. proxied failures as status=503
  (this is how rollback analysis "sees" backend outages at the gateway)

## Test that matters

`GatewayRoutesTest` boots the context and asserts both route definitions
loaded - catching property-namespace regressions instantly.

Read next: [04 - Docker packaging](04-docker-images.md)
