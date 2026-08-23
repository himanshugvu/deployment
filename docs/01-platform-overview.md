# 01 - Platform Overview

## What this workspace is

A GitOps platform lab: three Java microservices, Helm charts, an Istio
service mesh, Prometheus monitoring, Argo CD continuous delivery, and
progressive delivery (canary + blue-green) with automatic rollback.

## Repository map

```text
services/
  mesh-lab             Core Spring Boot API (status + echo)
  mesh-lab-inventory   Second Spring Boot API (stock), second mesh hop
  mesh-lab-gateway     Spring Cloud Gateway routing /api/* to both
deploy/
  helm/                One chart per service (Deployment OR Rollout)
  argocd/              Root app-of-apps + one Application per component
  istio/config/        Gateway, VirtualService, DestinationRules, mTLS
  observability/       kube-prometheus-stack values
docs/                  This learning series
```

## Traffic path (production mode)

```text
client
  -> Istio ingress gateway   (host mesh-lab.local)      deploy/istio/config/gateway.yaml
  -> mesh-lab-gateway        (/api routing table)        services/mesh-lab-gateway
       -> /api/status,/api/echo          -> mesh-lab
       -> /api/inventory/**              -> mesh-lab-inventory
(each hop has an Envoy sidecar; mTLS between hops; Prometheus scrapes all pods)
```

## The GitOps contract

`main` branch = desired state. Nothing is deployed by hand:

1. You push a commit.
2. Argo CD notices (<=3 min polling) and syncs Applications in dependency
   order (sync waves).
3. Services roll out through canary/blue-green gates that query Prometheus.
4. If the new version's success rate drops below 90%, it rolls back alone.

Everything in `deploy/argocd/apps/*.yaml` points at this same repo, so the
cluster always converges to whatever GitHub says.

## Component versions used

| Layer | Version |
| --- | --- |
| Java | 25 (Zulu/Corretto) |
| Spring Boot | 4.1.0 |
| Spring Cloud | 2025.1.3 |
| Istio | 1.30.3 (base + istiod) |
| Kiali | 2.30.0 |
| kube-prometheus-stack | 88.5.2 |
| Argo Rollouts | 2.41.1 |

Read next: [02 - Spring Boot services](02-spring-boot-services.md)
