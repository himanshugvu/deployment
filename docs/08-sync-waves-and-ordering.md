# 08 - Sync Waves and Ordering

App-of-apps creates 9 Applications at once. Without ordering, Argo CD would
try to sync services before their CRDs exist. Waves fix this - each child
Application carries:

```yaml
metadata:
  annotations:
    argocd.argoproj.io/sync-wave: "3"
```

Argo CD syncs wave N only after wave N-1 is healthy.

## Our dependency graph

| Wave | Apps | Why here |
| --- | --- | --- |
| 1 | istio-base, kube-prometheus-stack, argo-rollouts | CRDs + controllers first |
| 2 | istiod | needs base CRDs |
| 3 | kiali | needs istiod + prometheus URLs |
| 4 | mesh-config (deploy/istio/config) | needs Gateway/VirtualService CRDs |
| 5-6 | mesh-lab, inventory, gateway | need Rollouts CRDs; gateway last |

## What "healthy enough" means

Waves gate on **health**, not just sync status. A resource that reports
Healthy while actually broken lets later waves proceed (we saw services sync
while istiod was still failing on a bad chart version). Treat waves as
ordering, not verification.

## Self-healing interplay

`selfHeal: true` reverts any drift back to Git - including your own
operational tweaks. During testing we tried `kubectl patch rollout ...`
to restart a pod; the annotation vanished within seconds because it wasn't
in Git. The GitOps-clean ways to act:

```powershell
# restart a workload without changing the template
kubectl delete pod <pod> -n mesh-lab        # controller recreates it

# change behavior -> change Git -> push
```

## Automated retries make waves resilient

A failed app inside a wave does not block the pipeline forever: automated
sync keeps retrying, so once you push a fix, everything converges without
manual intervention. We relied on this repeatedly during version fixes.

Read next: [09 - Istio service mesh](09-istio-mesh.md)
