# mParticle Android SDK

![mParticle logo](https://static.mparticle.com/sdk/mp_logo_black.svg)

## Overview

[![Maven Central](https://img.shields.io/maven-central/v/com.mparticle/android-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.mparticle/android-core)

A single SDK to collect analytics data and send it to 100+ marketing, analytics, and data platforms. Simplify your data integration with a single API.

Hello! This is the public repo of the mParticle Android SDK. mParticle's mission is straightforward: make it really easy to use all of the great services in the app ecosystem. Our SDKs and platform are designed to be your abstraction layer and data hub, and we do the work of integrating with each individual app service so you don't have to.

The platform has grown to support 100+ partners in the ecosystem, including developer tools, analytics, attribution, marketing automation, and advertising services. We also have a powerful audience engine that sits atop our platform to let you action on all of your data - [learn more here](https://www.mparticle.com)!

### Core SDK

mParticle's Android integration is powered by a Core library, which supports mParticle's server-side integrations and audience platform.

You can grab the Core SDK via Maven Central. Please see the badge above and follow the [releases page](https://github.com/mParticle/mparticle-android-sdk/releases) to stay up to date with the latest version.

```groovy
dependencies {
    implementation 'com.mparticle:android-core:6.0.0'
}
```

### Kits

Several integrations require additional client-side add-on libraries called "kits." Some kits embed other SDKs, others just contain a bit of additional functionality. Kits are designed to feel just like server-side integrations; you enable, disable, filter, sample, and otherwise tweak kits completely from the mParticle platform UI. The Core SDK will detect kits at runtime, but you need to add them as dependencies to your build:

```groovy
dependencies {
    implementation 'com.mparticle:appsflyer-6:6.0.0'
    implementation 'com.mparticle:braze-41:6.0.0'
}
```

Kits maintained by mParticle are developed in this monorepo under the [`kits/`](kits) directory and deployed as individual Maven Central
artifacts. The table below lists kits with a final v6 artifact available on Maven Central:

| Kit                                        | Maven artifact                                                                                           |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------- |
| [Adjust](kits/adjust/adjust-5)             | [`com.mparticle:adjust-5`](https://central.sonatype.com/artifact/com.mparticle/adjust-5)                 |
| [Adobe](kits/adobe/adobe)                  | [`com.mparticle:adobe`](https://central.sonatype.com/artifact/com.mparticle/adobe)                       |
| [AppsFlyer](kits/appsflyer/appsflyer-6)    | [`com.mparticle:appsflyer-6`](https://central.sonatype.com/artifact/com.mparticle/appsflyer-6)           |
| [Apptentive](kits/apptentive/apptentive-6) | [`com.mparticle:apptentive-6`](https://central.sonatype.com/artifact/com.mparticle/apptentive-6)         |
| [Apptimize](kits/apptimize/apptimize-3)    | [`com.mparticle:apptimize-3`](https://central.sonatype.com/artifact/com.mparticle/apptimize-3)           |
| [Branch Metrics](kits/branch/branch-5)     | [`com.mparticle:branch-5`](https://central.sonatype.com/artifact/com.mparticle/branch-5)                 |
| [Braze 38](kits/braze/braze-38)            | [`com.mparticle:braze-38`](https://central.sonatype.com/artifact/com.mparticle/braze-38)                 |
| [Braze 39](kits/braze/braze-39)            | [`com.mparticle:braze-39`](https://central.sonatype.com/artifact/com.mparticle/braze-39)                 |
| [Braze 40](kits/braze/braze-40)            | [`com.mparticle:braze-40`](https://central.sonatype.com/artifact/com.mparticle/braze-40)                 |
| [Braze 41](kits/braze/braze-41)            | [`com.mparticle:braze-41`](https://central.sonatype.com/artifact/com.mparticle/braze-41)                 |
| [CleverTap](kits/clevertap/clevertap-7)    | [`com.mparticle:clevertap-7`](https://central.sonatype.com/artifact/com.mparticle/clevertap-7)           |
| [comScore](kits/comscore/comscore-6)       | [`com.mparticle:comscore-6`](https://central.sonatype.com/artifact/com.mparticle/comscore-6)             |
| [Iterable](kits/iterable/iterable-3)       | [`com.mparticle:iterable-3`](https://central.sonatype.com/artifact/com.mparticle/iterable-3)             |
| [Kochava](kits/kochava/kochava-5)          | [`com.mparticle:kochava-5`](https://central.sonatype.com/artifact/com.mparticle/kochava-5)               |
| [Leanplum](kits/leanplum/leanplum-7)       | [`com.mparticle:leanplum-7`](https://central.sonatype.com/artifact/com.mparticle/leanplum-7)             |
| [Localytics](kits/localytics/localytics-6) | [`com.mparticle:localytics-6`](https://central.sonatype.com/artifact/com.mparticle/localytics-6)         |
| [OneTrust](kits/onetrust/onetrust)         | [`com.mparticle:onetrust`](https://central.sonatype.com/artifact/com.mparticle/onetrust)                 |
| [Optimizely](kits/optimizely/optimizely-3) | [`com.mparticle:optimizely-3`](https://central.sonatype.com/artifact/com.mparticle/optimizely-3)         |
| [Radar](kits/radar/radar-3)                | [`com.mparticle:radar-3`](https://central.sonatype.com/artifact/com.mparticle/radar-3)                   |
| [Rokt](kits/rokt/rokt)                     | [`com.mparticle:android-rokt-kit`](https://central.sonatype.com/artifact/com.mparticle/android-rokt-kit) |
| [Singular](kits/singular/singular-12)      | [`com.mparticle:singular-12`](https://central.sonatype.com/artifact/com.mparticle/singular-12)           |

### Google Play Services Ads

The Google Play Services Ads framework is necessary to collect the Android Advertisting ID. AAID collection is required by all attribution and audience integrations, and many other integrations. Include the `-ads` artifact, a subset of [Google Play Services](https://developers.google.com/android/guides/setup):

```groovy
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
```

If your app does not declare this permission when targeting Android 13 or higher, the advertising ID is automatically removed and replaced with a string of zeroes.

When apps target Android 13 or above, you will need to declare a Google Play services permission in the manifest file as follows:

```xml
    <uses-permission android:name="com.google.android.gms.permission.AD_ID"/>
```

For more information, please check out this link: [https://support.google.com/googleplay/android-developer/answer/6048248?hl=en](https://support.google.com/googleplay/android-developer/answer/6048248?hl=en)

### Firebase Cloud Messaging

mParticle supports several marketing automation and push messaging integrations. These require that mParticle register for an instance id using the Firebase Cloud Messaging framework:

```groovy
    implementation(platform("com.google.firebase:firebase-bom:29.1.0"))
    implementation("com.google.firebase:firebase-messaging")

    //optional
    implementation("com.google.firebase:firebase-iid")
```

## Google Play Install Referrer

In order for attribution, deep linking, and many other integrations to work properly, the mParticle SDK collects the Google Play Install referrer string, which tracks the original link that brought the user to Google Play.

Since google has deprecated the "INSTALL_REFERRER" broadcast intent, you will need to add a the Play Install Referrer Library

### Play Install Referrer Library

Google now supports a [library that surface the referrer string](https://developer.android.com/google/play/installreferrer/library.html):

Simply add this dependency to your app and the mParticle SDK will detect it:

```groovy
implementation 'com.android.installreferrer:installreferrer:1+'
```

## Initialize the SDK

1. Grab your mParticle key and secret from [your workspace's dashboard](https://app.mparticle.com/setup/inputs/apps) and construct an `MParticleOptions` object.

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
            .logLevel(MParticle.LogLevel.VERBOSE)
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

### Proguard

Proguard is a minification/optimization/obfuscation tool that's extremely useful, and it can also cause some sticky bugs. The mParticle SDK is already minified so there's no need to...double-minify it. If you're using Gradle there's nothing to do - we include a `consumer-proguard` rules file inside our `AAR` which Gradle will automatically include in your build. If you're not using Gradle, please add those same rules manually - [see here for the latest](https://github.com/mParticle/mparticle-android-sdk/blob/master/android-core/consumer-proguard.pro).

### Data Planning (beta)

> **requires `node` and `npm`**

The Android SDK provides the ability to enforce your Data Plan via linting. Currently, this feature is beta-level, but it only runs
in the build environment, so there is no chance that it affects the runtime behavior of the mParticle SDK.

To enable Data Plan validation via linting, you must first download your Data Plan according to [these steps](insert-url.com)

We recommended you add the Data Plan in your application's root level directory, but it can be located anywhere in your project directory since `dataPlanFile` accepts a relative file path

### 1) Add the mParticle Gradle Plugin

The next step is to configure the mParticle Gradle Plugin. In your root `build.gradle` use the following code to add the plugin dependency to your buildscript:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        ...
        classpath 'com.mparticle:android-plugin:6.0.0'
    }
}
```

Next, apply the plugin in your project-level `build.gradle`

```groovy
apply plugin: 'com.mparticle'
```

### 2) Configure the Plugin

Either configure the mParticle Plugin object

```groovy
mparticle {
    dataPlanFile 'mp-dataplan.json'     //(required) accepts filename or path

    resultsFile 'mp-dp-results.json'    //(optional) accepts filename or path
    disabled false                      //(optional) defaults to "false"
    verbose false                       //{optional) defaults to "false"
}
```

### Or

provide an `mp.config` config file in the project-level directory

```json
{
    "dataPlanFile": "./mp-dataplan.json", //(required) accepts filename or path

    "resultsFile": "./mp-dp-results.json", //(optional) accepts filename or path
    "disabled": "false", //(optional) defaults to "false"
    "verbose": false //(optional) defaults to "false"
}
```

### 3) Install the mParticle CLI tool

Install the mParticle CLI. More documentation is available in it's [Github repo](https://git.corp.mparticle.com/mParticle/mparticle-cli)

```bash
./gradlew mpInstall
```

### 4) Viewing results

> Note: Any changes to your dataplan are not applied until the Gradle Project Syncs

Validation Errors surface in multiple locations.

- Individual Errors in the IDE as linting errors (red squiggly underlines), marking the offending code.
- Written to your `resultsFile`, if you configured one in the mParticle plugin
- Within the terminal, run `./gradlew lint`

### Custom Lint Checks

This SDK contains a number of custom link checks. These are designed to make the development process simpler and more integrated.

If at any time, they become too intrusive, they can easily switch off by including the Lint ID in the following block of your `build.gradle`:

```groovy
android {
    lintOptions {
        disable {LINT_ISSUE_ID_1}, {LINT_ISSUE_ID_2}, {LINT_ISSUE_ID_3}...
    }
}
```

### General

| Lint Issue ID                 | Description                                                                                            |
| ----------------------------- | ------------------------------------------------------------------------------------------------------ |
| MParticleVersionInconsistency | mParticle dependencies should, but do not have, matching versions                                      |
| MParticleInitialization       | mParticle.start() is not being called in Application.onCreate(), or may be being called multiple times |
| MParticleInstallRefReceiver   | ReferrerReceiver is present, but has been removed                                                      |

### Data Planning

| Lint Issue ID     | Description                                                                               |
| ----------------- | ----------------------------------------------------------------------------------------- |
| DataplanViolation | DataPlan violations                                                                       |
| NodeMissing       | The required `node` dependency is not present in the $PATH variable                       |
| DataPlanMissing   | Unable to fetch you DataPlan, could be a problem with credentials or network connectivity |

## Downloading and configuring the mParticle Kits

For information on regarding this topic please read our [Onboarding Document](ONBOARDING.md)

## Read More

Just by initializing the SDK you'll be set up to track user installs, engagement, and much more. Check out our doc site to learn how to add specific event tracking to your app.

- [SDK Documentation](https://docs.mparticle.com/developers/sdk/android/)
- [Javadocs](http://docs.mparticle.com/developers/sdk/android/javadocs/index.html)

## License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
