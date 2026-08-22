# Observability

The `prometheus` Argo CD application (see `deploy/argocd/apps/prometheus.yaml`)
installs kube-prometheus-stack into the `monitoring` namespace using the values
in this folder.

## What you get

- Prometheus with a 3 day retention and 15s scrape interval
- Grafana (admin / admin by default, change for anything shared)
- Prometheus Operator CRDs, so `ServiceMonitor` resources are honored

Alertmanager is disabled to keep the lab footprint small.

## How the services are scraped

Each service chart ships an optional `ServiceMonitor` template targeting
`/actuator/prometheus` on port `http`. The Argo CD applications for the
services enable it:

```yaml
monitoring:
  serviceMonitor:
    enabled: true
    additionalLabels:
      release: monitoring
```

Prometheus is configured with `serviceMonitorSelectorNilUsesHelmValues: false`
(see `kube-prometheus-stack-values.yaml`), so all ServiceMonitors in the
cluster are discovered regardless of labels.

## Verify

```powershell
kubectl get pods -n monitoring
kubectl get servicemonitors -A

# Grafana
kubectl port-forward svc/monitoring-grafana 3000:80 -n monitoring
```

Useful Grafana dashboard IDs to import from grafana.com:

- `4701` JVM (Micrometer)
- `11378` Spring Boot Statistics
- `19004` Spring Boot APL

Kiali (installed by its own Argo CD application) also embeds mesh traffic
graphs sourced from this Prometheus.
