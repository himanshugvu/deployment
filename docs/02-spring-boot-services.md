# 02 - The Spring Boot Services

`services/mesh-lab` and `services/mesh-lab-inventory` follow identical
conventions, so learning one gives you both.

## Anatomy

```text
src/main/java/com/example/meshlab/
  MeshLabApplication.java     @SpringBootApplication entrypoint
  api/AppController.java      REST endpoints
  api/*Request|Response.java  Java records as DTOs
src/main/resources/application.yaml
src/test/java/...             Plain unit tests (no Spring context)
pom.xml                       spring-boot-starter-parent 4.1.0
```

## Endpoints

mesh-lab:

- `GET /api/status` -> service name/version/message
- `POST /api/echo` -> echoes a validated message

inventory:

- `GET /api/inventory/status`
- `GET /api/inventory/items`
- `POST /api/inventory/adjust` body `{"itemId":1,"delta":5}` (404 on unknown id)

## Conventions worth copying

1. **Records for DTOs** - immutable, zero ceremony.
2. **Constructor-injected config**:
   ```java
   public AppController(@Value("${app.name}") String appName, ...) 
   ```
   Values come from `application.yaml`, overridable by env vars in k8s
   (`APP_NAME`, `APP_VERSION` set by the Helm chart).
3. **Bean Validation** - `@Valid @RequestBody` plus constraints on records
   (`AdjustStockRequest`) give 400s for free; custom errors use
   `@ResponseStatus(HttpStatus.NOT_FOUND)`.
4. **Actuator + Prometheus** (same block in every app):
   ```yaml
   management.endpoints.web.exposure.include: health,info,prometheus
   management.endpoint.health.probes.enabled: true   # liveness/readiness
   ```
5. **Plain unit tests**: instantiate the controller directly, no Spring test
   context - fast and deterministic.

## The chaos hook (failure injection)

Both controllers accept an error rate and throw HTTP 500 randomly:

```java
void maybeInjectFailure() {
    if (chaosErrorRate > 0 && ThreadLocalRandom.current().nextInt(100) < chaosErrorRate)
        throw new ChaosException();   // @ResponseStatus(INTERNAL_SERVER_ERROR)
}
```

Wired via `chaos.error-rate: ${CHAOS_ERROR_RATE:0}` in application.yaml.
Default is off. It exists so you can prove the rollback pipeline reacts to
real failures (see doc 14).

## Local run

```powershell
cd services/mesh-lab; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
cd services/mesh-lab-inventory; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
cd services/mesh-lab-gateway; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9000
curl http://localhost:9000/api/inventory/items
```

Read next: [03 - API gateway](03-api-gateway.md)
