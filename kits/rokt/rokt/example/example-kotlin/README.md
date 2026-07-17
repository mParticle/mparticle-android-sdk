# Rokt Kit Kotlin Example

Standalone sample app for exercising the Rokt Kit through the mParticle SDK
(identify, user attributes, overlay and embedded placements, event stream).

This module is built standalone (own Gradle wrapper and settings) because the
Rokt SDK requires a newer AGP than the shared multi-kit build. It resolves
published `com.mparticle` artifacts instead of the in-repo kit project.

## Setup

1. Set the mParticle credentials: either replace the `DEFAULT_API_KEY` /
   `DEFAULT_API_SECRET` placeholders in `ExampleApplication.kt`, or enter them
   in the input fields at the top of the app and tap "Apply credentials"
   (switches the workspace at runtime via `MParticle.switchWorkspace`, no
   restart needed).
2. Replace the placement identifier placeholders in `MainActivity.kt`
   (`REPLACE_WITH_OVERLAY_VIEW_NAME`, `REPLACE_WITH_BOTTOM_SHEET_VIEW_NAME`,
   `REPLACE_WITH_EMBEDDED_VIEW_NAME`) and, if needed, the embedded placeholder
   name (`Location1`).
3. Build and install:

    ```bash
    ./gradlew installDebug
    ```

The Rokt account id and the Stripe publishable key are not configured in the
app: both arrive with the kit configuration for the selected workspace.

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
- "Apply credentials" switches to another workspace at runtime and the kit
  re-initializes with that workspace's Rokt account.
- "Show overlay placement" and "Show bottom sheet placement" trigger
  `selectPlacements` for overlay-style layouts.
- "Show embedded placement" passes a `RoktEmbeddedView` placeholder; the
  height callback is logged.
- Placement events are collected from the kit's event flow and logged with
  the `RoktKitExample` tag.
- "Close placements" dismisses any active overlay placement.

Debug builds trust user-installed CA certificates
(`network_security_config.xml`), so HTTPS traffic can be inspected with tools
like Proxyman or Charles.
