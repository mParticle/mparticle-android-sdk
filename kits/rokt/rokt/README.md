# Rokt Kit Integration

This repository contains the [Rokt](https://docs.rokt.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

## Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-rokt-kit:6+'
    }
    ```

2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Rokt detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

## Usage

Kotlin consumers can access the Rokt Kit facade from the mParticle instance:

```kotlin
import com.mparticle.MParticle
import com.mparticle.kits.rokt

MParticle.getInstance()?.rokt?.selectPlacements(
    identifier = "RoktExperience",
    attributes = attributes,
)
```

Java consumers can use the kit helper:

```java
MParticleRokt.Rokt().selectPlacements("RoktExperience", attributes);
```

Compose integrations can receive native Rokt SDK events from `RoktLayout`:

```kotlin
RoktLayout(
    sdkTriggered = true,
    identifier = "RoktExperience",
    attributes = attributes,
    location = "RoktEmbedded1",
    onEvent = { event ->
        // Handle RoktEvent
    },
)
```

## Documentation

[Rokt integration](https://docs.rokt.com/developers/integration-guides/android/overview)

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
