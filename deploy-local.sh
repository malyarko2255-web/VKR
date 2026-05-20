#!/usr/bin/env bash
set -euo pipefail

echo "=== Advance Service Local Deploy (minikube) ==="

# Start minikube if not running
if ! minikube status | grep -q "Running"; then
    echo "Starting minikube..."
    minikube start --cpus 4 --memory 8192 --driver docker
fi

# Enable required addons
minikube addons enable ingress
minikube addons enable metrics-server

# Use minikube Docker daemon for local images
eval "$(minikube docker-env)"

echo ""
echo "Building Docker images..."
MODULES=(advance-gateway advance-identity advance-core advance-payment advance-scoring advance-reference advance-notification advance-frontend)
for mod in "${MODULES[@]}"; do
    echo "  Building $mod..."
    docker build -t "ghcr.io/finuniversity/$mod:latest" -f "$mod/Dockerfile" .
done

echo ""
echo "Applying Kubernetes manifests..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml

echo "Deploying infrastructure..."
kubectl apply -f k8s/infra/

echo "Waiting for infrastructure to be ready..."
kubectl rollout status statefulset/advance-postgres -n advance-system --timeout=120s
kubectl rollout status statefulset/advance-redis -n advance-system --timeout=120s
kubectl rollout status statefulset/advance-kafka -n advance-system --timeout=180s

echo "Deploying services..."
kubectl apply -f k8s/services/

echo "Waiting for services to be ready..."
for svc in advance-gateway advance-identity advance-core advance-payment advance-scoring advance-reference advance-notification advance-frontend; do
    kubectl rollout status deployment/$svc -n advance-system --timeout=120s
done

kubectl apply -f k8s/ingress.yaml

echo ""
echo "=== Deploy complete! ==="
echo ""
echo "Services:"
for svc in advance-gateway advance-identity advance-core advance-payment advance-scoring advance-reference advance-notification advance-frontend; do
    echo "  $svc: $(kubectl get svc $svc -n advance-system -o jsonpath='{.spec.ports[0].port}' 2>/dev/null || echo 'pending')"
done

echo ""
echo "Gateway URL:"
minikube service advance-gateway -n advance-system --url 2>/dev/null || echo "  Use: kubectl port-forward svc/advance-gateway 8080:8080 -n advance-system"
