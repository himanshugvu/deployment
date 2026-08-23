# 13 - Analysis Templates & Automatic Rollback

This is the brain of the progressive delivery system: a Prometheus query
that decides whether the new version is healthy, and aborts if not.

## The rendered object

Each chart ships `templates/analysistemplate.yaml`:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: <fullname>-success-rate
spec:
  args:
    - name: rollouts-pod-template-hash
  metrics:
    - name: success-rate
      interval: 20s          # measure every 20s
      count: 5               # up to 5 measurements per gate
      successCondition: result[0] >= 0.90
      failureCondition: result[0] < 0.90
      failureLimit: 0        # ONE failed measurement = abort rollout
      provider:
        prometheus:
          address: http://monitoring-kube-prometheus-prometheus.monitoring.svc:9090
          query: |
            (
              1 - (
                sum(increase(http_server_requests_seconds_count{namespace="mesh-lab",
                    status=~"5..", rollouts_pod_template_hash="{{args.rollouts-pod-template-hash}}"}[120s]))
                /
                clamp_min(sum(increase(http_server_requests_seconds_count{namespace="mesh-lab",
                    rollouts_pod_template_hash="{{args.rollouts-pod-template-hash}}"}[120s])), 1)
              )
            ) or vector(1)
```

Note the Helm escaping trick: `"{{ "{{args.rollouts-pod-template-hash}}" }}"`
emits Argo's literal placeholder through Helm untouched.

## Three hard-won lessons baked into this query

### 1. Empty vectors are fatal
If the new pod has no scraped requests yet, `sum()` over nothing returns an
EMPTY vector (not zero). Then `result[0]` in successCondition crashes with
`reflect: slice index out of range`, five provider errors accumulate, and the
rollout aborts for the wrong reason. Fix: append `or vector(1)` so "no data"
evaluates as 100% success.

### 2. Division by zero
Sparse traffic -> denominator 0 -> NaN. `clamp_min(sum(...), 1)` floors it.

### 3. Getting the hash: podTemplateHashValue, NOT fieldRef
Our first attempt used:

```yaml
valueFrom: { fieldRef: { fieldPath: metadata.labels['rollouts-pod-template-hash'] } }
```

fieldRef reads **Rollout labels** - which never contain that hash - so every
AnalysisRun recorded `value: ""`, matched no series, and passed vacuously.
The correct mechanism (per Argo Rollouts docs) is:

```yaml
valueFrom: { podTemplateHashValue: Latest }   # Latest or Stable
```

We only caught it because a chaos drill kept passing; inspecting the run's
`spec.args` showed the empty string. Always verify args on a real AnalysisRun:

```powershell
kubectl get analysisrun <name> -n mesh-lab -o jsonpath="{.spec.args}"
```

## Threshold semantics

- `successCondition/failureCondition`: compare `result[0]` (the single
  PromQL sample) to `rollout.analysis.successRateThreshold`.
- `failureLimit: 0`: first failed measurement aborts immediately - strict,
  demonstrable, and configurable per chart values.
- Both canary step-gates and blue-green postPromotionAnalysis reuse this one
  template.

Read next: [14 - Chaos drills](14-chaos-drills.md)
