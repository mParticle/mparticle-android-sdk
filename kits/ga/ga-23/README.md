> Google launched Google Analytics 4, a new way of analyzing iOS, Android, and Web data together.  Google encourages all customers to [upgrade their Firebase instances](https://support.google.com/analytics/answer/9379599) so that Firebase data flows to Google Analytics 4.  mParticle encourages all customers to also upgrade to the [new mParticle Android Firebase GA4 kit](https://github.com/mparticle-integrations/mparticle-android-integration-google-analytics-firebase-ga4). Please see the [mParticle GA4](https://docs.mparticle.com/integrations/google-analytics-4/event) docs for more information on upgrading.

## Google Analytics for Firebase Kit Integration

This repository contains the [Google Analytics for Firebase](https://firebase.google.com/docs/analytics/android/start) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-googleanalyticsfirebase-kit:5+'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Google Analytics for Firebase detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Google Analytics for Firebase integration](http://docs.mparticle.com/integrations/firebase)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
