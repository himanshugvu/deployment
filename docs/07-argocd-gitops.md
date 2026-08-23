# 07 - Argo CD GitOps

## Model

`deploy/argocd/application.yaml` is an **app-of-apps**: one Application whose
source is the directory `deploy/argocd/apps/`. Every YAML there is itself an
Application, so applying one manifest manages the entire platform:

```powershell
kubectl apply -f deploy/argocd/namespace.yaml
kubectl apply -f deploy/argocd/application.yaml
```

## Anatomy of a child Application

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: mesh-lab-inventory
  namespace: argocd
  annotations:
    argocd.argoproj.io/sync-wave: "5"       # ordering (doc 08)
spec:
  source:
    repoURL: https://github.com/himanshugvu/deployment.git
    targetRevision: main
    path: deploy/helm/mesh-lab-inventory
    helm:
      releaseName: mesh-lab                  # critical! see below
      parameters:
        - { name: rollout.enabled, value: "true" }
        - { name: rollout.strategy, value: canary }
  destination: { server: https://kubernetes.default.svc, namespace: mesh-lab }
  syncPolicy:
    automated: { prune: true, selfHeal: true }
    syncOptions: [CreateNamespace=true]
```

- `automated` = sync on every detected commit
- `prune` = resources removed from Git get deleted
- `selfHeal` = manual kubectl edits are reverted (Git wins)

Platform components (istio, prometheus, kiali, rollouts) use the same shape
but `chart:` + upstream `repoURL:` instead of a git path.

## Lesson 1: release names

Argo CD uses the Application's **metadata.name as the Helm release name**.
App `mesh-lab-inventory` + chart fullname template produced
`mesh-lab-inventory-mesh-lab-inventory` - breaking every URL that assumed
`mesh-lab-mesh-lab-inventory`. Fix: pin `helm.releaseName: mesh-lab` on all
service apps so DNS stays predictable. The same bug class later hit
kube-prometheus-stack (fixed with `releaseName: monitoring`).

## Lesson 2: multi-source apps

To keep Helm values inside our repo while pulling the chart from upstream:

```yaml
sources:
  - repoURL: https://prometheus-community.github.io/helm-charts
    chart: kube-prometheus-stack
    helm:
      valueFiles: [$values/deploy/observability/kube-prometheus-stack-values.yaml]
  - repoURL: https://github.com/himanshugvu/deployment.git
    targetRevision: main
    ref: values          # $values points here
```

## Watching syncs

```powershell
kubectl get applications -n argocd            # SYNC/HEALTH columns
kubectl get app <name> -n argocd -o jsonpath="{.status.conditions}"
kubectl annotate app <name> -n argocd argocd.argoproj.io/refresh=hard --overwrite
```

A transient GitHub API failure once froze one app at its cached revision;
hard refresh forced recompare.

Read next: [08 - Sync waves](08-sync-waves-and-ordering.md)
