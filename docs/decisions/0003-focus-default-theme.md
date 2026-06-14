# ADR-0003: Focus is the default theme

- Status: Accepted (2026-05-31)

## Decision

Ship three switchable themes — **Focus** (default), **Vitality**, **Flow** — and make Focus the
default.

## Why

ClassMate's headline is *evidence-based, scholarly understanding* for concept-dense courses. A
calm, academic look (ink + paper, knowledge indigo, **amber evidence highlight**) reads as
credible to judges and screenshots cleanly. Vitality (youthful, encouraging, growth) and Flow
(calm, immersive, deep-focus / white-noise scenarios) cover motivation and focus sessions, and
remain one tap away in Settings.

## Consequences

- The evidence highlight stays recognisable across all three themes (it's the brand element).
- Theme + light/dark are runtime state in `AppViewModel`; switching is instant and screenshot-able.
- A bundled brand font can later replace the system sans without touching screens.
