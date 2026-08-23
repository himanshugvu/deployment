# 14 - Chaos Drills (Proving the Rollback)

Trust, but verify. The rollback pipeline is only real if you've watched it
fire. This lab ships a failure injector for exactly that.

## The knob

Every backend reads `CHAOS_ERROR_RATE` (0-100) and returns HTTP 500 on that
percentage of requests:

```java
if (chaosErrorRate > 0 && ThreadLocalRandom.current().nextInt(100) < chaosErrorRate)
    throw new ChaosException();   // 500
```

Charts expose it as `chaos.errorRate` values; Argo CD parameters flip it per
environment without touching code.

## The drill (as executed live)

1. **Arm it** - add to `deploy/argocd/apps/mesh-lab-inventory.yaml`:
   ```yaml
   - { name: chaos.errorRate, value: "50" }
   - { name: app.version, value: v3 }   # forces a fresh ReplicaSet
   ```
2. **Generate traffic** so metrics actually exist:
   ```powershell
   kubectl run chaos-traffic -n mesh-lab --image=busybox:1.36 --restart=Never `
     --command -- /bin/sh -c "while true; do wget -qO- -T 2 `
     http://mesh-lab-mesh-lab-gateway/api/inventory/items >/dev/null; sleep 1; done"
   ```
3. **Push** and watch:
   ```text
   13:03:54 Progressing step0     canary (v3+chaos) at 25%
   13:05:10 step2 analysis measuring...
   13:05:35 AnalysisRunFailed -> RolloutAborted -> canary scaled to 0
   ```
4. **Evidence** from the failed run:
   ```json
   "measurements": [{ "value": "[0.6085953602551475]", "phase": "Failed" }],
   "message": "Metric \"success-rate\" assessed Failed due to failed (1) > failureLimit (0)"
   ```
   Success rate 60.9% < 90% threshold -> automatic rollback, stable untouched.
5. **Disarm**: remove the parameters, push; a clean canary cycles through all
   gates back to Healthy.

## Drill checklist

- [ ] traffic generator running BEFORE the gate window (no traffic = vacuous pass)
- [ ] `kubectl get analysisrun <run> -o jsonpath="{.spec.args}"` shows a real hash
- [ ] measured value in `status.metricResults[0].measurements[0].value`
- [ ] stable RS still serving during/after abort (`rollout status` shows Degraded, service fine)

## What we learned the hard way

A first drill "passed" despite chaos because (a) the hash arg was empty and
(b) the traffic pod had died during a cluster restart - zero requests meant
the query returned no data and `or vector(1)` called it healthy. A rollback
system is only as good as its telemetry; always drill with observable load.

Read next: [15 - Debugging runbook](15-debugging-runbook.md)
