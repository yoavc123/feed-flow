# Android Maestro Coverage

Run smoke with `e2e/scripts/run-android-smoke.sh` and the complete release suite with `e2e/scripts/run-android.sh`.

## Calm Flow acceptance coverage

| Behavior | Flow |
| --- | --- |
| Flow seed loading and hidden-source filtering | `smoke/002-seeded-timeline-loads.yaml` |
| Flow, Saved, Voices, Streams, and Source filters | `smoke/003-library-filters.yaml` |
| Save and offline reader entry | `smoke/004-article-read-bookmark-state.yaml` |
| Search and reading history | `smoke/006-search-core.yaml`, `regression/165-focus-history-progress.yaml` |
| Reader progress and Focus deck | `regression/165-focus-history-progress.yaml` |
| Source pace, Voice, webcomic, mute, and rate limit | `smoke/008-feed-edit-core.yaml`, `regression/166-source-calm-tuning.yaml` |
| Let Go, undo, and local action set | `regression/106-article-context-menu.yaml`, `regression/143-article-context-menu-action-set.yaml` |
| Coaching insight placement and source-tuning entry | `regression/167-coaching-card-placement.yaml` |
| Existing two-direction swipe compatibility | `regression/107-swipe-actions.yaml` |
| Source pace persists and applies in Flow and Streams | `regression/164-scoped-flow-pace.yaml` |
| Curated palettes, reduce motion, insights controls | `regression/114-appearance-settings.yaml` |
| Responsive feed layouts | `regression/108-feed-layout-matrix-*.yaml` |
| RTL and pagination | `regression/162-pagination-scroll-read.yaml`, `regression/163-rtl-content-direction.yaml` |

The remaining numbered Android flows cover feed/category management, sync providers, import/export, notifications, reader fallbacks, search result actions, settings persistence, deep links, widgets, and accessibility-oriented content direction.

## Known limitations

- Swipe expansion beyond the existing left/right actions is intentionally pending a compatibility decision; the current swipe behavior is preserved and covered by REG-107.
- Real provider authentication, OS browser destinations, background notification delivery, launcher widgets, and the OS review prompt require external state and remain manual validation.
- Exact visual styling, TalkBack traversal, large-font clipping, contrast, and phone/foldable/tablet screenshots require Android CLI or device validation in addition to Maestro semantics.
- Baseline-profile generation requires a connected API 33+ device or emulator.
