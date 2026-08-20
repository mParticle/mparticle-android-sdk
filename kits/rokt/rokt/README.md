# Rokt Kit Integration

This directory contains the [Rokt](https://docs.rokt.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

## Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-rokt-kit:6.0.0'
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

### Shoppable Ads

Add the optional Rokt payment extension dependency in your app, then register the extension after mParticle starts. The Rokt kit reads `stripePublishableKey` from the mParticle dashboard configuration and forwards it to the Rokt SDK during registration.

Kotlin:

```kotlin
import com.mparticle.MParticle
import com.mparticle.kits.rokt
import com.rokt.payment.extension.StripePaymentExtension

MParticle.getInstance()?.rokt?.registerPaymentExtension(StripePaymentExtension())
MParticle.getInstance()?.rokt?.selectShoppableAds(
    identifier = "RoktShoppableExperience",
    attributes = attributes,
)
```

Java:

```java
MParticleRokt.Rokt().registerPaymentExtension(new StripePaymentExtension());
MParticleRokt.Rokt().selectShoppableAds("RoktShoppableExperience", attributes);
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

## Session management

Rokt sessions are managed automatically. Placements shown to the same user share one session, and **the kit ends the Rokt session whenever the mParticle user changes**:

| Identity transition | Rokt session |
|---|---|
| Anonymous user is identified (e.g. unknown on the payment page, known on the confirmation page) | **Kept** — same person, in-session state survives |
| A different user identifies or logs in | **Ended** — the next placement starts a new session |
| The current user logs out | **Ended** |

No integration code is needed for this behaviour.

### Self-service terminals (kiosks, shared devices)

Where a queue of unrelated customers uses one device, the recommended pattern is to **log the user out (or identify the next customer) between transactions** — the kit resets the Rokt session at that boundary, so each customer's placements and events land on their own session.

A manual reset is also available for explicit control, called on the Rokt SDK directly:

```kotlin
import com.rokt.roktsdk.Rokt

Rokt.clearSession()
```

Notes:

- The new session begins on the **next** `selectPlacements` call; `clearSession` only ends the current one.
- Calling `clearSession` with no active session (or before init) is a no-op, and it is safe to combine with the automatic behaviour — both converge on the same idempotent reset.
- Avoid enabling Rokt experience caching on shared terminals: a cached experience belongs to the customer it was fetched for.

## Documentation

[Rokt integration](https://docs.rokt.com/developers/integration-guides/android/overview)

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
