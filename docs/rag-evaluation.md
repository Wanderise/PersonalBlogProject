# RAG evaluation baseline

Use `server/src/test/resources/rag-evaluation/cases.json` as the reviewed question set.
Each case records the knowledge base, question, expected terms, and source document.

Run the same cases after changing these environment variables:

- `RAG_SEARCH_TOP_K`: candidate count, default `6`.
- `RAG_SIMILARITY_THRESHOLD`: minimum vector similarity, default `0.2`.

Record four values for every run: hit rate, source recall, answer latency, and token cost.
Do not tune against a single document; keep at least 20 questions across factual lookup,
cross-document synthesis, and deliberately unanswerable questions. An unanswerable case
passes only when the application avoids inventing an answer.

Recommended experiment matrix:

| Run | TopK | Threshold | Purpose |
| --- | ---: | ---: | --- |
| A | 4 | 0.20 | Precision-oriented baseline |
| B | 6 | 0.20 | Default balance |
| C | 8 | 0.15 | Recall-oriented comparison |

Add reranking only after the baseline is stable, then compare it against run B with the
same dataset. Keep the evaluation file in version control so quality regressions are visible.
