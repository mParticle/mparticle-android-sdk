## Kochava Kit Integration

This repository contains the [Kochava](https://www.kochava.com) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

    ```
1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-kochava-kit:5+'
    }
    ```
2. Add the following dependencies to enable various Kochava capabilites.

    >
    > NOTE: Starting with Play Services 15 the dependency requirements have changed. The Android Advertising ID (adid) collection has moved out of play-services “base” and into “ads-identifier”. Use the appropriate option for the version of Play Services you are using as indicated below.
    >
    > ```
    > dependencies {
    >
    >      //Required: Google Play Services Ads Identifier (If publishing to the Google Play Store)
    >      implementation 'com.google.android.gms:play-services-ads-identifier:15.0.1'
    >      //Required: If using Play Services prior to version 15.
    >      //implementation 'com.google.android.gms:play-services-base:12.0.1'
    >
    >      //Required: Install Referrer (If publishing to the Google Play Store)
    >      implementation 'com.android.installreferrer:installreferrer:1+'
    >
    >      //Optional: Location Collection. Note: This feature must also be enabled server side before collection will occur.
    >      implementation 'com.google.android.gms:play-services-location:15.0.1'
    >
    >      //Optional: Instant App Status Collection
    >      implementation 'com.google.android.instantapps:instantapps:1.1.0'
    >  }
    > ```
    >  https://support.kochava.com/sdk-integration/sdk-kochavatracker-android/_


3. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Kochava detected"` in the output of `adb logcat`.
4. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Kochava integration](https://docs.mparticle.com/integrations/kochava/event/)

### IdentityLink

If you would like to associate the Account Identity with a custom Identifier that is not the Device Identity, you can include the data in the Kochava kit's initialization, by calling `KochavaKit.setIdentityLink` before `MParticle.start()`

```
Map<String, String> identityLink = new HashMap<String, String>();
identityLink.put("key1", "identity1");
identityLink.put("key2", "identity2");
KochavaKit.setIdentityLink(identityLink);
```

### Attribution, Deeplinking and Enhanced Deeplinking results

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

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
