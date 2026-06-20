# Semantic Index and Similarity Report

Date: 2026-06-20

## Current Status

Local semantic index is `COMPLETE` for lightweight lexical retrieval and `LOCAL_FALLBACK` for similarity. Official embedding/similarity providers remain smoke-verified but are not called in this sprint.

## Implemented

- `LocalSemanticIndexRecord` stores source type, owner id, text, vector, tokens, embedding status, and timestamp.
- `LocalSemanticIndexRepository` persists records to app-private storage.
- `LocalSemanticIndexEngine.search` returns top local hits.
- Similar-question lookup uses the same local vector fallback.
- Course detail and Review can expose semantic search status.

## Honest Limits

The local vector is lexical and deterministic. It is not a persistent vector database and it does not pretend to be an official embedding result.
