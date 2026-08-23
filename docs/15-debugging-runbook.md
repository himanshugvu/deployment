# 15 - Debugging Runbook (Everything That Broke, and How)

Every failure below actually happened in this lab. Commands are the ones that
found the answer.

## Triage order

```powershell
kubectl get applications -n argocd                  # which app, SYNC/HEALTH
kubectl get events -n mesh-lab --sort-by=.lastTimestamp | Select-Object -First 30
kubectl get rollout <name> -n mesh-lab -o jsonpath="{.status.phase}|{.status.conditions}"
```

## 1. Chart version doesn't exist

**Symptom**: app stuck `Unknown` sync; condition
`ComparisonError: chart "base" version "1.31.0" not found`.
**Cause**: hand-scraped version numbers were wrong.
**Fix**: always confirm with Helm itself:
```powershell
docker run --rm --entrypoint sh alpine/helm:3.16.4 -c `
  "helm repo add istio https://istio-release.storage.googleapis.com/charts && helm search repo istio/base --versions"
```

## 2. Wrong service DNS names

**Symptom**: gateway -> inventory returned 503.
**Cause**: Argo CD names the Helm release after the Application
(`mesh-lab-inventory`) so fullname doubled up.
**Diagnosis**: `kubectl get svc -n mesh-lab`, compare with env vars in pods.
**Fix**: `helm.releaseName: mesh-lab` in each Application.

## 3. Pod missing sidecar after installing Istio

**Symptom**: one pod 1/1 while others are 2/2; STRICT mTLS resets.
**Cause**: pod created before the injection webhook existed.
**Fix**: `kubectl delete pod <pod>` - recreated pod gets injected. Don't
patch templates: selfHeal reverts non-Git changes.

## 4. Prometheus can't scrape (connection reset)

**Symptom**: targets DOWN, `read: connection reset by peer`.
**Cause**: STRICT PeerAuthentication rejects plaintext scraper.
**Fix**: PERMISSIVE + path exemption in AuthorizationPolicy for
`/actuator/prometheus`.

## 5. Analysis aborts with `slice index out of range`

**Cause**: empty PromQL vector when a fresh pod has no samples yet.
**Fix**: `(...) or vector(1)` in the query (doc 13).

## 6. Gates pass but shouldn't (vacuous success)

**Diagnosis chain**:
```powershell
kubectl get analysisrun <run> -o jsonpath="{.spec.args}"        # hash empty?
# query prometheus directly for the metric+label
kubectl get pod chaos-traffic -n mesh-lab                       # traffic alive?
```
**Causes found**: fieldRef instead of podTemplateHashValue; dead traffic pod.

## 7. Argo CD stuck on old revision / GitHub hiccup

```powershell
kubectl annotate app <name> -n argocd argocd.argoproj.io/refresh=hard --overwrite
```

## 8. Manual kubectl edits keep reverting

That's `selfHeal: true`. Change Git instead, or delete a pod to force
recreation without template drift.

## 9. API server TLS timeouts under load

Single-node lab clusters get saturated during big syncs. Wait and retry;
prefer `--request-timeout` on kubectl. For real work, give the node more RAM.

## 10. Docker Desktop dies mid-exercise

k3d state survives: restart Docker Desktop, then
`kubectl get nodes` - everything resumes; Argo CD re-syncs on its own.

## Golden rules learned

1. Verify versions against the live registry, never hand-copy numbers.
2. Read `.spec.args` of an actual AnalysisRun before trusting "Successful".
3. Rollback drills need synthetic load - no traffic means no signal.
4. Events + conditions answer 90% of "why is it stuck" questions.
