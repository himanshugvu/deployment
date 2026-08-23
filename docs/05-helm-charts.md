# 05 - Helm Charts

Three sibling charts under `deploy/helm/` share one structure:

```text
deploy/helm/mesh-lab/
  Chart.yaml            name, version 0.2.0, appVersion
  values.yaml           every knob, with safe defaults
  templates/
    _helpers.tpl        name/fullname/labels/serviceAccountName defines
    deployment.yaml     Deployment OR Rollout (see doc 06)
    service.yaml        ClusterIP service
    service-preview.yaml  extra Service, only for blueGreen (doc 12)
    serviceaccount.yaml
    servicemonitor.yaml Prometheus scrape config (doc 10)
    analysistemplate.yaml rollback metric (doc 13)
    tests/test-connection.yaml  `helm test` hook pod
```

## Helper pattern

Each chart prefixes its own defines (`mesh-lab.fullname`,
`mesh-lab-gateway.fullname`) so charts never collide:

```go
{{- define "mesh-lab.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "mesh-lab.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
```

Fullname = `<release>-<chart>`; this is why service DNS names look like
`mesh-lab-mesh-lab` (release `mesh-lab` + chart `mesh-lab`).

## Values philosophy

Every behavior is a value; Git (via Argo CD parameters) is the only thing
that changes them at runtime:

- `rollout.enabled`, `rollout.strategy` (canary|blueGreen)
- `rollout.analysis.*` thresholds and windows
- `monitoring.serviceMonitor.enabled`
- `chaos.errorRate`

## Conditional resources

Templates render only when relevant:

```go
{{- if .Values.monitoring.serviceMonitor.enabled }} ... {{- end }}
{{- if and .Values.rollout.enabled (eq .Values.rollout.strategy "blueGreen") }} preview svc {{- end }}
```

## Validate without installing

We never had Helm installed locally - everything ran through a container:

```powershell
docker run --rm -v "${PWD}:/workdir" --workdir /workdir alpine/helm:3.16.4 lint deploy/helm/...
docker run --rm -v "${PWD}:/workdir" --workdir /workdir alpine/helm:3.16.4 template t deploy/helm/mesh-lab --set rollout.enabled=true
```

`helm lint` catches schema errors; `helm template` proves each feature flag
renders the right resources before you push.

## Manual install path (no Argo CD)

```powershell
kubectl create namespace mesh-lab
helm upgrade --install mesh-lab deploy/helm/mesh-lab -n mesh-lab
```

Charts default to plain Deployments so this works on any cluster even
without Rollouts CRDs.

Read next: [06 - Workloads, probes and pods](06-workload-and-probes.md)
