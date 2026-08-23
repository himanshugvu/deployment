# 09 - Istio Service Mesh

Installed via Argo CD apps: `base` + `istiod` 1.30.3 into `istio-system`,
Kiali 2.30.0 into `kiali`. Mesh behavior for our namespace lives in
`deploy/istio/config/` (synced by the `mesh-config` Application, wave 4).

## Sidecar injection

```yaml
# deploy/argocd/namespace.yaml
metadata:
  name: mesh-lab
  labels:
    istio-injection: enabled     # webhook injects envoy into new pods
```

Every service pod ends up `2/2` containers (app + istio-proxy). Pods created
*before* istiod exists get no sidecar - a classic ordering trap we hit and
fixed by deleting the stale pod.

## mTLS: STRICT vs PERMISSIVE

```yaml
apiVersion: security.istio.io/v1
kind: PeerAuthentication
spec:
  mtls:
    mode: PERMISSIVE   # accepts plaintext AND mTLS
```

- DestinationRules still force **ISTIO_MUTUAL** for service-to-service calls,
  so mesh traffic is encrypted.
- PERMISSIVE exists because Prometheus (outside the mesh) scrapes plaintext.
  Flip to STRICT once Prometheus itself carries a sidecar.

## Traffic entry

`gateway.yaml`: an Istio `Gateway` on host `mesh-lab.local` plus a
`VirtualService` routing everything to the Spring Cloud Gateway service:

```text
client -> istio-ingressgateway -> mesh-lab-gateway Service
```

## Resilience per destination

`destinationrules.yaml` (one per service):

```yaml
trafficPolicy:
  tls: { mode: ISTIO_MUTUAL }
  outlierDetection:            # circuit-breaker-ish ejection
    consecutive5xxErrors: 3
    interval: 10s
    baseEjectionTime: 30s
```

## Zero-trust between services

`authorizationpolicy.yaml`: only the gateway's ServiceAccount may call the
inventory API - anything else gets 403 from the *sidecar*, even inside the
cluster. Metrics are exempted by path so Prometheus can scrape:

```yaml
rules:
  - from: [{ source: { principals: ["...mesh-lab-mesh-lab-gateway"] } }]
    to: [{ operation: { methods: ["GET","POST"] } }]
  - to: [{ operation: { methods: ["GET"], paths: ["/actuator/prometheus"] } }]
```

## Telemetry

`telemetry.yaml` enables Envoy access logging namespace-wide; logs stream
from each pod's `istio-proxy` container.

## Gotcha: Argo CD diff noise

Istiod rewrites the webhook `caBundle` at runtime, so git never matches.
Standard fix in both Istio apps:

```yaml
ignoreDifferences:
  - group: admissionregistration.k8s.io
    kind: ValidatingWebhookConfiguration
    jqPathExpressions: [.webhooks[].clientConfig.caBundle, .webhooks[].failurePolicy]
```

## Verify

```powershell
kubectl get pods -n mesh-lab                       # all 2/2?
kubectl exec <pod> -c istio-proxy -- pilot-agent request GET config_dump > $null
kubectl logs <pod> -c istio-proxy                  # access logs
kubectl port-forward svc/kiali 20001:20001 -n kiali
```

Read next: [10 - Prometheus](10-prometheus-telemetry.md)
