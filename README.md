<img src="http://static.mparticle.com/sdk/logo.svg" width="280">

## Android SDK

[![Maven Central Status](https://maven-badges.herokuapp.com/maven-central/com.mparticle/android-core/badge.svg?style=flat-square)](https://search.maven.org/#search%7Cga%7C1%7Cmparticle)

Hello! This is the public repo of the mParticle Android SDK. mParticle's mission is straightforward: make it really easy to use all of the great services in the app ecosystem. Our SDKs and platform are designed to be your abstraction layer and data hub, and we do the work of integrating with each individual app service so you don't have to.

The platform has grown to support 100+ partners in the ecosystem, including developer tools, analytics, attribution, marketing automation, and advertising services. We also have a powerful audience engine that sits atop our platform to let you action on all of your data - [learn more here](https://www.mparticle.com)!

### Core SDK

mParticle's Android integration is powered by a Core library, which supports mParticle's server-side integrations and audience platform.

You can grab the Core SDK via Maven Central. Please reference the badge above and follow the [releases page](https://github.com/mParticle/mparticle-android-sdk/releases) to stay up to date with the latest version.

```groovy
dependencies {
    implementation 'com.mparticle:android-core:5.1.1'
}
```

### Kits

Several integrations require additional client-side add-on libraries called "kits." Some kits embed other SDKs, others just contain a bit of additional functionality. Kits are designed to feel just like server-side integrations; you enable, disable, filter, sample, and otherwise tweak kits completely from the mParticle platform UI. The Core SDK will detect kits at runtime, but you need to add them as dependencies to your build:

```groovy
dependencies {
    implementation (
        'com.mparticle:android-example-kit:5.1.1',
        'com.mparticle:android-another-kit:5.1.1'
    )
}
```

Kits are deployed as individual artifacts in Maven Central, and each has a dedicated repository if you'd like to view the source code. Review the table below to see if you need to include any kits:

Kit | Maven Artifact 
----|---------
[Adjust](https://github.com/mparticle-integrations/mparticle-android-integration-adjust)                |  [`android-adjust-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-adjust-kit%22)
[Appboy](https://github.com/mparticle-integrations/mparticle-android-integration-appboy)                |  [`android-appboy-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-appboy-kit%22) 
[AppsFlyer](https://github.com/mparticle-integrations/mparticle-android-integration-appsflyer)          |  [`android-appsflyer-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-appsflyer-kit%22)
[Apptentive](https://github.com/mparticle-integrations/mparticle-android-integration-apptentive)          |  [`android-apptentive-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-apptentive-kit%22)
[Apptimize](https://github.com/mparticle-integrations/mparticle-android-integration-apptimize)              |  [`android-apptimize-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-apptimize-kit%22)
[Apteligent](https://github.com/mparticle-integrations/mparticle-android-integration-apteligent)        |  [`android-apteligent-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-apteligent-kit%22)
[Branch Metrics](https://github.com/mparticle-integrations/mparticle-android-integration-branch) |  [`android-branch-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-branch-kit%22)
[Button](https://github.com/mparticle-integrations/mparticle-android-integration-button)              |  [`android-button-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-button-kit%22)
[comScore](https://github.com/mparticle-integrations/mparticle-android-integration-comscore)            |  [`android-comscore-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-comscore-kit%22)
[Flurry](https://github.com/mparticle-integrations/mparticle-android-integration-flurry)                |  [`android-flurry-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-flurry-kit%22)
[ForeSee](https://github.com/mparticle-integrations/mparticle-android-integration-foresee)              |  [`android-foresee-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-foresee-kit%22)
[Instabot](https://github.com/mparticle-integrations/mparticle-android-integration-instabot)              |  [`android-instabot-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-instabot-kit%22)
[Iterable](https://github.com/mparticle-integrations/mparticle-android-integration-iterable)              |  [`android-iterable-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-iterable-kit%22)
[Kahuna](https://github.com/mparticle-integrations/mparticle-android-integration-kahuna)                |  [`android-kahuna-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-kahuna-kit%22)
[Kochava](https://github.com/mparticle-integrations/mparticle-android-integration-kochava)              |  [`android-kochava-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-kochava-kit%22)
[Leanplum](https://github.com/mparticle-integrations/mparticle-android-integration-leanplum)              |  [`android-leanplum-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-leanplum-kit%22)
[Localytics](https://github.com/mparticle-integrations/mparticle-android-integration-localytics)        |  [`android-localytics-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-localytics-kit%22)
[Radar](https://github.com/mparticle-integrations/mparticle-android-integration-radar)    |  [`android-radar-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-radar-kit%22)
[Reveal Mobile](https://github.com/mparticle-integrations/mparticle-android-integration-revealmobile)       |  [`android-revealmobile-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-revealmobile-kit%22)
[Tune](https://github.com/mparticle-integrations/mparticle-android-integration-tune)                    |  [`android-tune-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-tune-kit%22)
[Urban Airship](https://github.com/mparticle-integrations/mparticle-android-integration-urbanairship)                    |  [`android-urbanairship-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-urbanairship-kit%22)
[Wootric](https://github.com/mparticle-integrations/mparticle-android-integration-wootric)              |  [`android-wootric-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-wootric-kit%22)
[Singular](https://github.com/mparticle-integrations/mparticle-android-integration-singular)              |  [`android-singular-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-singular-kit%22)
[Skyhook](https://github.com/mparticle-integrations/mparticle-android-integration-skyhook)              |  [`android-skyhook-kit`](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mparticle%22%20AND%20a%3A%22android-skyhook-kit%22)

### Optional Dependencies

##### Google Play Services Ads

The Google Play Services Ads framework is necessary to collect the Android Advertisting ID. AAID collection is required by all attribution and audience integrations, and many other integrations. Include the `-ads` artifact, a subset of [Google Play Services](https://developers.google.com/android/guides/setup):

```groovy
    implementation 'com.google.android.gms:play-services-ads:11.6.2'
```

##### Firebase Cloud Messaging

mParticle supports several marketing automation and push messaging integrations. These require that mParticle register for an instance id using the Firebase Cloud Messaging framework:

```groovy
    implementation 'com.google.firebase:firebase-messaging:11.6.2'
```

## Google Play Install Referrer

In order for attribution, deep linking, and many other integrations to work properly, the mParticle SDK collects the Google Play Install referrer string, which tracks the original link that brought the user to Google Play.

There are two different ways to collect this value - you only need to implement one.

### 1. Play Install Referrer Library

Google now supports a [library that surface the referrer string](https://developer.android.com/google/play/installreferrer/library.html):

Simply add this dependency to your app and the mParticle SDK will detect it:

```groovy
implementation 'com.android.installreferrer:installreferrer:1.0'
```

### 2. Broadcast Receivers

Alternatively, you can add the mParticle `ReferrerReceiver` to your manifest file within the `<application>` tag. The mParticle SDK
will collect any campaign referral information and automatically forward it to kits (such as AppsFlyer, Kochava, and Adjust) and server-side integrations.

#### Single Receiver

If you have no other `BroadcastReceiver` that listens for the `INSTALL_REFERRER` intent, you can just add the mParticle receiver:

```xml
<receiver android:name="com.mparticle.ReferrerReceiver" android:exported="true">
    <intent-filter>
        <action android:name="com.android.vending.INSTALL_REFERRER"/>
    </intent-filter>
</receiver>
```

#### Multiple Receivers

Google Play will only deliver the `INSTALL_REFERRER` Intent to a single `BroadcastReceiver` - you **cannot** have multiple in your manifest. If you already have your own receiver, or otherwise have multiple receivers that require
referral data, you must expose your own `BroadcastReceiver` in your manifest and then forward the received Intent to mParticle:

```java
public class ExampleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //process the Intent/send to other receivers as desired, and
        //send the Context and Intent into mParticle's BroadcastReceiver
        new com.mparticle.ReferrerReceiver().onReceive(context, intent);
    }
}
```

## Initialize the SDK

1. Grab your mParticle key and secret from [your workspace's dashboard](https://app.mparticle.com/apps) and construct an `MParticleOptions` object.

2. Call `start` from the `onCreate` method of your app's `Application` class. It's crucial that the SDK be started here for proper session management. If you don't already have an `Application` class, create it and then specify its fully-qualified name in the `<application>` tag of your app's `AndroidManifest.xml`.

```java
package com.example.myapp;

import android.app.Application;
import com.mparticle.MParticle;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MParticleOptions options = MParticleOptions.builder(this)
            .credentials("REPLACE ME WITH KEY","REPLACE ME WITH SECRET")
            .setLogLevel(MParticle.LogLevel.VERBOSE)
            .identify(identifyRequest)
            .identifyTask(
                new BaseIdentityTask()
                        .addFailureListener(this)
                        .addSuccessListener(this)
                    )
            .build();

        MParticle.start(options);
    }
}
```

> **Warning:** It's generally not a good idea to log events in your `Application.onCreate()`. Android may instantiate your `Application` class for a lot of reasons, in the background, while the user isn't even using their device. 

#### Proguard

Proguard is a minification/optimization/obfuscation tool that's extremely useful, and it can also cause some sticky bugs. The mParticle SDK is already minified so there's no need to...double-minify it. If you're using Gradle there's nothing to do - we include a `consumer-proguard` rules file inside our `AAR` which Gradle will automatically include in your build. If you're not using Gradle, please add those same rules manually - [see here for the latest](https://github.com/mParticle/mparticle-android-sdk/blob/master/android-core/consumer-proguard.pro). 

## Read More

Just by initializing the SDK you'll be set up to track user installs, engagement, and much more. Check out our doc site to learn how to add specific event tracking to your app.

* [SDK Documentation](https://docs.mparticle.com/developers/sdk/android/)
* [Javadocs](http://docs.mparticle.com/developers/sdk/android/javadocs/index.html)

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
