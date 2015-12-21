<img src="https://www.mparticle.com/assets/img/logo.svg" width="280">

## Android SDK

Hello! This is the public repo of the mParticle Android SDK. Right now, it contains the "kit" implementations and maintains a dependency on the mParticle Core library. 

We originally built the [mParticle platform](www.mparticle.com) to help simplify the approach to SDKs and improve app performance. We've found that having a bunch of SDKs in an app bloats its size and is no-good for your users' battery. So, we built client-side APIs for you to collect all of your data, and a server platform that lets you send your data where you want it to go.

From that original goal the platform has grown to support 50+ services and SDKs, including developer tools, analytics, attribution, messaging, and advertising services. mParticle is designed to serve as the connector between all of these services - check out [our site](www.mparticle.com), or hit us at dev@mparticle.com to learn more.

## Get the SDK

The SDK is composed of the Core library and a series of "kit" libraries that depend on Core. Though most of mParticle's integrations are on the server-side, some of these services need to be integrated on the client side - and that's where *kits* come in. The Core SDK takes care of initializing the kits depending on what you've configured in [your app's dashboard](http://app.mparticle.com), so you just have to decide which kits you *may* use prior to submission to Google Play. You can easily include all of the kits, none of the kits, or individual kits - the choice is yours.

### Gradle Setup

##### 1. Add our maven repo to your repositories

```groovy
repositories {
    maven { url "http://maven.mparticle.com/" } 
    ...
}
```
##### 2. Add dependencies

There are a few approaches to incorporating the SDK:

- Include *only* the Core library (`com.mparticle:android-core`). This means that you'll only be using *server-based* integrations.
- Pick the Kits (`com.mparticle:android-XXX-kit`) that you'd like to use, and include only those. A Kit will only be instantiated if you configure it in the mParticle services dashboard.
- Include all the Kits with the `com.mparticle:android-sdk` artifact.

```groovy
dependencies {
    // Option 1: 
    // Just the Core library - only use server-side integrations
    compile 'com.mparticle:android-core:4.+'
    
    // Option 2: 
    // Pick the individual kits you want
    // This will automatically incorporate android-core
    compile (
        'com.mparticle:android-adjust-kit:4.+',
        'com.mparticle:android-appboy-kit:4.+',
        'com.mparticle:android-branch-kit:4.+'
    )
    
    // Option 3: 
    // Use this to include ALL kits
    // This will automatically incorporate android-core
    compile "com.mparticle:android-sdk:4.+"
    
    //Required for many attribution services
    compile "com.google.android.gms:play-services-ads:7.8.0"
    
    //Optional - for Push notification support
    compile "com.google.android.gms:play-services-gcm:7.8.0"
}
```

Here's the full list of currently supported kits:

- `com.mparticle:android-adjust-kit:4.+`
- `com.mparticle:android-appboy-kit:4.+`
- `com.mparticle:android-branch-kit:4.+`
- `com.mparticle:android-comscore-kit:4.+`
- `com.mparticle:android-flurry-kit:4.+`
- `com.mparticle:android-kahuna-kit:4.+`
- `com.mparticle:android-kochava-kit:4.+`
- `com.mparticle:android-localytics-kit:4.+`

## Proguard

Proguard is a minification/optimization/obfuscation tool that's extremely useful, and it can also cause some sticky bugs. The mParticle SDK is already minified so there's no need to...double-minify it. With Proguard you can specify a set of exclusion rules - reference the sample below and edit your app's rules file to avoid processing mParticle, Google Play Services, and the various Kit SDK classes.

```ini
# mParticle (required)
-keep class com.mparticle.** { *; }

# Google Play Services (required)
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }

# Appboy Kit
-dontwarn com.amazon.device.messaging.**
-dontwarn bo.app.**
-keep class bo.app.** { *; } 
-keep class com.appboy.** { *; }

# Kahuna Kit
-keep class kahuna.sdk.** { *; } 

# Kochava Kit
-keep class kochava.android.** { *; } 

# Comscore Kit
-keep class com.comscore.** { *; } 
-dontwarn com.comscore.**

# Flurry Kit
-keep class com.flurry.** { *; }
-dontwarn com.flurry.**

# Localytics Kit
-keep class com.localytics.android.** { *; }
-keepattributes JavascriptInterface

```

## Show me the code

OK OK, slow down. Grab your mParticle key and secret from [your app's dashboard](https://app.mparticle.com/apps) and add them as `string` resources in your app:

```xml
<!-- ex. src/main/res/values/mparticle.xml -->
<?xml version="1.0" encoding="utf-8" ?>
<resources>
    <string name="mp_key">APP KEY</string>
    <string name="mp_secret">APP SECRET</string>
</resources>
```

#### Initialize the SDK

Call `start` from the `onCreate` method of your app's `Application` class. If you don't already have an `Application` class, create it and then specify its fully-qualified name in the `<application>` tag of your app's `AndroidManifest.xml`.

```java
package com.example.myapp;

import android.app.Application;
import com.mparticle.MParticle;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MParticle.start(this);
    }
}
```

**Note:** It's not advisable to log events in your `Application.onCreate()`. Android may instantiate your `Application` class for a lot of reasons, in the background, while the user isn't even using their device. 

## Read More

Just by initializing the SDK you'll be set up to track user installs, engagement, and much more. Check out our doc site to learn how to add specific event tracking to your app.

* [SDK Documentation](http://docs.mparticle.com/#sdk-documentation)
* [Javadocs](http://docs.mparticle.com/includes/javadocs)

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

