## Leanplum Kit Integration

This repository contains the [Leanplum](https://www.leanplum.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. The Leanplum Kit requires that you add Leanplum's Maven server to your buildscript:

    ```
    repositories {
        maven { url "http://www.leanplum.com/leanplum-sdks/" }
        ...
    }
    ```

2. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-leanplum-kit:5+'
    }
    ```

3. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Leanplum detected"` in the output of `adb logcat`.
4. Reference mParticle's integration docs below to enable the integration.

### GCM Compatibility

Leanplum is deprecating GCM support, but it is still available. While we recommend migrating to FCM, if your application requires GCM, take the following steps:

1. Exclude the FCM transient dependency in the leanplum kit

    ```groovy
    dependencies {
        implementation ('com.mparticle:android-leanplum-kit:REPLACE-ME') {
                exclude module: 'leanplum-fcm'
        }
        implementation 'com.leanplum:leanplum-gcm:4.1.1'
    }
    ```
### Documentation

[Leanplum integration](https://docs.mparticle.com/integrations/leanplum/event/)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
