# Kochava Kit Integration

This directory contains the [Kochava](https://www.kochava.com) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

This kit targets **Kochava Android SDK 7.x** (`com.kochava.measurement:base:7.0.0`, `com.kochava.measurement:google:7.0.0`, and `com.kochava.measurement:googletracking:7.0.0`). For Kochava 5.x, use the `kochava-5` kit instead.
Kochava 7.x requires `minSdk 21`.

## Adding the integration

1. Add the kit dependency to your app's `build.gradle`:

    ```groovy
    dependencies {
        implementation 'com.mparticle:kochava-7:6+'
    }
    ```

2. Add the following dependencies to enable various Kochava capabilities.

    > NOTE: Starting with Play Services 15 the dependency requirements have changed. The Android Advertising ID (adid) collection has moved out of play-services “base” and into “ads-identifier”. Use the appropriate option for the version of Play Services you are using as indicated below.

    ```groovy
    dependencies {
        //Required: Google Play Services Ads Identifier (If publishing to the Google Play Store)
        implementation 'com.google.android.gms:play-services-ads-identifier:15.0.1'
        //Required: If using Play Services prior to version 15.
        //implementation 'com.google.android.gms:play-services-base:12.0.1'

        //Required: Install Referrer 2.0+ (If publishing to the Google Play Store)
        implementation 'com.android.installreferrer:installreferrer:2.2'

        //Optional: Google Play Services App Set ID
        implementation 'com.google.android.gms:play-services-appset:16.0.2'

        //Optional: Location Collection. Note: This feature must also be enabled server side before collection will occur.
        implementation 'com.google.android.gms:play-services-location:15.0.1'

        //Optional: Instant App Status Collection
        implementation 'com.google.android.instantapps:instantapps:1.1.0'
    }
    ```

    > For apps distributed on Huawei, Samsung, Meta, or Amazon app stores, add the corresponding Kochava 7 store modules (`com.kochava.measurement:huawei:7.0.0`, `com.kochava.measurement:samsung:7.0.0`, `com.kochava.measurement:meta:7.0.0`, or `com.kochava.measurement:amazon:7.0.0`, plus their `*tracking:7.0.0` modules as needed) from Maven Central.
    >
    > [Kochava SDK documentation](https://support.kochava.com/sdk-integration/sdk-kochavatracker-android/_)

3. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Kochava detected"` in the output of `adb logcat`.
4. Reference mParticle's integration docs below to enable the integration.

## Migrating from the `kochava-5` kit

- **`minSdk` is now 21** (Kochava 7.x raised its minimum API level from 16 to 21).
- **Updated modules**: `com.kochava.tracker:*` modules (`tracker`, `events`, `datapointnetwork`, `engagement`, `legacyreferrer`) are replaced by `com.kochava.measurement:base:7.0.0`, `com.kochava.measurement:google:7.0.0` (Google Play Install Referrer and App Set ID; requires `com.android.installreferrer:installreferrer:2.0+`), and `com.kochava.measurement:googletracking:7.0.0` (Google Advertising ID), all published on Maven Central, and `com.kochava.tracker.Tracker` moved to `com.kochava.measurement.base.Measurement`. Store collectors for Huawei, Samsung, Meta, and Amazon moved into separate `huawei`, `samsung`, `meta`, and `amazon` (plus `*tracking`) modules.
- **Automatic deeplink event**: `processDeeplink(..., true)` makes Kochava 7 queue an automatic deeplink event (`EventType.DEEPLINK` with `_auto`) whenever the app starts from a URI, whereas the Kochava 5.7.1 standard-deeplink job sent no event. Apps that already send their own deeplink event will double-count after switching unless updated.
- **Attribution on every start**: `kochava-5` skipped retrieval once `installAttribution.isRetrieved` was `true`. `kochava-7` calls `retrieveInstallAttribution` on every kit start and Kochava returns the cached value, so `AttributionListener.onResult` with `attribution` now fires on every app start.

## Documentation

[Kochava integration](https://docs.mparticle.com/integrations/kochava/event/)

## IdentityLink

If you would like to associate the Account Identity with a custom Identifier that is not the Device Identity, you can include the data in the Kochava kit's initialization, by calling `KochavaKit.setIdentityLink` before `MParticle.start()`

```java
Map<String, String> identityLink = new HashMap<String, String>();
identityLink.put("key1", "identity1");
identityLink.put("key2", "identity2");
KochavaKit.setIdentityLink(identityLink);
```

## Attribution, Deeplinking and Enhanced Deeplinking results

Kochava offers a number of APIs to process attribution and deeplinking data. In our abstraction, the
results from these are all routed to our `AttributionListener` under distinct, constant keys.

```kotlin
val attributionListener = object: AttributionListener {
    override fun onResult(result: AttributionResult) {
        when (result.serviceProviderId) {
            MParticle.ServiceProviders.KOCHAVA -> {
                val parameters = result.parameters ?: JSONObject()

                //process Attribution results
                if (parameters.has(KochavaKit.ATTRIBUTION_PARAMETERS)) {
                    val attributionParamters =
                        parameters.getJSONObject(KochavaKit.ATTRIBUTION_PARAMETERS)
                }

                //process Deeplink results
                if (parameters.has(KochavaKit.DEEPLINK_PARAMETERS)) {
                    val deeplinkParameters =
                        parameters.getJSONObject(KochavaKit.DEEPLINK_PARAMETERS)
                }

                //process Enhanced Deeplink results
                if (parameters.has(KochavaKit.ENHANCED_DEEPLINK_PARAMETERS)) {
                    val enhancedDeeplinkParameters =
                        parameters.getJSONObject(KochavaKit.ENHANCED_DEEPLINK_PARAMETERS)
                }
            }
        }
    }

    override fun onError(error: AttributionError) {
        //error handling
    }
}

MParticle.start(
    MParticleOptions.builder(this)
        .attributionListener(attributionListener)
        .build()
)

```

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
