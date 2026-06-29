> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# ClassMate Theme Engine v1

Status: design-token extraction only. This file does not define Kotlin or Compose implementation.

## Fixed Theme Mapping

ClassMate must keep exactly three theme presets:

| Stitch source | ClassMate theme preset | Product meaning |
|---|---|---|
| Dashboard | `STANDARD_STUDY` / Standard Study / 默认学习主题 | Daily learning, home, course detail, Ask, Review, Settings, Export |
| Main Dashboard | `ACTIVE_STUDY` / Active Study / 活力主题 | Practice, progress, streaks, momentum, review action surfaces |
| Vertical Feed | `FOCUS_IMMERSION` / Focus Immersion / 沉浸式主题 | Focus mode, ambient sound, night learning, deep review |

Do not add a fourth theme. Do not rename these presets. Do not copy source-domain content, names, metrics, media-feed behavior, or original business semantics into ClassMate.

## ThemePreset and AccentColorPreset

`ThemePreset` design values:

| Preset | Source basis | Default accent |
|---|---|---|
| `STANDARD_STUDY` | soft off-white dashboard surfaces, sage primary, restrained warm callouts | Green or Graphite |
| `ACTIVE_STUDY` | clean light dashboard, high-activity green and blue progress language | Green or Blue |
| `FOCUS_IMMERSION` | dark immersive vertical rhythm, glass panels, strong focus hierarchy | Rose, Cyan, Ocean, or Graphite |

`AccentColorPreset` values for Settings:

| Accent | Suggested core color | Notes |
|---|---:|---|
| Blue | `#2563eb` | General academic / cloud source / information |
| Cyan | `#00a0aa` | Focus highlights, new insight, success alternative in dark mode |
| Green | `#006d32` | Progress, correct, practice momentum |
| Purple | `#7c3aed` | Knowledge map or concept grouping |
| Amber | `#b26a00` | Warnings and due-soon review tasks |
| Rose | `#ff4b89` | Focus Immersion accent only; keep restrained |
| Graphite | `#353535` | Neutral / high contrast / developer-safe state |
| Ocean | `#0059bb` | Secondary action, retrieval, evidence flow |

Accent presets should tint `primary`, `primaryContainer`, status highlights, active navigation, and progress accents. They should not replace each theme's surface hierarchy.

## Compose-Ready Color Tokens

Values are extracted from Stitch HTML Tailwind color maps and DESIGN.md. Semantic tokens with no direct source token are mapped conservatively and noted.

### STANDARD_STUDY

| Token | Value | Source token / rationale |
|---|---:|---|
| background | `#f8faf3` | `background`, `surface` |
| surface | `#f8faf3` | `surface` |
| surfaceVariant | `#e1e3dc` | `surface-variant` |
| surfaceContainerLow | `#f2f4ed` | `surface-container-low` |
| surfaceContainerHigh | `#e7e9e2` | `surface-container-high` |
| primary | `#55624d` | `primary`; key icons, titles, primary actions |
| primaryContainer | `#98a68e` | `primary-container`; gradient/progress companion |
| secondary | `#755754` | `secondary`; warm action/callout text |
| secondaryContainer | `#fed7d2` | `secondary-container`; reward/callout surface |
| tertiary | `#605f56` | `tertiary`; quiet neutral accent |
| textPrimary / onSurface | `#191c18` | `on-surface`, `on-background` |
| textSecondary / onSurfaceVariant | `#444841` | `on-surface-variant` |
| border / outline | `#757870` | `outline`; use sparingly |
| outlineVariant | `#c5c8be` | ghost border at 10-15% opacity |
| success | `#55624d` | extracted progress/sage primary; use for correct/mastered |
| warning | `#755754` | extracted warm secondary; for soft caution only |
| error | `#ba1a1a` | `error` |
| info | `#605f56` | extracted tertiary/neutral info |

### ACTIVE_STUDY

| Token | Value | Source token / rationale |
|---|---:|---|
| background | `#f8f9ff` | `background`, `surface` |
| surface | `#f8f9ff` | `surface` |
| surfaceVariant | `#d3e4fe` | `surface-variant` |
| surfaceContainerLow | `#eff4ff` | `surface-container-low` |
| surfaceContainerHigh | `#dce9ff` | `surface-container-high` |
| primary | `#006d32` | `primary`; progress/action green |
| primaryContainer | `#00d166` | `primary-container`; glow/progress companion |
| secondary | `#0059bb` | `secondary`; blue secondary flow |
| secondaryContainer | `#0070ea` | `secondary-container`; saturated information/action panel |
| tertiary | `#565e74` | `tertiary`; neutral-blue tertiary |
| textPrimary / onSurface | `#0b1c30` | `on-surface`, `on-background` |
| textSecondary / onSurfaceVariant | `#3c4a3d` | `on-surface-variant` |
| border / outline | `#6c7b6c` | `outline` |
| outlineVariant | `#bbcbb9` | ghost border at 10-20% opacity |
| success | `#006d32` | extracted primary; correct/progress |
| warning | `#b26a00` | no explicit Stitch warning; use AccentColorPreset.Amber for real warnings |
| error | `#ba1a1a` | `error` |
| info | `#0059bb` | extracted secondary; evidence/retrieval/info |

### FOCUS_IMMERSION

| Token | Value | Source token / rationale |
|---|---:|---|
| background | `#0e0e0e` | vertical feed body / `surface-container-lowest` |
| surface | `#131313` | `surface`, `background`, `surface-dim` |
| surfaceVariant | `#353535` | `surface-variant`, highest panel |
| surfaceContainerLow | `#1b1b1b` | `surface-container-low` |
| surfaceContainerHigh | `#2a2a2a` | `surface-container-high` |
| primary | `#ffb1c3` | extracted neon primary; replaceable by accent preset |
| primaryContainer | `#ff4b89` | extracted strong CTA gradient endpoint |
| secondary | `#c8c6c5` | `secondary`; muted neutral |
| secondaryContainer | `#4a4949` | `secondary-container`; dark supporting panel |
| tertiary | `#00dbe9` | `tertiary`; success/new/info accent |
| textPrimary / onSurface | `#e2e2e2` | `on-surface`, `on-background` |
| textSecondary / onSurfaceVariant | `#e5bcc4` | `on-surface-variant`; pink-tinted secondary text |
| border / outline | `#ac878f` | `outline`; use at low opacity |
| outlineVariant | `#5c3f45` | ghost border at 20% opacity |
| success | `#00dbe9` | DESIGN.md explicitly allows tertiary for success/new badges |
| warning | `#ffb1c3` | no explicit amber; use low-emphasis primary or AccentColorPreset.Amber for due warnings |
| error | `#ffb4ab` | `error` |
| info | `#00dbe9` | extracted tertiary |

## Typography Tokens

| Theme | Headline font | Body font | Label font |
|---|---|---|---|
| STANDARD_STUDY | Manrope | Plus Jakarta Sans | Plus Jakarta Sans |
| ACTIVE_STUDY | Space Grotesk | Inter | Inter |
| FOCUS_IMMERSION | Space Grotesk | Inter | Inter |

Recommended Compose typography levels:

| Role | STANDARD_STUDY | ACTIVE_STUDY | FOCUS_IMMERSION |
|---|---|---|---|
| screen title | Manrope 44-56sp, 500-700, relaxed | Space Grotesk 34-48sp, 600-700 | Space Grotesk 44-60sp, 700 |
| section title | Manrope 22-28sp, 500-700 | Space Grotesk 22-28sp, 600-700 | Space Grotesk 24-30sp, 600-700 |
| card title | Plus Jakarta Sans or Manrope 18-24sp, 500-700 | Inter or Space Grotesk 18-24sp, 600 | Inter or Space Grotesk 18-24sp, 600 |
| body | Plus Jakarta Sans 14-16sp, 400-500 | Inter 14-16sp, 400-500 | Inter 14-16sp, 400-500 |
| caption | Plus Jakarta Sans 10-12sp, uppercase optional, letter spacing +0.08em | Inter 10-12sp, uppercase optional | Inter 10-12sp, medium |
| button label | Plus Jakarta Sans 14sp, 600-700 | Inter 14sp, 600-700 | Inter 14-16sp, 600-700 |

Do not scale type from viewport width. Use stable role sizes and responsive layout constraints.

## Shape Tokens

| Shape token | STANDARD_STUDY | ACTIVE_STUDY | FOCUS_IMMERSION |
|---|---:|---:|---:|
| card radius | 16dp default; 24dp for hero/large focus cards | 12dp default; 16dp for bento/hero cards | 16dp content cards; 24-32dp immersive focus panels |
| button radius | 999dp for primary pills; 16dp for square FAB | 8dp for primary action; 999dp for toggles/chips | 999dp for all main actions |
| pill radius | 999dp | 999dp | 999dp |
| modal radius | 24dp | 20-24dp | 24-32dp |

Conflict note: Standard DESIGN.md recommends 24dp hero cards while HTML frequently uses rounded-2xl (16dp). Active HTML uses rounded-xl/rounded-lg more often than the design prose. Focus DESIGN.md recommends very large rounded surfaces, while the HTML uses rounded-lg in list grids. Conservative ClassMate landing: use the lower value for dense learning lists and the higher value only for hero/focus surfaces.

## Component Rules

### Cards

| Theme | Rule |
|---|---|
| STANDARD_STUDY | Off-white base with white cards; use tonal separation, not hard dividers. Soft tinted shadow is allowed at very low opacity. |
| ACTIVE_STUDY | Surface-low cards for horizontal progress, white bento cards for priority actions, green/blue progress accents. |
| FOCUS_IMMERSION | Dark layered cards, glass panels, tonal separation. Avoid pure black text or hard white strokes. |

### Bottom Navigation

| Theme | Rule |
|---|---|
| STANDARD_STUDY | Floating translucent pill, `surface` about 70-80% opacity, blur about 20px, rounded full. Active item uses primary dot/glow. |
| ACTIVE_STUDY | Can be a low-height anchored or glass bar; active item uses primary color and small indicator. Keep layout functional. |
| FOCUS_IMMERSION | Floating glass pill over dark surface, `surfaceContainerLow` around 70% opacity, blur 20-30px, low-opacity outline. Active icon uses primary glow, not a solid tab. |

### Buttons

| Theme | Rule |
|---|---|
| STANDARD_STUDY | Primary is sage pill; secondary is tonal surface or warm callout. Avoid loud saturated CTAs. |
| ACTIVE_STUDY | Primary is compact green rectangle/rounded-lg; secondary may use blue. Good for "start practice" and progress action. |
| FOCUS_IMMERSION | Primary is full pill, accent gradient allowed; secondary is glass pill. Replace source pink with selected ClassMate accent when needed. |

### Search / Input

| Theme | Rule |
|---|---|
| STANDARD_STUDY | `surfaceContainerLow` fill, 16-24dp radius, no heavy border; focus shifts to `primaryFixed`/tinted surface. |
| ACTIVE_STUDY | `surfaceContainerLow` fill with ghost outline; focus may use 2dp primary stroke for clarity. |
| FOCUS_IMMERSION | `surfaceContainerHigh` fill, full pill, no visible box border; focus ring is low-opacity primary. |

### Icon Button

| Theme | Rule |
|---|---|
| STANDARD_STUDY | 40-56dp circular/squircle, primary icon on white or low surface. |
| ACTIVE_STUDY | 40dp circular, hover/pressed state shifts to surface container. |
| FOCUS_IMMERSION | 48-56dp circular glass button, low-opacity outline, primary/tertiary glow only on active state. |

### Status Pill

| Theme | Rule |
|---|---|
| STANDARD_STUDY | Soft warm or primary-fixed background, tiny uppercase label, restrained. |
| ACTIVE_STUDY | High-contrast progress pill using primary-fixed or secondary/blue for state. |
| FOCUS_IMMERSION | Full pill over dark surface; tertiary for success/new, primary only for main focus state. |

### Focus Mode Surface

| Theme | Rule |
|---|---|
| STANDARD_STUDY | Use large breathing card with white surface and very soft shadow. |
| ACTIVE_STUDY | Use progress dial or concise stats surface, not immersive media. |
| FOCUS_IMMERSION | Use full-screen dark layered surface with reduced distraction, ambient sound controls, and large typography. Do not copy vertical social feed behavior. |

## Theme Application Guidance

| Theme | Can absorb | Must not copy | ClassMate pages |
|---|---|---|---|
| STANDARD_STUDY | calm spacing, off-white surfaces, sage primary, floating nav, readable cards | source-domain content, source product names, unrelated metrics | Home, Course Detail, Ask, Settings, Export |
| ACTIVE_STUDY | progress rings, green/blue momentum, bento progress cards, compact action buttons | original source-domain logic and labels | Practice, Feedback, Weakness, Review momentum |
| FOCUS_IMMERSION | dark tonal hierarchy, glass nav, full-screen focus surface, ambient controls | source vertical-feed content, carousel behavior, default hot-pink dominance | Flow/Focus mode, ambient audio, night review |

## Implementation Guardrails

- This document is token guidance only; do not convert Stitch HTML directly to Compose.
- Keep the three preset mapping fixed.
- User-selected accent color may override accent tokens but must not flatten surface hierarchy.
- Keep text readable and layout stable across phone widths.
- Do not use source-domain labels or original business copy.
