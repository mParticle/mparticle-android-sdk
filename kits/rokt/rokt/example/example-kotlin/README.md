# Rokt Kit Kotlin Example

Standalone sample app for exercising the Rokt Kit through the mParticle SDK
(identify, user attributes, overlay and embedded placements, event stream).

This module is built standalone (own Gradle wrapper and settings) because the
Rokt SDK requires a newer AGP than the shared multi-kit build. It resolves
published `com.mparticle` artifacts instead of the in-repo kit project.

## Setup

1. Replace the API key and secret placeholders in `ExampleApplication.kt`.
2. Replace the placement identifier placeholders in `MainActivity.kt`
   (`REPLACE_WITH_OVERLAY_VIEW_NAME`, `REPLACE_WITH_EMBEDDED_VIEW_NAME`) and,
   if needed, the embedded placeholder name (`Location1`).
3. Build and install:

    ```bash
    ./gradlew installDebug
    ```

By default the app uses the latest published release candidate
(`6.0.0-rc.1`) of `android-core` and `android-rokt-kit` from Maven Central.

## Joint testing against local builds

To test unreleased SDK + kit changes end to end, publish them to Maven Local
from the repository root and point the example at the local version (`0.0.0`):

```bash
# from the repository root
./gradlew publishMavenPublicationToMavenLocal
./gradlew -c settings-kits.gradle :kits:android-rokt:rokt:publishMavenPublicationToMavenLocal \
  -Pmparticle.kit.mparticleFromMavenLocalOnly=true

# from this directory
./gradlew installDebug -PmparticleVersion=0.0.0
```

Maven Local is resolved first, so locally published `com.rokt` artifacts
(e.g. a locally built `roktsdk` or `roktux`) also take precedence over Maven
Central when present.

## What to verify

- Identify runs at startup (`ExampleApplication`) with a test email and
  customer id; user attributes are set once the identity resolves.
- "Show overlay placement" triggers `selectPlacements` for an overlay layout.
- "Show embedded placement" passes a `RoktEmbeddedView` placeholder; the
  height callback is logged.
- Placement events are collected from the kit's event flow and logged with
  the `RoktKitExample` tag.
- "Close placements" dismisses any active overlay placement.
