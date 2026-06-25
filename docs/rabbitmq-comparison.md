# Local executor vs RabbitMQ

The project supports two RAG job transports without changing ingestion logic:

- `RAG_TRANSPORT=local`: submits work to the application process thread pool.
- `RAG_TRANSPORT=rabbitmq`: publishes a persistent message to a durable queue.

## Expected architectural difference

| Dimension | Local executor | RabbitMQ |
| --- | --- | --- |
| Request decoupling | Yes | Yes |
| Survives application restart | Recovery depends on database scan | Queued messages remain durable |
| Back pressure | In-memory queue of 100 | Broker queue, visible and monitorable |
| Horizontal consumers | Single application process | Multiple application instances |
| Failed-message isolation | Database `FAILED` status | `FAILED` status plus dead-letter queue |
| Operational cost | Low | Requires RabbitMQ |

## Repeatable measurement

Use `tools/compare-rag-transport.ps1` with a directory containing unique PDF, Word, or TXT
files. Run the backend once with `RAG_TRANSPORT=local`, then again with
`RAG_TRANSPORT=rabbitmq`. The script records:

- `ack_ms`: time until the upload API accepts and persists the document.
- `ready_ms`: total time until every document reaches `READY` or `FAILED`.
- `result`: final processing state.

Use the same machine, files, embedding model, and empty knowledge base for both runs. Report
median and P95 rather than one request. The RabbitMQ goal is durability and controlled back
pressure; it may not reduce single-document completion latency.
