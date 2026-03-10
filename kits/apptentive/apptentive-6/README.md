## Apptentive Kit Integration

This repository contains the [Apptentive](https://www.apptentive.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-apptentive-kit:5+'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Apptentive detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.
4. Reference Apptentive's integration doc below for more features.
5. Check out the example app below.

### Documentation

[Apptentive integration](https://docs.mparticle.com/integrations/apptentive/event/)

[Apptentive-mParticle Integration](https://learn.apptentive.com/knowledge-base/mparticle-integration-android)

### Example App

[Apptentive Android mParticle Example](https://github.com/apptentive/android-mparticle-example)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
