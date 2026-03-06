## Braze (formerly Appboy) Kit Integration

This repository contains the [Braze](https://www.braze.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Example App

This repository contains an [example app](https://github.com/mparticle-integrations/mparticle-android-integration-appboy/tree/master/example) showing how to implement mParticle, Braze, and Firebase Cloud Messaging. The key changes you need to make to your app are below, and please also reference mParticle and Braze's documentation:

- [Instrumenting Push](https://docs.mparticle.com/developers/sdk/android/push-notifications)
- [Braze Documentation](https://docs.mparticle.com/integrations/braze/event)

### 1. Adding the integration

[See a full build.gradle example here](https://github.com/mparticle-integrations/mparticle-android-integration-appboy/blob/master/example/build.gradle)

1. The Braze Kit requires that you add Braze's Maven server to your buildscript:

    ```
    repositories {
        maven { url "https://appboy.github.io/appboy-android-sdk/sdk" }
        //Braze's library depends on the Google Support Library
        google()
        ...
    }
    ```

2. Add the kit dependency to your app's `build.gradle`:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-appboy-kit:5+'
    }
    ```

### 2. Registering for Push

mParticle's SDK takes care of registering for push notifications and passing tokens or instance IDs to the Braze SDK. [Follow the mParticle push notification documentation](https://docs.mparticle.com/developers/sdk/android/push-notifications#register-for-push-notifications) to instrument the SDK for push registration. You can skip over [this section of Braze's documentation](https://www.braze.com/docs/developer_guide/platform_integration_guides/android/push_notifications/integration/#registering-for-push).

### 3. Displaying Push

[See a full example of an AndroidManifest.xml here](https://github.com/mparticle-integrations/mparticle-android-integration-appboy/blob/master/example/src/main/AndroidManifest.xml).

mParticle's SDK also takes care of capturing incoming push notifications and passing the resulting `Intent` to Braze's `BrazePushReceiver`. Follow the [mParticle push notification documentation](https://docs.mparticle.com/developers/sdk/android/push-notifications#display-push-notifications) to ensure you add the correct services and receivers to your app's AndroidManifest.xml.

### 4. Reacting to Push and Deeplinking

There are a wide variety of implementation options available in Braze to deeplink a user when they tap a notification. There are **two specific requirements** to ensure automatic deeplinking works as intended.

- `BrazePushReceiver`

    Whereas up until now you should have nothing Braze-specific in your `AndroidManifest.xml`, using Braze's automatic deeplinking does require you to add their `BrazePushReceiver`. Note that you do not need to specify any Intent filters (for example to receive push tokens, since mParticle takes care of that). You just need to add the following:

        <receiver android:name="com.braze.push.BrazePushReceiver" />
    
- `braze.xml`

    For automatic deep-linking, you need to add a boolean resource named `com_braze_handle_push_deep_links_automatically`. This can be in any resource file, or you can name it `braze.xml`:

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <bool name="com_braze_handle_push_deep_links_automatically">true</bool>
    </resources>
    ```


From here you should be able to successfully test push via Braze! Braze offers many client-side configurable options via xml resources and otherwise. Please see review the rest of [their documentation here](https://www.braze.com/docs/developer_guide/platform_integration_guides/android/push_notifications/integration/#step-3-add-deep-links) for more information.

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
