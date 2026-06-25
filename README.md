# AI Blog

An AI-assisted blog and personal RAG knowledge base built with Spring Boot 3,
Vue 3, MySQL, Redis, RabbitMQ, Qdrant, DeepSeek, DashScope embeddings, and Cloudflare R2.

## Local infrastructure

1. Copy `.env.example` to `.env` and replace every secret placeholder.
   If Docker Hub is not reachable from your network, set `DOCKER_IMAGE_PREFIX`
   in `.env` to an accessible Docker Hub mirror prefix. Keep it empty when
   Docker Hub works normally.
2. Start the stack with `docker compose up --build`.
3. Open `http://localhost:5173`; backend health is available at
   `http://localhost:8080/actuator/health`.

The first MySQL startup applies `docker/mysql/init/01-schema.sql`. Existing Docker
volumes are not modified automatically when that file changes.

## Development

Backend requires JDK 17:

```bash
mvn -pl server -am test
mvn -pl server -am spring-boot:run
```

Frontend requires Node.js 20.19+ or 22.12+:

```bash
cd frontend
npm install
npm run dev
```

Set `VITE_API_BASE` when the backend is not available at `http://localhost:8080`.

## RAG processing

Uploaded documents move through `PROCESSING`, `READY`, and `FAILED`. Parsing and
vectorization are dispatched through RabbitMQ by default. Failed jobs can be retried from
the knowledge base manager. Set `RAG_TRANSPORT=local` to use the original bounded executor
as a comparison baseline. RabbitMQ management is available at `http://localhost:15672`.
Deleting a document removes its Qdrant vectors before deleting database metadata and the
R2 object.

Metrics are exposed through Actuator and Prometheus. Application-specific metrics:

- `blog_rag_ingestion_total{result="success|failed"}`
- `blog_rag_ingestion_duration_seconds`
- `blog_rag_dispatch_total{transport="local|rabbitmq"}`

See `docs/rag-evaluation.md` for the retrieval-quality baseline.
See `docs/rabbitmq-comparison.md` for the transport comparison procedure.

## Security

Never commit `.env` or `application-dev.yml`. Credentials previously stored in local
configuration should be rotated in the DeepSeek, DashScope, Cloudflare, and JWT systems;
removing a value from Git does not revoke it.
