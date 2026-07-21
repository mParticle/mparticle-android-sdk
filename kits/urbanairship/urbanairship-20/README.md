## Urban Airship Kit Integration

This repository contains the [Urban Airship](https://www.urbanairship.com) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-urbanairship-kit:5+'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Urban Airship detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.
4. If you wish to utilize Urban Airship's Push Messaging capabilities, please refer to the Push Message Considerations section below

## Tag-Based Segmentation

All mParticle user attributes are forwarded to Airship as [tags](https://docs.airship.com/platform/android/segmentation/) which can be used to identify and segment your audience.

Most clients prefer for all tags to remain constant if set. But, a tag can be removed manually by invoking removeTag directly on the Airship SDK as shown bellow.

#### Java
```java
    UAirship.shared().getChannel().editTags()
    .removeTag("some_tag")
    .apply();
```

#### Kotlin
```kotlin
    UAirship.shared().getChannel().editTags()
    .removeTag("some_tag")
    .apply()
```

### Documentation

[Urban Airship integration](https://docs.mparticle.com/integrations/airship/event/)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
