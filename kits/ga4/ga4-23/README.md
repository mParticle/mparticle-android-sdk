## Firebase for Google Analytics 4 (GA4) Kit Integration

This repository contains the [Firebase for GA4](https://firebase.google.com/docs/analytics/get-started?platform=android) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-googleanalyticsfirebasega4-kit:5+'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"GA4 for Firebase detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Firebase for GA4 integration](http://docs.mparticle.com/integrations/google-analytics-4/event/)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
