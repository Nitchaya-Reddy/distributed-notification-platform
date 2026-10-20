# Kubernetes deployment

These manifests deploy the **application layer**. Use managed PostgreSQL, Kafka, and Redis (or provide equivalent in-cluster services).

1. Build and push `notification-api` and `notification-worker` images.
2. Replace `YOUR_*` values in `config.yaml` and image names in `api.yaml` / `worker.yaml`.
3. In production, store database credentials in your secret manager rather than committing literal values.
4. Run database migrations as a controlled release step before scaling workers.
5. Pre-create Kafka topics with your production replication factor, retention, ACLs, and min ISR.
6. Apply manifests in order:

```bash
kubectl apply -f namespace.yaml
kubectl apply -f config.yaml
kubectl apply -f api.yaml
kubectl apply -f worker.yaml
```

The worker deployment can be scaled horizontally; useful active consumer parallelism is ultimately bounded by delivery-topic partition count.
