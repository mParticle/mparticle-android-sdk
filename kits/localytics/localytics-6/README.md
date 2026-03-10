## Localytics Kit Integration

This repository contains the [Localytics](https://www.localytics.com) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. The Localytics Kit requires that you add Localytics' Maven server to your buildscript:

    ```
    repositories {
        maven { url 'https://maven.localytics.com/public' }
        ...
    }
    ```

2. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-localytics-kit:5+'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Localytics detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Localytics integration](https://docs.mparticle.com/integrations/localytics/event/)

### License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
