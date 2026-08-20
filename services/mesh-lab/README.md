# Mesh Lab Service

This service is the first building block in the larger mesh lab workspace.

- Java 25
- one Spring Boot service
- one Docker image
- one Helm chart
- one Argo CD application manifest

## What is included

The service exposes:

- `GET /api/status`
- `POST /api/echo`
- Spring Boot Actuator health endpoints for Kubernetes probes

The related deployment assets live outside this folder:

- `Dockerfile` in this folder
- Helm chart under `../../deploy/helm/mesh-lab`
- Argo CD application manifest under `../../deploy/argocd/application.yaml`

## Local build

```powershell
mvn test
mvn package
```

## Docker image

```powershell
docker build -t ghcr.io/himanshugvu/mesh-lab:0.0.1 .
```

## Helm deploy to Docker Desktop Kubernetes

```powershell
kubectl create namespace mesh-lab
helm upgrade --install mesh-lab ../../deploy/helm/mesh-lab -n mesh-lab
kubectl get pods -n mesh-lab
kubectl port-forward svc/mesh-lab-mesh-lab 8080:80 -n mesh-lab
```

## Argo CD integration

1. Install Argo CD in your cluster.
2. Build and push `ghcr.io/himanshugvu/mesh-lab:0.0.1`.
3. Apply the namespace and Argo CD application:

```powershell
kubectl apply -f ../../deploy/argocd/namespace.yaml
kubectl apply -f ../../deploy/argocd/application.yaml
```

Argo CD will then sync the Helm chart from Git into the `mesh-lab` namespace.
