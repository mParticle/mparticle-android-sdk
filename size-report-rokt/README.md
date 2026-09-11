# Rokt SDK+ size report fixture

Measures what the Rokt kit, and the `com.rokt:rokt-sdk-plus` umbrella (which adds the payment
extension), add to a minified release APK. Results are posted to pull requests by
`.github/workflows/rokt-size-report.yml`.

This is a standalone Gradle build with its own wrapper and AGP, modelled on
`kits/rokt/rokt/example/example-kotlin`. It has to be: the kit plugin wires kits to module
coordinates rather than projects, and the Rokt SDK needs a newer AGP than the shared multi-kit
build, so the umbrella can only be measured against published artifacts. `scripts/rokt-size-report.sh`
publishes the chain to mavenLocal and then builds this fixture against it.

Three flavours of one throwaway app, differing only in dependencies so that one
`assembleRelease` produces every APK under one AGP/R8:

| Flavour    | Dependencies                       | Yields                                        |
| ---------- | ---------------------------------- | --------------------------------------------- |
| `baseline` | the reference app                  | the floor                                     |
| `kit`      | `+ com.mparticle:android-rokt-kit` | `kit - baseline` = Core + kit + Rokt SDK      |
| `sdkplus`  | `+ com.rokt:rokt-sdk-plus`         | `sdkplus - kit` = the payment extension alone |

## The reference app

The baseline is a small Jetpack Compose + Material3 screen, not an empty app. The Rokt kit is
Compose-based, so an empty baseline charged it for Compose that almost every partner app already
ships — around 1.2 MB of the reported figure.

**This dependency set is a convention, not a measured average.** Nobody surveyed real partner
apps. Treat the number as "what the kit costs an app that looks roughly like this", and do not
quote it to a partner as a typical or average figure.

- `androidx.compose:compose-bom` — `ui`, `material3`
- `androidx.activity:activity-compose`
- `androidx.lifecycle:lifecycle-runtime-compose`
- `androidx.appcompat:appcompat`
- `androidx.core:core-ktx`

The per-pull-request check in `size-report` deliberately keeps an **empty** baseline instead:
`android-core` ships no Compose and no resources, so there is nothing a host app would already
provide. The two numbers are produced by different builds and are not comparable with each other.

`ReferenceAppActivity` has to keep rendering real Compose. If it stops being reachable, R8
shrinks Compose out of the baseline, the baseline collapses towards an empty app, and every delta
silently reverts to charging the kit for Compose. `scripts/rokt-size-report.sh` asserts the
baseline stays above 768 KB to catch exactly that.

## Running it locally

```bash
scripts/rokt-size-report.sh .    # publishes the chain, builds, prints one line of JSON
```

`--skip-build` measures APKs that are already built. Expect the publish chain to take a few
minutes: it is three sequential Gradle invocations before this fixture is built at all.
