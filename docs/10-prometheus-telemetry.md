# 10 - Prometheus & Telemetry

kube-prometheus-stack 88.5.2 (Prometheus Operator + Grafana) installs into
namespace `monitoring` via the `prometheus` Application, configured by
`deploy/observability/kube-prometheus-stack-values.yaml`:

```yaml
alertmanager: { enabled: false }        # keep the lab light
prometheus:
  prometheusSpec:
    retention: 3d
    scrapeInterval: 15s
    serviceMonitorSelectorNilUsesHelmValues: false   # <- key line
    podMonitorSelectorNilUsesHelmValues: false
```

That "NilUsesHelmValues" flag is what makes Prometheus discover **all**
ServiceMonitors in the cluster instead of only ones labeled for its Helm
release.

## ServiceMonitor per service

Each chart ships one (`templates/servicemonitor.yaml`), enabled by Argo CD
parameters:

```yaml
endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 15s
    relabelings:
      - sourceLabels: [__meta_kubernetes_pod_label_rollouts_pod_template_hash]
        targetLabel: rollouts_pod_template_hash
```

## Why that relabeling exists (two lessons)

1. Kubernetes labels allow dashes; Prometheus label names do NOT. Copying
   `rollouts-pod-template-hash` verbatim would be rejected. The relabeling
   rule renames it to `rollouts_pod_template_hash`.
2. With that label on every sample, rollback analysis can compute metrics
   for *exactly one ReplicaSet* - the canary - instead of averaging stable
   and new pods together.

Spring's `/actuator/prometheus` then gives us per-pod counters like:

```text
http_server_requests_seconds_count{status="500", uri="/api/inventory/items", ...}
```

## Queries used in this project

Success rate for a specific ReplicaSet over a 2-minute window:

```promql
1 - (
  sum(increase(http_server_requests_seconds_count{namespace="mesh-lab",
      status=~"5..", rollouts_pod_template_hash="abc123"}[120s]))
  / clamp_min(sum(increase(http_server_requests_seconds_count{namespace="mesh-lab",
      rollouts_pod_template_hash="abc123"}[120s])), 1)
)
```

- `increase()[window]` = how many requests accumulated in the window
- `clamp_min(...,1)` avoids division by zero when traffic is sparse
- status regex `5..` catches all server errors

## Verify scraping

```powershell
kubectl get servicemonitors -A
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090
# browser: http://localhost:9090/targets  -> mesh-lab targets should be UP
```

Grafana (admin/admin): `kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80`
- import dashboard IDs 4701 (JVM/Micrometer), 11378 (Spring Boot).

Read next: [11 - Canary rollouts](11-canary-rollouts.md)
