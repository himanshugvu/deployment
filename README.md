# Mesh Lab Workspace

This repository is organized as a platform workspace so independent services,
an API gateway, Helm charts, and Argo CD manifests can live together.

## Layout

- `services/mesh-lab` contains the current Spring Boot service
- `deploy/helm/mesh-lab` contains the current Helm chart
- `deploy/argocd/application.yaml` contains the Argo CD application manifest
- `sources/` is mirrored reference material and should not be committed

## Build and test

Each app is independent and should be built from its own folder. For the
current service:

```powershell
cd services/mesh-lab
mvn test
mvn package
docker build -t ghcr.io/your-org/mesh-lab:0.0.1 .
```

## Kubernetes deploy

```powershell
kubectl create namespace mesh-lab
helm upgrade --install mesh-lab deploy/helm/mesh-lab -n mesh-lab
kubectl get pods -n mesh-lab
kubectl port-forward svc/mesh-lab-mesh-lab 8080:80 -n mesh-lab
```

## Argo CD

1. Install Argo CD in your cluster.
2. Push this repository to Git.
3. Build and push the service image to your container registry.
4. Update `repoURL`, `image.repository`, and `image.tag` in `deploy/argocd/application.yaml` to your real values.
5. Apply the application manifest.

```powershell
kubectl apply -f deploy/argocd/application.yaml
```

Commit the repository root, but exclude mirrored metadata and generated output.

## Planned expansion

The intended next additions are:

- more services under `services/`
- an API gateway service
- Istio and Kiali
- Prometheus and telemetry
- Argo Rollouts for progressive delivery
# deployment
