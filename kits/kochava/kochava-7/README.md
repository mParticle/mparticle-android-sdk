# Kochava Kit Integration

This directory contains the [Kochava](https://www.kochava.com) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk), built against Kochava SDK 7.x. It requires Android API level 21 or later.

## Adding the integration

1. Add the Kochava Maven repository and the kit dependency to your app's build.gradle. Kochava SDK 7 is published to Kochava's repository, not Maven Central:

    ```groovy
    repositories {
        exclusiveContent {
            forRepository {
                maven { url 'https://maven.sdk.kochava.com' }
            }
            filter {
                includeGroup 'com.kochava.measurement'
            }
        }
    }

    dependencies {
        implementation 'com.mparticle:kochava-7:6.0.0'
    }
    ```

2. Add the following dependencies to enable various Kochava capabilities.

    ```groovy
    dependencies {
        //Required: Google Play Services Ads Identifier (If publishing to the Google Play Store)
        implementation 'com.google.android.gms:play-services-ads-identifier:15.0.1'

        //Required: Install Referrer 2.2 or later (If publishing to the Google Play Store)
        implementation 'com.android.installreferrer:installreferrer:2.2'

        //Optional: Location Collection. Note: This feature must also be enabled server side before collection will occur.
        implementation 'com.google.android.gms:play-services-location:15.0.1'
    }
    ```

    > [Kochava SDK documentation](https://support.kochava.com/sdk-integration/android-sdk-integration/)

3. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Kochava detected"` in the output of `adb logcat`.
4. Reference mParticle's integration docs below to enable the integration.

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
