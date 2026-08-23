# 12 - Blue-Green Rollouts

The gateway uses `rollout.strategy: blueGreen` - instant full cutover with a
safety net, ideal for the edge service where partial canary traffic split is
less meaningful.

## Strategy block (rendered by the chart)

```yaml
strategy:
  blueGreen:
    activeService: mesh-lab-mesh-lab-gateway           # what users hit
    previewService: mesh-lab-mesh-lab-gateway-preview  # new version staging
    autoPromotionEnabled: true                         # no manual gate
    scaleDownDelaySeconds: 30
    postPromotionAnalysis:                             # rollback watchdog
      templates:
        - templateName: <fullname>-success-rate
      args:
        - name: rollouts-pod-template-hash
          valueFrom: { podTemplateHashValue: Latest }
```

The chart renders the extra preview Service only when strategy is blueGreen
(`templates/service-preview.yaml`), selecting the same pods as the active
service.

## Lifecycle (from real events)

```text
Scaled up      ReplicaSet <new> from 0 to 1            # new version starts
Switched       selector for ...-preview -> <new-hash>  # preview serves it
RolloutCompleted update to revision N: Initial deploy
Switched       selector for <active> -> <new-hash>     # USERS MOVED
Scaled down    ReplicaSet <old> from 1 to 0            # after delay+analysis
Healthy
```

## Why post-promotion analysis matters

Blue-green switches **all** traffic at once, so "test before switch" is
limited. The safety net instead watches live traffic *after* the switch:
if the success rate of the new ReplicaSet breaches the threshold, Argo
Rollouts aborts and flips the active service back to the old ReplicaSet
within `scaleDownDelaySeconds`.

## Canary vs Blue-Green decision guide

| | Canary | Blue-Green |
| --- | --- | --- |
| Blast radius during rollout | small (weight%) | all-at-once |
| Rollback speed | automatic at gate | automatic post-switch |
| Resource cost | +1 pod | 2x during rollout |
| Best for | backends with steady traffic | edge/entry services |

## Try it manually

```powershell
helm upgrade --install gw deploy/helm/mesh-lab-gateway -n mesh-lab `
  --set rollout.enabled=true --set rollout.strategy=blueGreen
kubectl argo rollouts get rollout gw-mesh-lab-gateway -n mesh-lab --watch
```

Read next: [13 - The analysis & rollback engine](13-analysis-and-rollback.md)
