# Mesh Lab Workspace

This repository is organized as a platform workspace so independent services,
an API gateway, Helm charts, Istio mesh config, observability, and Argo CD
manifests can live together.

## Layout

```text
services/
  mesh-lab             Spring Boot service (status + echo API)
  mesh-lab-inventory   Spring Boot service (stock API), second mesh hop
  mesh-lab-gateway     Spring Cloud Gateway routing /api/* to both services
deploy/
  helm/                One chart per service
  istio/config/        Gateway, VirtualService, DestinationRules, mTLS, telemetry
  observability/       kube-prometheus-stack values + docs
  argocd/              Root application (app-of-apps) + per-component apps
sources/               Mirrored reference material, not committed
```

## Build and test

Each app is independent and should be built from its own folder:

```powershell
cd services/mesh-lab           # repeat for mesh-lab-inventory, mesh-lab-gateway
mvn test
mvn package
docker build -t ghcr.io/himanshugvu/mesh-lab:0.0.1 .
```

Images to publish for a full stack: `mesh-lab`, `mesh-lab-inventory`,
`mesh-lab-gateway` (all tagged `0.0.1`).

### Run everything locally

```powershell
# terminal 1
cd services/mesh-lab; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
# terminal 2
cd services/mesh-lab-inventory; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
# terminal 3
cd services/mesh-lab-gateway; mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9000

curl http://localhost:9000/api/status
curl http://localhost:9000/api/inventory/items
```

Gateway routes: `/api/status`, `/api/echo` -> mesh-lab; `/api/inventory/**`
-> mesh-lab-inventory.

## Kubernetes deploy (plain Helm)

Charts render plain Deployments by default (no Rollouts CRDs needed):

```powershell
kubectl create namespace mesh-lab
helm upgrade --install mesh-lab deploy/helm/mesh-lab -n mesh-lab
helm upgrade --install mesh-lab-mesh-lab-inventory deploy/helm/mesh-lab-inventory -n mesh-lab
helm upgrade --install mesh-lab-gateway deploy/helm/mesh-lab-gateway -n mesh-lab
kubectl get pods -n mesh-lab
```

To scrape metrics, first install the Prometheus Operator CRDs (or the whole
kube-prometheus-stack) and pass `--set monitoring.serviceMonitor.enabled=true`.

## GitOps deploy (Argo CD)

1. Install Argo CD in your cluster.
2. Push the three images to GitHub Container Registry.
3. Apply the namespace and the root application:

```powershell
kubectl apply -f deploy/argocd/namespace.yaml
kubectl apply -f deploy/argocd/application.yaml
```

`deploy/argocd/application.yaml` is an app-of-apps that syncs everything under
`deploy/argocd/apps` in dependency order via sync waves:

| Wave | Component | Source |
| ---- | --------- | ------ |
| 1 | istio-base 1.30.3, kube-prometheus-stack 88.5.2, argo-rollouts 2.41.1 | upstream charts |
| 2 | istiod 1.30.3 | upstream chart |
| 3 | kiali-server 2.30.0 | upstream chart |
| 4 | mesh-config (Gateway/VirtualService/DestinationRules/mTLS/telemetry) | `deploy/istio/config` |
| 5-6 | mesh-lab, mesh-lab-inventory, mesh-lab-gateway | local Helm charts |

The service applications enable Argo Rollouts canaries and ServiceMonitors,
so the platform layers must exist before them (the waves handle this;
automated sync retries make convergence self-healing).

## Progressive delivery

With the Argo Rollouts controller installed, flip any service chart to a
managed rollout strategy:

```powershell
helm upgrade --install mesh-lab deploy/helm/mesh-lab -n mesh-lab --set rollout.enabled=true --set rollout.strategy=canary
```

- `rollout.strategy: canary` (mesh-lab, mesh-lab-inventory): steps run
  25% -> pause 30s -> **analysis gate** -> 60% -> pause 30s -> gate.
- `rollout.strategy: blueGreen` (gateway default): preview service warms up,
  traffic switches automatically, then post-promotion analysis watches live
  traffic and rolls back on breach.

### Automatic rollback on failures

Every chart renders an `AnalysisTemplate` that queries Prometheus every 20s
(5 measurements) and computes the success rate of exactly the new ReplicaSet:

```promql
1 - (
  sum(increase(http_server_requests_seconds_count{namespace="...", status=~"5..", rollouts_pod_template_hash="<hash>"}[120s]))
  / clamp_min(sum(increase(http_server_requests_seconds_count{..., rollouts_pod_template_hash="<hash>"}[120s])), 1)
)
```

If success drops below `rollout.analysis.successRateThreshold` (default
**0.90**), the measurement fails and Argo Rollouts aborts the update,
scaling the bad ReplicaSet back to zero automatically.

The ServiceMonitor relabels each pod's `rollouts-pod-template-hash` into a
Prometheus-safe label so the query isolates the new ReplicaSet. Verified in
the E2E lab: a canary with `chaos.errorRate=50` measured a 61% success rate
and was rolled back without touching stable traffic.

To drill rollbacks yourself, inject errors via an Argo CD parameter (or
`--set chaos.errorRate=50`) and generate traffic against the gateway.

Watch progress: `kubectl argo rollouts get rollout mesh-lab-mesh-lab-inventory
-n mesh-lab`.

## Observability and mesh

- Grafana: `kubectl port-forward svc/monitoring-grafana 3000:80 -n monitoring`
- Kiali: `kubectl port-forward svc/kiali 20001:20001 -n kiali`
- Edge entrypoint: host `mesh-lab.local` via the Istio ingress gateway, see
  `deploy/istio/README.md`

Details: `deploy/observability/README.md`, `deploy/istio/README.md`.

## Planned expansion

Done in this iteration: second service, API gateway, Istio + Kiali,
Prometheus + telemetry, Argo Rollouts progressive delivery.

Possible next steps:

- tracing backend (Jaeger/Tempo) wired into the Telemetry resource
- CI workflow to build and push all three images
- NetworkPolicies alongside the AuthorizationPolicy
