# Changelog

## [Unreleased]

## [0.1.2]
### Added
- Android Studio (Meerkat) compatibility via `pluginVerification`
- Automated `verifyPlugin` CI check on every push and PR
- `since-build` declared as `232` (IntelliJ 2023.2 minimum)
- Journeys list in the dropdown is now sorted lexicographically

### Changed
- Renamed plugin, project, and directory from `JourneyKMPVisualizer` to `JourneyKMPGraph`
- Replaced `JComboBox` with `ComboBox`, `JScrollPane` with `JBScrollPane`, and `Insets` with `JBUI.insets` for better IDE integration
- PSI scanner now uses targeted `try-catch` that correctly rethrows `ProcessCanceledException`
- Annotations per step parsed once and reused for both node building and edge extraction

### Fixed
- Resolved redundant `runCatching` layers in PSI scanner and window refresh
- Fixed `BufferedImage` usage replaced with `UIUtil.createImage()` for HiDPI support
- Suppressed incorrect `UseJBColor` warnings for canvas-theme-driven colors

## [0.1.1]
### Fixed
- Added `com.intellij.modules.platform` dependency to support all JetBrains IDEs

## [0.1.0]
### Added
- Initial release
- Interactive graph visualization for `@Journey`-annotated navigation flows
- Horizontal and vertical layout modes
- Zoom in/out, fit to view, and reset zoom
- Draggable nodes on the canvas
- Piggyback tag visualization (ON_ENTER / ON_EXIT)
- Export as Mermaid diagram or PNG
- Light and dark theme support
