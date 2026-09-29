# Leanplum Kit Integration

This directory contains the [Leanplum](https://www.leanplum.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

## Adding the integration

1. Add the kit dependency to your app's build.gradle. The Leanplum SDK it depends on is published to Maven Central:

    ```groovy
    dependencies {
        implementation 'com.mparticle:leanplum-7:6.0.0'
    }
    ```

2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Leanplum detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

## GCM Compatibility

Leanplum is deprecating GCM support, but it is still available. While we recommend migrating to FCM, if your application requires GCM, take the following steps:

1. Exclude the FCM transient dependency in the leanplum kit

    ```groovy
    dependencies {
        implementation ('com.mparticle:leanplum-7:6.0.0') {
                exclude module: 'leanplum-fcm'
        }
        implementation 'com.leanplum:leanplum-gcm:4.1.1'
    }
    ```

## Documentation

[Leanplum integration](https://docs.mparticle.com/integrations/leanplum/event/)

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
