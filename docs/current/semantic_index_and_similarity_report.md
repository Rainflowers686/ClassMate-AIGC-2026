# Semantic Index and Similarity Report

Date: 2026-06-20

## Current Status

Local semantic index is `COMPLETE` for lightweight lexical retrieval and `LOCAL_FALLBACK` for similarity. v1.7 injects Vivo Embedding and Text Similarity adapters into the production `OfficialRuntimeGatewayFactory`; official success is recorded as official provenance, while missing config/runtime failure falls back to local vectors and local similarity. Current status is `OFFICIAL_RUNTIME_READY / VALIDATION_PENDING`, not guaranteed `OFFICIAL_RUNTIME_USED`.

## Implemented

- `LocalSemanticIndexRecord` stores source type, owner id, text, vector, tokens, embedding status, timestamp, `officialVector`, `localVector`, and `vectorSource`.
- `LocalSemanticIndexRepository` persists records to app-private storage.
- `LocalSemanticIndexEngine.search` returns top local hits.
- Similar-question lookup uses the same local vector fallback.
- `TextSimilarityMatch` stores `scoreSource`, so official rerank scores and local fallback scores are distinguishable.
- Runtime diagnostics report whether official Embedding/Text Similarity were ready, used, fell back, or were not configured.
- Course detail and Review can expose semantic search status.

## Honest Limits

The local vector is lexical and deterministic. It is not a persistent vector database and it does not pretend to be an official embedding result. Official vectors are only recorded when the official runtime adapter returns a vector successfully during demo/cloud validation.
