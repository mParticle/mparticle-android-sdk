# AppsFlyer Kit Integration

This directory contains the [AppsFlyer](https://www.appsflyer.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

This kit targets **AppsFlyer Android SDK 7.x**. For AppsFlyer 6.x, use the `appsflyer-6` kit instead.
AppsFlyer 7.x requires `minSdk 21`.

## Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:appsflyer-7:6+'
    }
    ```

2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"AppsFlyer detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.
4. If you wish to utilize Appsflyers InstallReferrer capabilities, add a dependency for Play Install Referrer library in you app's build.gradle. For more information visit the [Appsflyer SDK's documentation page](https://support.appsflyer.com/hc/en-us/articles/207032066#attribution) on the subject:

    ```groovy
    dependencies {
         implementation "com.android.installreferrer:installreferrer:2.2"
    }
    ```

## Migrating from the `appsflyer-6` kit

AppsFlyer 7.0 relocated much of its public API and removed several methods. The kit absorbs most of
this, but two changes are visible to integrating apps:

- **`minSdk` is now 21** (AppsFlyer 7.x raised its own minimum from 19).
- **Email identities are forwarded via AppsFlyer's `setUserEmail`.** AppsFlyer 7.0 removed
  `setUserEmails(EmailsCryptType, ...)` along with the `EmailsCryptType` enum, so the kit can no
  longer request SHA256 hashing on AppsFlyer's side. The kit previously passed
  `EmailsCryptType.NONE` (plaintext), so the value actually forwarded is unchanged.

AppsFlyer 7.x also splits its implementation into a companion `com.appsflyer:af-android-sdk-base`
artifact, which resolves transitively.

If your app calls the AppsFlyer SDK directly in addition to using this kit, see AppsFlyer's own 7.0
migration notes — the `com.appsflyer.*` -> `com.appsflyer.share.*` package move affects your code too.

## Documentation

[AppsFlyer integration](http://docs.mparticle.com/integrations/appsflyer/event/)

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
