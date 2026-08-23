# 04 - Docker Packaging

Every service ships the same 16-line multi-stage `Dockerfile`:

```dockerfile
FROM maven:3.9.11-amazoncorretto-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM amazoncorretto:25-alpine
WORKDIR /app
COPY --from=build /workspace/target/<name>-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## Why each choice

| Choice | Reason |
| --- | --- |
| Multi-stage | JDK+Maven (300MB+) never reaches the final image |
| `COPY pom.xml` before `COPY src` | Docker layer cache: dependency download layer is reused when only code changes |
| JRE-only runtime (`25-alpine`) | ~180MB final image, fast pulls |
| `.dockerignore` excludes `target/` | host build output can't pollute container builds |

## Build & publish

```powershell
cd services/mesh-lab
mvn test && mvn package
docker build -t ghcr.io/himanshugvu/mesh-lab:0.0.2 .
docker push ghcr.io/himanshugvu/mesh-lab:0.0.2   # needs GHCR login
```

## Local-cluster trick used in this lab

Real clusters pull from GHCR; our throwaway k3d cluster imports images so no
registry credentials are needed:

```powershell
k3d image import ghcr.io/himanshugvu/mesh-lab:0.0.2 -c mesh-lab-e2e
```

Because charts set `imagePullPolicy: IfNotPresent`, the kubelet finds the
imported tag locally and never contacts GHCR.

## Tag discipline

- Chart `values.yaml` pins an immutable tag per release (`"0.0.2"`), never
  `latest`.
- Bumping the tag in Git is what triggers a rollout - Argo CD sees the diff,
  and a new pod template hash means a new ReplicaSet to roll out.
- Chart.yaml carries matching `version` (chart) and `appVersion` (image).

Read next: [05 - Helm charts](05-helm-charts.md)
