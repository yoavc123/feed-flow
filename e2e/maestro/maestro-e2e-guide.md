# Android Maestro E2E Guide

FeedFlow's UI suite is Android-only. Flows live under `e2e/maestro/android/{smoke,regression}` and use the debug application ID `com.prof18.feedflow.debug`.

## Deterministic setup

Every flow must reset or seed through the debug-only deep link and must not depend on live feeds, OAuth, or prior app state:

```yaml
appId: com.prof18.feedflow.debug
---
- launchApp
- openLink: feedflow://e2e/reset-and-seed?profile=content-rich
- assertVisible: "E2E seed complete"
- tapOn:
    id: e2e_open_app
```

Available profiles include `empty`, `content-rich`, `card-layout`, `big-image-layout`, `grid-layout`, `compact-list`, `reader-mode`, `external-browser`, `notifications`, `sync-linked-mock`, `large-content`, `pagination-scroll-read`, `reorder-drag`, `feed-content`, `rtl-content`, `swipe-actions`, and `swipe-disabled`.

Fixtures are stored under `e2e/fixtures/` and copied to `/sdcard/Download/feedflow-e2e` by `e2e/scripts/push-android-fixtures.sh`.

## Authoring rules

- Prefer stable accessibility IDs over visible labels.
- Keep one behavior per flow.
- Use seeded titles only when the content is deliberately stable.
- Compose popups need `Modifier.exposeTestTagsAsResourceIds()` before their child tags are visible to Maestro.
- Use `back` for Android system-back coverage and explicit back buttons when the button itself is under test.
- User-visible changes need an Android flow unless the state is inaccessible without production-only hooks; document such cases in the catalog.

## Running

```bash
# Full Android release suite
e2e/scripts/run-android.sh

# Fast smoke suite
e2e/scripts/run-android-smoke.sh

# One flow against an installed debug build
maestro --platform android test e2e/maestro/android/regression/165-focus-history-progress.yaml
```

The wrappers build and install the Google Play debug APK, push fixtures, then run each YAML flow sequentially.

## Android CLI validation

Use `android layout --pretty` for semantics/state and `android screen capture --output=ui.png --annotate` for visual validation. Test the resizable emulator in phone, foldable, and tablet modes with:

```bash
adb emu resize-display 0
adb emu resize-display 1
adb emu resize-display 2
```
