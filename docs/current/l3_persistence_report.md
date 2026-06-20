# L3 Persistence Report

Date: 2026-06-20

## Status

L3 critical study data is now persisted to app-private storage through `classmate_l3_store.json`.

The L3 store contains study artifacts only. It does not store AppKey, Authorization, endpoint URLs, raw provider request bodies, or `config.local.json` contents.

## Persisted Across Restart

- lesson source
- evidence
- generated questions
- practice attempts
- wrong question book
- review queue
- mastery stats
- mastery history events
- exam reports

## Related Stores

- `classmate_semantic_index.json`: local lexical semantic index records.
- `classmate_learning_state.json`: existing cross-course learning state.
- `classmate_history.json`: existing course history.
- `classmate_theme_preferences.json`: appearance preferences only.
- `classmate_model_config.json`: app-private model config; not part of L3 study data.

## Failure Behavior

- Missing store: loads an empty L3 snapshot.
- Corrupted store: falls back to empty L3 snapshot without crashing.
- Existing in-memory pipeline still works if persistence is unavailable.

## Validation

Unit tests save and reload a snapshot containing a lesson, question, wrong answer, review queue, mastery history, and exam report.
