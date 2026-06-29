> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# ClassMate Theme Engine v1 Implementation

Date: 2026-06-19

Scope: controlled Compose theme-system implementation from the Stitch token extraction docs. This is not a full page rewrite.

## Fixed Mapping

| Stitch source | ThemePreset | Student-facing label |
|---|---|---|
| Dashboard | `STANDARD_STUDY` | 默认学习 |
| Main Dashboard | `ACTIVE_STUDY` | 活力学习 |
| Vertical Feed | `FOCUS_IMMERSION` | 沉浸学习 |

No fourth theme was added.

## Implemented Tokens

- `ThemePreset`: `STANDARD_STUDY`, `ACTIVE_STUDY`, `FOCUS_IMMERSION`.
- `AccentColorPreset`: Blue, Cyan, Green, Purple, Amber, Rose, Graphite, Ocean.
- `ClassMateColorScheme`: background, surface hierarchy, primary/accent, secondary, tertiary, text, border, success, warning, error, info, focus surface, evidence surface.
- `ClassMateShapeScheme`: card, button, pill, modal, and focus-panel radii from the extraction report.
- `ClassMateSpacing`: stable spacing local for future component migration.

Focus Immersion keeps the dark surface hierarchy from Vertical Feed, but the source pink is not a fixed app primary. The selected `AccentColorPreset` controls focus primary and selected states.

## Product Wiring

- App root `ClassMateTheme` now receives the persisted theme preset and accent color.
- Settings → 外观与主题 shows three theme cards with background, surface, accent bar, and current selection.
- Settings shows an Accent Color swatch section with the required eight presets.
- Theme and accent preferences persist in app-private storage in a file separate from AI model configuration.
- Shared card, primary/secondary buttons, status chips, bottom navigation, and the Flow scene card now use theme tokens.

## Boundary

This pass did not rewrite Home, Ask, Practice, Review, Import, Export, provider logic, navigation, Gradle, `.github`, `.codex_work`, or local config files.
