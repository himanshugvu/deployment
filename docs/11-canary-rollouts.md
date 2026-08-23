# 11 - Canary Rollouts with Argo Rollouts

mesh-lab and mesh-lab-inventory use `rollout.strategy: canary`. The Rollout
resource looks like a Deployment plus a `strategy`:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
spec:
  replicas: 1
  selector: ...
  strategy:
    canary:
      maxSurge: 1                 # allow an extra pod so canary always exists
      steps:
        - setWeight: 25           # ~25% of traffic to new ReplicaSet
        - pause: { duration: 30s }
        - analysis:               # Prometheus gate (doc 13)
            templates:
              - templateName: <fullname>-success-rate
            args:
              - name: rollouts-pod-template-hash
                valueFrom: { podTemplateHashValue: Latest }
        - setWeight: 60
        - pause: { duration: 30s }
        - analysis: { ... }
```

## What actually happens

1. You change the image tag in Git -> Argo CD syncs -> Rollout controller
   creates **ReplicaSet #2** (new pod-template-hash).
2. `setWeight: 25` scales canary up; without Istio traffic routing the shared
   Service load-balances across all pods, so weights are approximated by pod
   counts (with maxSurge=1 you get stable+canary running side by side).
3. Each `analysis` step creates an AnalysisRun that must succeed before the
   next step starts.
4. All steps pass -> canary becomes the new **stable**, old RS scales to zero.

## Observed timeline (real run)

```text
Progressing step0 (setWeight 25)
Paused      step1 (30s)
Progressing step2 -> AnalysisRunSuccessful
Progressing step3-4 (60%, pause)
Progressing step5 -> AnalysisRunSuccessful
Healthy     step6, old RS scaled down
```

~4 minutes end-to-end for one service with default windows.

## Why steps + gates beat a plain Deployment

A Deployment swaps pods immediately on image change. A canary exposes the new
version to a fraction of traffic and *requires evidence* (Prometheus success
rate) before proceeding. A bad build dies at step 2 instead of serving 100%
of users.

## Key fields cheat sheet

| Field | Meaning |
| --- | --- |
| `setWeight` | target % of traffic/replicas for canary |
| `pause.duration` | soak time between moves |
| `maxSurge` | extra pod budget during rollout |
| `podTemplateHashValue: Latest` | arg = new RS hash (**not** fieldRef!) |

Read next: [12 - Blue-green rollouts](12-bluegreen-rollouts.md)
