package com.mparticle.kits

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

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
        target.dependencies.add('api', 'com.mparticle:android-kit-base:' + target.version)
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


        //formerly in maven.gradle
        target.apply(plugin: 'maven-publish')
        target.apply(plugin: 'signing')

        target.afterEvaluate {
            PublishingExtension publishing = target.extensions.findByName('publishing')
            publishing.publications.create("release", MavenPublication.class) {
                groupId = "com.mparticle"
                artifactId = target.name
                version = target.version
                if (target.plugins.findPlugin("com.android.library")) {
                    from target.components.release
                } else {
                    from target.components.java
                }

                pom {
                    artifactId = target.name
                    packaging = 'aar'
                    name = target.name
                    if (target.mparticle.kitDescription == null) {
                        description = target.name + ' for the mParticle SDK'
                    } else {
                        description = target.mparticle.kitDescription
                    }
                    url = 'https://github.com/mparticle/mparticle-sdk-android'
                    licenses {
                        license {
                            name = 'The Apache Software License, Version 2.0'
                            url = 'https://www.apache.org/license/LICENSE-2.0.txt'
                        }
                    }
                    developers {
                        developer {
                            id = 'mParticle'
                            name = 'mParticle Inc.'
                            email = 'developers@mparticle.com'
                        }
                    }
                    scm {
                        url = 'https://github.com/mparticle/mparticle-android-sdk'
                        connection = 'scm:git:https://github.com/mparticle/mparticle-android-sdk'
                        developerConnection = 'scm:git:git@github.com:mparticle/mparticle-android-sdk.git'
                    }
                }
            }
            publishing.publications.register("debug", MavenPublication.class) {
                groupId = "com.mparticle"
                artifactId = target.name
                version = target.version
                if (target.plugins.findPlugin("com.android.library")) {
                    from target.components.debug
                } else {
                    from target.components.java
                }

                pom {
                    artifactId = target.name
                    packaging = 'aar'
                    name = target.name
                    if (target.mparticle.kitDescription == null) {
                        description = target.name + ' for the mParticle SDK'
                    } else {
                        description = target.mparticle.kitDescription
                    }
                    url = 'https://github.com/mparticle/mparticle-sdk-android'
                    licenses {
                        license {
                            name = 'The Apache Software License, Version 2.0'
                            url = 'https://www.apache.org/license/LICENSE-2.0.txt'
                        }
                    }
                    developers {
                        developer {
                            id = 'mParticle'
                            name = 'mParticle Inc.'
                            email = 'developers@mparticle.com'
                        }
                    }
                    scm {
                        url = 'https://github.com/mparticle/mparticle-android-sdk'
                        connection = 'scm:git:https://github.com/mparticle/mparticle-android-sdk'
                        developerConnection = 'scm:git:git@github.com:mparticle/mparticle-android-sdk.git'
                    }
                }
            }

            publishing.repositories.maven {
                credentials {
                    username System.getenv('sonatypeUsername') ?: ""
                    password System.getenv('sonatypePassword') ?: ""
                }
                url = 'https://oss.sonatype.org/service/local/staging/deploy/maven2/'
            }

            def signingKey = System.getenv("mavenSigningKeyId")
            def signingPassword = System.getenv("mavenSigningKeyPassword")

            if (signingKey != null) {
                target.extensions.add('signing.keyId', signingKey)
                target.extensions.add('signing.password', signingPassword)

                SigningExtension signing = new SigningExtension(target)
                signing.required = { target.gradle.taskGraph.hasTask("publishReleasePublicationToMavenRepository") }
                signing.useInMemoryPgpKeys(signingKey, signingPassword)
                signing.sign publishing.publications.findByName("release")
            }
        }

        //Publishing task aliases for simpler local development
        target.task("publishLocal") { dependsOn "publishDebugPublicationToMavenLocal" }
        target.task("publishReleaseLocal") { dependsOn "publishReleasePublicationToMavenLocal" }
    }
}

