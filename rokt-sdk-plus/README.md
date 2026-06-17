# Rokt SDK+ (Android)

Rokt SDK+ is a single umbrella artifact that bundles everything needed to run Rokt Shoppable
Ads through mParticle on Android:

- the mParticle core SDK (`com.mparticle:android-core`)
- the mParticle Rokt kit (`com.mparticle:android-rokt-kit`)
- the Rokt Payment Extension (`com.rokt:payment-extension`, which brings the Rokt SDK,
  Stripe, and Google Pay)

It is the Android counterpart of the iOS [`RoktSDKPlus`](https://github.com/mParticle/mparticle-apple-sdk/tree/main/Kits/rokt-sdk-plus/rokt-sdk-plus-ios)
umbrella. Add **one** dependency instead of wiring up core + kit + payment extension by hand,
with versions guaranteed to be compatible.

Maven coordinates: **`com.rokt:rokt-sdk-plus`**.

> The mParticle Rokt kit deliberately does **not** pull the payment extension (and Stripe)
> transitively, so apps that don't need Shoppable Ads stay lightweight. Rokt SDK+ is the
> artifact that adds it on top.

## Adding the dependency

```groovy
dependencies {
    implementation 'com.rokt:rokt-sdk-plus:6+'
}
```

This transitively provides `android-core`, `android-rokt-kit`, `com.rokt:roktsdk`,
`com.rokt:payment-extension`, Stripe, and Google Pay. You do not need to declare any of those
separately.

## Versioning

Rokt SDK+ tracks the mParticle Android SDK release line (it shares its version with
`android-core` and `android-rokt-kit`). The bundled Rokt artifacts (`roktsdk` /
`payment-extension`) ride their own Rokt release line; the pinned versions live in the repo's
root `gradle.properties` (`roktSdkVersion`, `roktPaymentExtensionVersion`).

## Usage

Initialize mParticle as usual:

```kotlin
import com.mparticle.MParticle
import com.mparticle.MParticleOptions

val options = MParticleOptions.builder(this)
    .credentials("<<<App Key>>>", "<<<App Secret>>>")
    .build()
MParticle.start(options)
```

Select Rokt placements through the mParticle Rokt kit facade:

```kotlin
import com.mparticle.kits.rokt

MParticle.getInstance()?.rokt?.selectPlacements(
    identifier = "RoktExperience",
    attributes = attributes,
)
```

### Shoppable Ads

Shoppable Ads use the payment extension bundled by this artifact. Register your payment
extension once (the Stripe publishable key is supplied from your Rokt kit settings in the
mParticle dashboard), then select shoppable placements:

```kotlin
MParticle.getInstance()?.rokt?.registerPaymentExtension()

MParticle.getInstance()?.rokt?.selectShoppableAds(
    identifier = "RoktExperience",
    attributes = attributes,
)
```

> The `registerPaymentExtension` / `selectShoppableAds` facade is provided by the mParticle
> Rokt kit. They require a kit version that includes Shoppable Ads support
> (see the kit [README](../kits/rokt/rokt/README.md)).

## Documentation

[Rokt Android integration guide](https://docs.rokt.com/developers/integration-guides/android/overview)

## License

[Rokt SDK License](https://rokt.com/sdk-license-2-0/)
