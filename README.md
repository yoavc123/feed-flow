<div align="center">
  <img style="border-radius: 50%" src="assets/logo.png" width="100" alt="Flow RSS app icon">
  <h1>Flow RSS</h1>
  <p><strong>Minimal, fast RSS reading without the clutter.</strong></p>
  <p>
    Flow RSS is a calm RSS reader for Android phones, tablets, and foldables.
    It uses an Android-only Kotlin Multiplatform build and Compose UI.
  </p>
  <p>
    It focuses on a time-bounded Flow, flexible reading modes, and control over sync and storage.
  </p>
  <p>
    <a href="https://www.feedflow.dev">Website</a>
    ·
    <a href="https://github.com/prof18/feed-flow/releases/latest">Latest release</a>
    ·
    <a href="https://github.com/prof18/feed-flow/issues">Issues</a>
    ·
    <a href="https://hosted.weblate.org/engage/feedflow/">Translate</a>
  </p>
  <p>
    <img alt="GitHub Release" src="https://img.shields.io/github/v/release/prof18/feed-flow?display_name=release">
    <img alt="License" src="https://img.shields.io/github/license/prof18/feed-flow">
    <img alt="Platform" src="https://img.shields.io/badge/platform-Android-2ea44f">
  </p>
</div>

![FeedFlow banner](assets/banners.png)

## Why FeedFlow

- Read articles the way you want: Reader Mode, the in-app browser, or your preferred browser
- Keep your library local, sync it through cloud storage, or connect directly to reader services
- Organize a busy timeline with bookmarks, filters, and blocked words
- Move data in and out easily with OPML feed import/export and CSV article import/export

## Highlights

- A time-bounded Flow with Flash, Daily, Standard, Slow, and Timeless paces
- Local-only Let Go with undo, without mutating remote read state
- Flexible reading modes: Reader Mode, the in-app browser, or your preferred browser
- Flexible sync and storage options: local library, Dropbox, Google Drive, FreshRSS, Miniflux, Feedbin, and BazQux Reader
- Offline reading by saving article content during sync
- Flow, Saved, source, and stream views
- Blocked words to hide articles containing specific keywords or phrases
- Curated feed suggestions across different topics
- Theme modes for system, light, dark, and OLED
- Android home-screen widgets

## Download

| Platform | Get FeedFlow |
| --- | --- |
| Android | [Google Play](https://play.google.com/store/apps/details?id=com.prof18.feedflow) or [F-Droid](https://f-droid.org/packages/com.prof18.feedflow) |

## What You Can Do With FeedFlow

### Read Your Way

FeedFlow does not lock you into a single article view. Open links in Reader Mode, use the in-app browser,
or send them to your preferred browser. Reader Mode also includes extras like opening comments directly
and sharing articles without leaving your reading flow.

### Keep Control Over Sync and Storage

You can keep everything local, use storage backends like Dropbox or Google Drive,
or connect directly to reader services such as FreshRSS, Miniflux, Feedbin, and BazQux Reader.

### Let a Busy Flow Move On

Choose how quickly articles leave your Flow, save what matters, and Let Go of the rest without creating unread debt.
FeedFlow also supports auto-saving article content for offline reading and includes cache cleanup tools.

### Discover New Feeds Faster

Feed suggestions are built into the app, with curated sources across ten categories, so a fresh install
does not have to start from zero.

## Building From Source

### Prepare Local Config

Android:

```bash
cp config/dummy-google-services.json androidApp/src/debug/google-services.json
cp config/dummy-google-services.json androidApp/src/release/google-services.json
```

Optional local keys:

- `keystore.properties` for the Android Dropbox key

## Tech Stack

- Kotlin Multiplatform for shared business logic
- Compose Multiplatform for Android UI
- SQLDelight-backed local storage
- [RSSParser](https://github.com/prof18/RSS-Parser) for feed parsing

## Translating

If you want to help translate FeedFlow, use [Weblate](https://hosted.weblate.org/engage/feedflow/)
or open a pull request with:

- a new `strings.xml` file under `i18n/src/commonMain/resources/locale/values-<language-code>/`
- matching store copy under `assets/storecopy/<language-code>/`

<div align="center">
  <a href="https://hosted.weblate.org/engage/feedflow/">
    <img src="https://hosted.weblate.org/widget/feedflow/287x66-grey.png" alt="Translation status">
  </a>
</div>

## Contributing

Issues and pull requests are welcome. If you are proposing a larger feature or a platform-specific change,
opening an issue first is usually the fastest way to align on scope.

## License

FeedFlow is released under the [Apache 2.0 License](LICENSE).
