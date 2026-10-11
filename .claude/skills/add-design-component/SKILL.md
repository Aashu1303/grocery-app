---
name: add-design-component
description: Add or change a component in core/designsystem with tokens, catalog entries for every state, screenshots and accessibility checks. Use when a screen needs UI that the design system does not have yet.
---

# Add a design-system component

Features may only use design-system components (`verifyFeatureSources`). Anything new goes here first.

1. **Check tokens.** Use `AppTheme.colors`, `.type`, `.spacing`, `.shapes`, `.motion`. If a value is missing, add a
   semantic token (named by meaning, not hue) to `theme/` or `tokens/`, for light **and** dark. New text/background
   pairs must be added to `ContrastTest` (WCAG AA 4.5:1 text, 3:1 non-text).
2. **Write the component** in `core/designsystem/src/commonMain/kotlin/app/rasoi/core/designsystem/component/`:
   - One public composable per file, named for what it is (`ExpiryChip`), plus small enums/data classes in their own
     files.
   - Stateless: values in, callbacks out. `modifier: Modifier = Modifier` is the first optional parameter.
   - Touch targets ≥ `AppTheme.spacing.minTouchTarget` (48 dp). Meaning never by colour alone; add text or an icon.
   - Content descriptions for icons, merged semantics for compound rows, `stateDescription` for selection.
   - User-visible text comes in as parameters or from `composeResources/values/strings.xml` (`ds_` prefix).
   - Material 3 is allowed here (and only here), always with Rasoi colours passed explicitly.
3. **Catalog.** Add one `CatalogEntry` per state to `catalog/CatalogEntries.kt` (enabled, disabled, loading,
   selected, error and so on). Add the component name to `CatalogCoverageTest`.
4. **Screenshots.** `CatalogScreenshotTest` renders every entry in light and dark. Re-record goldens by pushing a commit whose message contains
   `[record screenshots]` (or running `./gradlew recordRoborazziJvm` where Compose builds), and review every changed
   PNG in the diff.
5. **Pure logic** (formatting, initials, mapping) gets a unit test in `src/jvmTest`.
6. `./gradlew check` (or the lint-only command from AGENTS.md on restricted machines), then `pre-commit-review`.
