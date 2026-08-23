# Mesh Lab - Learning Series

Fifteen short docs covering everything built in this workspace, in reading
order. Each stands alone but they build on each other.

| # | Doc | You learn |
| --- | --- | --- |
| 01 | [Platform overview](01-platform-overview.md) | repo map, traffic path, GitOps contract |
| 02 | [Spring Boot services](02-spring-boot-services.md) | both backends, conventions, chaos hook |
| 03 | [API gateway](03-api-gateway.md) | Spring Cloud Gateway routes + version pairing |
| 04 | [Docker packaging](04-docker-images.md) | multi-stage builds, tags, k3d import trick |
| 05 | [Helm charts](05-helm-charts.md) | chart anatomy, helpers, feature flags, linting |
| 06 | [Workloads & probes](06-workload-and-probes.md) | Deployment/Rollout switch, probes, SA, env |
| 07 | [Argo CD GitOps](07-argocd-gitops.md) | app-of-apps, sync policies, release names |
| 08 | [Sync waves](08-sync-waves-and-ordering.md) | dependency ordering, self-heal behavior |
| 09 | [Istio mesh](09-istio-mesh.md) | sidecars, mTLS, DRs, AuthzPolicy, telemetry |
| 10 | [Prometheus](10-prometheus-telemetry.md) | ServiceMonitors, relabeling, success-rate PromQL |
| 11 | [Canary rollouts](11-canary-rollouts.md) | steps, weights, analysis gates |
| 12 | [Blue-green rollouts](12-bluegreen-rollouts.md) | preview/active services, post-promotion analysis |
| 13 | [Analysis & rollback](13-analysis-and-rollback.md) | the query, thresholds, three pitfalls fixed live |
| 14 | [Chaos drills](14-chaos-drills.md) | proving rollback with injected 500s |
| 15 | [Debugging runbook](15-debugging-runbook.md) | every real failure and its diagnosis |

Suggested paths:

- **App developer**: 01 -> 02 -> 03 -> 14
- **Platform/DevOps**: 05 -> 07 -> 08 -> 09 -> 10
- **Release engineering**: 11 -> 12 -> 13 (then 14 to prove it)
