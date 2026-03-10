## Iterable Kit Integration

This repository contains the [Iterable](https://www.iterable.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

mParticle's Iterable integration is predominantly server-side. This kit is an optional add-on to handle Iterable deep links.


### Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-iterable-kit:5+'
    }
    ```

2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Iterable detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Iterable Integration Docs](https://docs.mparticle.com/integrations/iterable/audience/)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
