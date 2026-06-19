# ClassMate Theme Extraction Report

Date: 2026-06-19

Scope: extract design tokens from Stitch exports into ClassMate documentation. No app code, Compose code, navigation, ViewModel, provider, Gradle, or runtime behavior was changed.

## Source Files Read

Standard Study / 默认学习主题:

- `design_refs/stitch_themes/standard_study_dashboard/notes.md`
- `design_refs/stitch_themes/standard_study_dashboard/zenith_ethereal/DESIGN.md`
- `design_refs/stitch_themes/standard_study_dashboard/dashboard/code.html`
- `design_refs/stitch_themes/standard_study_dashboard/insights/code.html`
- `design_refs/stitch_themes/standard_study_dashboard/settings/code.html`
- `design_refs/stitch_themes/standard_study_dashboard/community/code.html`
- `design_refs/stitch_themes/standard_study_dashboard/dashboard/screen.png`
- `design_refs/stitch_themes/standard_study_dashboard/insights/screen.png`
- `design_refs/stitch_themes/standard_study_dashboard/settings/screen.png`
- `design_refs/stitch_themes/standard_study_dashboard/community/screen.png`

Active Study / 活力主题:

- `design_refs/stitch_themes/active_study_main_dashboard/notes.md`
- `design_refs/stitch_themes/active_study_main_dashboard/veridian_flow/DESIGN.md`
- `design_refs/stitch_themes/active_study_main_dashboard/main_dashboard/code.html`
- `design_refs/stitch_themes/active_study_main_dashboard/automations/code.html`
- `design_refs/stitch_themes/active_study_main_dashboard/usage_history/code.html`
- `design_refs/stitch_themes/active_study_main_dashboard/main_dashboard/screen.png`
- `design_refs/stitch_themes/active_study_main_dashboard/automations/screen.png`
- `design_refs/stitch_themes/active_study_main_dashboard/usage_history/screen.png`

Focus Immersion / 沉浸式主题:

- `design_refs/stitch_themes/focus_immersion_vertical_feed/notes.md`
- `design_refs/stitch_themes/focus_immersion_vertical_feed/neon_noir/DESIGN.md`
- `design_refs/stitch_themes/focus_immersion_vertical_feed/vertical_feed/code.html`
- `design_refs/stitch_themes/focus_immersion_vertical_feed/discover/code.html`
- `design_refs/stitch_themes/focus_immersion_vertical_feed/vertical_feed/screen.png`
- `design_refs/stitch_themes/focus_immersion_vertical_feed/discover/screen.png`

Extraction priority: HTML Tailwind color maps and CSS first, DESIGN.md second, screenshots third for visual validation only.

## Fixed Theme Mapping

The required mapping is preserved exactly:

| Stitch source | ClassMate mapping |
|---|---|
| Dashboard | Standard Study / 默认学习主题 |
| Main Dashboard | Active Study / 活力主题 |
| Vertical Feed | Focus Immersion / 沉浸式主题 |

No fourth theme was added. No theme name was changed.

## Token Extraction Summary

Detailed token tables are in `docs/current/classmate_theme_engine_v1.md`.

| Theme | Surface direction | Accent direction | Typography | Shape direction |
|---|---|---|---|---|
| Standard Study | Off-white base `#f8faf3`, white cards, low sage containers | sage primary `#55624d`, soft warm callout `#fed7d2` | Manrope headlines, Plus Jakarta Sans body/label | 16dp cards, 24dp hero/modal, full pills |
| Active Study | light blue-white base `#f8f9ff`, progress-card containers | green primary `#006d32`, blue secondary `#0059bb` | Space Grotesk headlines, Inter body/label | 12dp cards, 8dp compact buttons, full toggles/chips |
| Focus Immersion | deep dark base `#0e0e0e` / `#131313`, glass panels | pink/rose source accent with cyan tertiary; should be accent-preset controlled | Space Grotesk headlines, Inter body/label | full pill actions, 16-32dp immersive panels |

## What ClassMate Can Absorb

Standard Study:

- Calm dashboard spacing.
- Off-white surfaces and low-contrast tonal layering.
- Floating pill navigation.
- Readable card hierarchy for daily learning, course details, Ask, Review, Settings, and Export.

Active Study:

- Progress rhythm and high-clarity status surfaces.
- Green/blue action language for practice, feedback, review momentum, and streaks.
- Bento-style progress cards when the information density is useful.

Focus Immersion:

- Dark immersive depth.
- Glass bottom navigation and round icon controls.
- Full-screen focus surface hierarchy for Flow, ambient sound, and night review.
- Large, direct focus typography.

## What ClassMate Must Not Copy

- Original source product names or business labels.
- Source-domain metrics, vertical-feed content, profile content, or utility logic.
- Poster/feed interaction patterns from the vertical feed source.
- A fourth theme or renamed theme presets.
- Direct HTML-to-Compose translation.

## Component Findings

| Component | Standard Study | Active Study | Focus Immersion |
|---|---|---|---|
| Card | white or low-sage surface, soft shadow, no hard dividers | low/white progress cards, bento modules | dark tonal cards, glass panels |
| Bottom nav | floating translucent full pill | anchored or glass functional bar | floating glass full pill |
| Button | sage pill, restrained | compact green primary, blue secondary | full pill, gradient allowed through selected accent |
| Search/input | low surface fill, 16-24dp radius | low surface fill plus ghost outline | dark high surface full pill |
| Icon button | circular/squircle low surface | 40dp circular surface shift | 48-56dp glass circle |
| Status pill | soft primary-fixed or warm callout | high-contrast primary-fixed/secondary | dark full pill, cyan or primary accent |
| Focus surface | breathing-style card | progress/stat surface | full immersive dark panel |

## Token Conflicts and Conservative Decisions

| Conflict | Source evidence | Conservative ClassMate decision |
|---|---|---|
| Standard Study radius varies | DESIGN.md recommends 1.5rem hero radius; HTML often uses rounded-2xl | Use 16dp for normal cards and 24dp for hero/modal surfaces. |
| Active Study radius varies | DESIGN.md says 0.5rem button radius; HTML cards use rounded-xl and rounded-2xl | Use 8dp buttons, 12dp regular cards, 16dp bento/hero cards. |
| Focus Immersion card radius varies | DESIGN.md suggests very large visual cards; HTML uses rounded-lg in grids and full pills in feed | Use 16dp dense cards, 24-32dp focus panels, full pill controls. |
| Focus source accent is pink-heavy | DESIGN.md and HTML use `#ffb1c3` / `#ff4b89` | Keep dark surface hierarchy; allow Settings accent preset to replace the hot accent. Do not force pink across ClassMate. |
| Warning semantic token is weak in Stitch | Source themes mostly define primary/secondary/error, not warning | Use Amber accent preset for real warnings; only use extracted warm/secondary colors for soft callouts. |
| Border guidance conflicts with accessibility | DESIGN.md says no hard 1px borders, HTML includes low-opacity outlines | Use ghost borders only at low opacity when accessibility or separation requires it. |

## Theme Engine v1 Recommendation

Settings should eventually expose:

- `ThemePreset`: `STANDARD_STUDY`, `ACTIVE_STUDY`, `FOCUS_IMMERSION`.
- `AccentColorPreset`: Blue, Cyan, Green, Purple, Amber, Rose, Graphite, Ocean.

Behavior:

- Theme preset controls surface hierarchy, typography family, shape scale, and component rhythm.
- Accent preset controls selected emphasis colors for primary actions, active navigation, progress, and status.
- Accent preset must not flatten the theme's source-derived surface hierarchy.
- Focus Immersion should support Rose/Cyan/Ocean/Graphite well, but it must remain study-focused and not inherit source media content.

## Implementation Boundary

This task produced documentation only:

- `docs/current/classmate_theme_engine_v1.md`
- `docs/current/classmate_theme_extraction_report.md`

No Kotlin code was generated. No Compose implementation was created. No `app/src/main`, `core`, `scripts`, Gradle, `.github`, provider logic, ViewModel, or navigation file was modified.

## Next Step

Use `classmate_theme_engine_v1.md` as the input spec for a later implementation task. That later task should map these documented tokens into existing ClassMate theme infrastructure without a global UI rewrite.
