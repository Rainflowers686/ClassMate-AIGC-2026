# Analysis prompt — v1

This is the human-readable spec for the prompt produced by
[`PromptBuilder`](../../core/src/main/kotlin/com/classmate/core/prompt/PromptBuilder.kt).
Keep this file and the code in sync; the code is the source of truth at runtime.

## Goal

Turn one Chinese lecture transcript into an **evidence-bound** analysis: real concepts +
learning-oriented micro-tests, returned as strict JSON.

## System message (rules)

1. Output **JSON only** — no markdown, no explanation.
2. Distil real **concepts** (e.g. 级数收敛与发散); do **not** copy paragraphs into knowledge points.
3. **Merge** duplicate / synonymous points.
4. Every knowledge point binds a `sourceSegmentId` and verbatim `evidenceQuotes` locatable in that segment.
5. Micro-tests must assess understanding — types: `CONCEPT_UNDERSTANDING`, `JUDGMENT`,
   `APPLICATION`, `ERROR_ANALYSIS`, `TRANSFER`. **No** "which line is closest to the original".
6. Every question references the knowledge point(s) it tests (`testedKnowledgePoints`) and cites evidence.
7. Every question has at least one correct option and an `explanation`.
8. **No conclusion without evidence.**
9. Do **not** output a review plan — the app generates it later from importance / difficulty /
   wrong answers / feedback.
10. If the rules cannot be met, output `{"knowledgePoints":[],"quizQuestions":[]}` so the app
    falls back — never invent.

## Output contract

See [`course_analysis_result.schema.json`](../schema/course_analysis_result.schema.json) and the
wire DTOs in
[`WireModels.kt`](../../core/src/main/kotlin/com/classmate/core/parser/WireModels.kt).

## User message (content)

```
课程标题：<title>
以下是课堂文本，已分段，每段前标注了 segment id：

[seg_1] <text>
[seg_2] <text>
...

要求：最多提炼 <N> 个知识点；每个知识点出 <M> 道微测题。严格按 system 中的 JSON schema 输出。
```

## How the output is consumed

1. `JsonExtractor` pulls the JSON object (handles ```json fences and stray prose).
2. `AnalysisJsonParser` assigns ids and resolves `evidenceQuotes` → `EvidenceSpan` offsets;
   maps `testedKnowledgePoints` titles → ids.
3. `ResultValidator` + `EvidenceValidator` enforce evidence + reference closure.
4. Any failure → the resolver falls back to the next provider (and ultimately the local heuristic).
