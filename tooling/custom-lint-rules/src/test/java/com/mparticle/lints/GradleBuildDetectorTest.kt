package com.mparticle.lints

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.mparticle.lints.Constants.getErrorWarningMessageString
import com.mparticle.lints.detectors.GradleBuildDetector
import org.intellij.lang.annotations.Language
import org.junit.Test

class GradleBuildDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector {
        return GradleBuildDetector()
    }

    override fun getIssues(): List<Issue> {
        return listOf(GradleBuildDetector.ISSUE)
    }

    override fun allowMissingSdk(): Boolean {
        return true
    }

    // This one inherently can't happen, since the lint.jar file is in the mParticle dependency, but
    // we should test it anyway, in case there might be a weird case where the lint.jar get's cached,
    // and we should make sure we aren't breaking any project's lints
    @Test
    @Throws(Exception::class)
    fun testNoDependency() {
        @Language("GROOVY")
        val source = """apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "com.mparticle.sample"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner" 
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile 'com.android.support:appcompat-v7:25.1.1'
    compile 'com.android.support:design:25.1.1'
}"""
        lint()
            .files(gradle(source))
            .run()
            .expectErrorCount(0)
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testNoSingleDependencies() {
        @Language("GROOVY")
        val source = """apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "com.mparticle.sample"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner" 
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile 'com.android.support:appcompat-v7:25.1.1'
    compile 'com.android.support:design:25.1.1'
    compile 'com.mparticle:android-core:5.0'
}"""
        lint()
            .files(gradle(source))
            .run()
            .expectErrorCount(0)
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testNoSingleDependencyWithPlus() {
        @Language("Groovy")
        val source = """apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "com.mparticle.sample"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner" 
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile 'com.android.support:appcompat-v7:25.1.1'
    compile 'com.android.support:design:25.1.1'
    compile 'com.mparticle:android-core:5+'
}"""
        lint()
            .files(gradle(source))
            .run()
            .expectErrorCount(0)
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testNoErrorMultipleDependencies() {
        @Language("GROOVY")
        val source = """apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "com.mparticle.sample"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner" 
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile 'com.android.support:appcompat-v7:25.1.1'
    compile 'com.android.support:design:25.1.1'
    compile 'com.mparticle:android-core:5.0'
    compile 'com.mparticle:android-branch-kit:5.0'
}"""
        lint()
            .files(gradle(source))
            .run()
            .expect(Constants.NO_WARNINGS)
    }

    @Test
    @Throws(Exception::class)
    fun testNoErrorMultipleDependenciesWithPluses() {
        @Language("GROOVY")
        val source = """apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "com.mparticle.sample"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner" 
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile 'com.android.support:appcompat-v7:25.1.1'
    compile 'com.android.support:design:25.1.1'
    implementation 'com.mparticle:android-core:5+'
    api 'com.mparticle:android-branch-kit:5+'
}"""
        lint()
            .files(gradle(source))
            .run()
            .expectErrorCount(0)
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testInconsistentVersions() {
        @Language("GROOVY")
        val source = """apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "com.mparticle.sample"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner" 
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile 'com.android.support:appcompat-v7:25.1.1'
    compile 'com.android.support:design:25.1.1'
    compile 'com.mparticle:android-core:5.0'
    implementation 'com.mparticle:android-branch-kit:4.10.+'
}"""
        lint()
            .files(gradle(source))
            .run()
            .expectContains(GradleBuildDetector.MESSAGE_INCONSISTENCY_IN_VERSIONS_DETECTED)
            .expectContains(getErrorWarningMessageString(1, 0))
    }

    @Test
    @Throws(Exception::class)
    fun testInconsistentPluses() {
        @Language("GROOVY")
        val source = """apply plugin: 'com.android.application'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "com.mparticle.sample"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner" 
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile 'com.android.support:appcompat-v7:25.1.1'
    compile 'com.android.support:design:25.1.1'
    compile 'com.mparticle:android-core:5.0'
    compile 'com.mparticle:android-adjust-kit:5+'
}"""
        lint()
            .files(gradle(source))
            .run()
            .expectContains(GradleBuildDetector.MESSAGE_INCONSISTENCY_IN_VERSIONS_DETECTED)
            .expectContains(GradleBuildDetector.MESSAGE_DONT_MIX_PLUSES)
            .expectContains(getErrorWarningMessageString(1, 0))
    }
}
