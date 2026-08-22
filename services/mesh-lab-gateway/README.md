# mesh-lab-gateway

Spring Cloud Gateway (Server WebFlux) entrypoint for the mesh lab platform.

## Routes

| Path | Backend |
| --- | --- |
| `GET /api/status`, `POST /api/echo` | mesh-lab (`MESH_LAB_URL`) |
| `GET /api/inventory/**`, `POST /api/inventory/adjust` | mesh-lab-inventory (`MESH_LAB_INVENTORY_URL`) |

Defaults target `localhost:8081` and `localhost:8082` for local runs; the Helm
chart overrides both with in-cluster service DNS names.

## Observability

- `GET /actuator/gateway/routes` configured routes
- `GET /actuator/prometheus` metrics
- `GET /actuator/health/liveness`, `GET /actuator/health/readiness`

## Build and run locally

```powershell
mvn test
mvn package
docker build -t ghcr.io/himanshugvu/mesh-lab-gateway:0.0.1 .

# terminal 1
cd ../mesh-lab; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
# terminal 2
cd ../mesh-lab-inventory; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
# terminal 3
cd ../mesh-lab-gateway; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9000
```

Then call the backends through the gateway on port 9000.
