## OneTrust Kit Integration

[See here for more information](https://github.com/mParticle/mparticle-android-sdk/wiki/Kit-Development) on how to use this example to write a new kit.

This repository contains the [OneTrust](https://www.onetrust.com) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-onetrust-kit:5+'
        // Implement the SDK version that corresponds to the published version you're using'
        implementation 'com.onetrust.cmp:native-sdk:X.X.0.0'

        // Example: 
        implementation 'com.onetrust.cmp:native-sdk:202308.1.0.0'
    }
}
    ```
    _Note: OneTrust is unique in their versioning and in that you must specify your version used from a constrained list in their UI. This necessitates that we cannot pin the version of the OneTrust SDK in this kit. Therefore you must pin the correct version in the build.gradle file of your application. For more information on this checkout this [OneTrust Guide for Adding the SDK to an Android App](https://developer.onetrust.com/onetrust/docs/adding-sdk-to-app-android)_
    
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"<REPLACE ME> detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### Documentation

[OneTrust integration](https://docs.mparticle.com/integrations/onetrust/event/)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
