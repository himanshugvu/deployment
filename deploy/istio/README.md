# Istio service mesh

Platform components are installed by Argo CD applications:

| App | Chart | Namespace |
| --- | --- | --- |
| `istio-base` | `base` 1.30.3 | istio-system |
| `istiod` | `istiod` 1.30.3 | istio-system |
| `kiali` | `kiali-server` 2.30.0 | kiali |

The `config/` folder holds the mesh configuration for the `mesh-lab`
namespace and is synced by the `mesh-config` Argo CD application:

- `config/gateway.yaml`: edge `Gateway` on host `mesh-lab.local` plus a
  `VirtualService` routing everything to the Spring Cloud Gateway service
- `config/destinationrules.yaml`: mTLS + outlier detection per service
- `config/peerauthentication.yaml`: STRICT mutual TLS in `mesh-lab`
- `config/telemetry.yaml`: Envoy access logging for the namespace
- `config/authorizationpolicy.yaml`: only the gateway (and the Istio ingress)
  may call `mesh-lab-inventory`

## Traffic path

```text
client -> istio-ingressgateway (mesh-lab.local) -> mesh-lab-gateway :80
       -> /api/*            -> mesh-lab
       -> /api/inventory/** -> mesh-lab-inventory
```

## Try it

With the ingress gateway exposed (`minikube tunnel`, `istioctl install`,
or `kubectl port-forward svc/istio-ingressgateway -n istio-system 8080:80`):

```powershell
curl http://mesh-lab.local/api/status --resolve mesh-lab.local:80:127.0.0.1
curl http://mesh-lab.local/api/inventory/items --resolve mesh-lab.local:80:127.0.0.1
kubectl logs deploy/mesh-lab-mesh-lab -c istio-proxy -n mesh-lab   # access logs
```

Open Kiali for the topology view:

```powershell
kubectl port-forward svc/kiali 20001:20001 -n kiali
```

Note: because of STRICT mTLS, direct calls between services without an
injected sidecar will fail; that is intentional for the lab.
