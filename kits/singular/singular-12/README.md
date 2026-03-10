## Singular Kit Integration

This repository contains the [Singular](https://www.singular.net/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. The singular Kit requires that you add singular's Maven server to your buildscript:

    ```
    repositories {
        maven { url "https://maven.singular.net"}
        ...
    }
    ```

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-singular-kit:5+'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Singular detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Singular integration](https://docs.mparticle.com/integrations/singular/event/)

### License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
