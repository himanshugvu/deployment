# mesh-lab-inventory

Inventory backend used as the second hop in the gateway and service mesh demos.

## Endpoints

- `GET /api/inventory/status` service info
- `GET /api/inventory/items` list items and quantities
- `POST /api/inventory/adjust` body `{"itemId": 1, "delta": 5}` adjusts stock
- `GET /actuator/prometheus` metrics

## Build

```powershell
mvn test
mvn package
docker build -t ghcr.io/himanshugvu/mesh-lab-inventory:0.0.1 .
```
