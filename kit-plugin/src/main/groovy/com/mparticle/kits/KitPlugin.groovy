package com.mparticle.kits

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class KitPlugin implements Plugin<Project> {
    void apply(Project target) {

        //formerly in kit-common.gradle
        target.apply(plugin: 'com.android.library')
        target.group = 'com.mparticle'
        target.buildscript.repositories.add(target.repositories.mavenLocal())
        target.buildscript.repositories.add(target.repositories.google())
        target.buildscript.repositories.add(target.repositories.mavenCentral())
        target.repositories.add(target.repositories.mavenLocal())
        target.repositories.add(target.repositories.google())
        target.repositories.add(target.repositories.mavenCentral())
        target.configurations.create('deployerJars')
        def kitBaseVersion = target.findProperty('VERSION') ?: '+'
        target.dependencies.add('api', 'com.mparticle:android-kit-base:' + kitBaseVersion)
        target.dependencies.add('testImplementation', 'junit:junit:4.13.2')
        target.dependencies.add('testImplementation', 'org.mockito:mockito-core:1.10.19')
        target.dependencies.add('testImplementation', 'androidx.annotation:annotation:[1.0.0,)')
        target.dependencies.add('compileOnly', 'androidx.annotation:annotation:[1.0.0,)')
        target.extensions.create("mparticle", MParticlePluginExtension)
        LibraryExtension androidLib = target.android
        androidLib.compileSdk 33
        int dateInt = Integer.parseInt(new Date().format('yyyyMMdd'))
        androidLib.defaultConfig.versionCode = dateInt
        androidLib.defaultConfig.minSdk 16
        androidLib.defaultConfig.targetSdk 33
        androidLib.defaultConfig.buildConfigField("String", "VERSION_CODE", '\"' + dateInt + '\"')
        androidLib.buildTypes.release.minifyEnabled false
        androidLib.buildTypes.release.consumerProguardFiles 'consumer-proguard.pro'
        androidLib.lintOptions.abortOnError true
        androidLib.testOptions.unitTests.all {  jvmArgs += ['--add-opens', 'java.base/java.lang=ALL-UNNAMED']
            jvmArgs += ['--add-opens', 'java.base/java.lang.reflect=ALL-UNNAMED']
            jvmArgs += ['--add-opens', 'java.base/java.util=ALL-UNNAMED']
            jvmArgs += ['--add-opens', 'java.base/java.text=ALL-UNNAMED']
            jvmArgs += ['--add-opens', 'java.desktop/java.awt.font=ALL-UNNAMED']
            jvmArgs += ['--add-opens', 'java.base/java.util.concurrent=ALL-UNNAMED'] }


        // mparticle.android.library.publish is only available when building inside the
        // monorepo (buildSrc on classpath). Standalone kit builds and test fixtures skip
        // Maven publishing configuration — kits will be migrated to the monorepo.
        try {
            target.apply(plugin: 'mparticle.android.library.publish')
            def publishExt = target.extensions.getByName('mparticleMavenPublish')
            publishExt.artifactId.convention(target.providers.provider { target.name })
            publishExt.description.convention(target.providers.provider {
                String desc = target.extensions.findByName('mparticle')?.kitDescription
                desc ?: (target.name + ' for the mParticle SDK')
            })
        } catch (org.gradle.api.plugins.UnknownPluginException ignored) {
            // no-op: publish plugin unavailable outside monorepo
        }
    }
}

