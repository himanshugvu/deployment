# 06 - Workloads, Probes and Pod Spec

`templates/deployment.yaml` is the heart of each chart: it renders either a
Kubernetes `Deployment` or an Argo Rollouts `Rollout` from the same pod spec.

## The switch

```go
{{- if .Values.rollout.enabled }}
apiVersion: argoproj.io/v1alpha1
kind: Rollout
{{- else }}
apiVersion: apps/v1
kind: Deployment
{{- end }}
```

Everything below (`selector`, `template`) is shared, so switching delivery
strategies never forks the container config.

## Probes that match Spring Boot 4

```yaml
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: http }
  initialDelaySeconds: 15
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: http }
  initialDelaySeconds: 30
```

They work because every app sets
`management.endpoint.health.probes.enabled: true`. Readiness gates traffic
during rollouts; liveness restarts a wedged JVM.

## Resources

```yaml
resources:
  requests: { cpu: 100m, memory: 256Mi }   # scheduling + Prometheus QoS
  limits:   { cpu: 500m, memory: 512Mi }
```

## ServiceAccount per service

Charts create `<fullname>` ServiceAccounts (not `default`). This matters for
Istio AuthorizationPolicy, which identifies callers by SA principal:

```
cluster.local/ns/mesh-lab/sa/mesh-lab-mesh-lab-gateway
```

## Env injection

```yaml
env:
  - name: APP_NAME          ... values.app.name
  - name: CHAOS_ERROR_RATE  ... values.chaos.errorRate (drill knob)
  {{- range .Values.extraEnv }}  # generic passthrough
```

Spring relaxed binding maps `CHAOS_ERROR_RATE` -> `chaos.error-rate`
property; application.yaml bridges it with `${CHAOS_ERROR_RATE:0}`.

## Pod labels = analysis keys

```yaml
labels:
  app.kubernetes.io/name: <chart>      # Service selector + ServiceMonitor
  app.kubernetes.io/instance: <release>
  app.kubernetes.io/version: "v1"
```

Argo Rollouts additionally stamps `rollouts-pod-template-hash` on every pod;
doc 10 shows how we surface that to Prometheus.

Read next: [07 - Argo CD GitOps](07-argocd-gitops.md)
