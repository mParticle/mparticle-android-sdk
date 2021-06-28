package com.mparticle.kits
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.tasks.Upload
import org.gradle.plugins.signing.SigningExtension

class KitPlugin implements Plugin<Project> {
    void apply(Project target) {

        //formerly in kit-common.gradle
        target.apply(plugin: 'com.android.library')
        target.group = 'com.mparticle'
        target.buildscript.repositories.add(target.repositories.mavenLocal())
        target.buildscript.repositories.add(target.repositories.jcenter())
        target.buildscript.repositories.add(target.repositories.google())
        target.repositories.add(target.repositories.mavenLocal())
        target.repositories.add(target.repositories.jcenter())
        target.repositories.add(target.repositories.google())
        target.configurations.create('deployerJars')
        target.dependencies.add('api', 'com.mparticle:android-kit-base:' + target.version)
        target.dependencies.add('testImplementation', 'junit:junit:4.12')
        target.dependencies.add('testImplementation', 'org.mockito:mockito-core:1.10.19')
        target.dependencies.add('deployerJars', 'org.kuali.maven.wagons:maven-s3-wagon:1.2.1')
        target.extensions.create("mparticle", MParticlePluginExtension)
        LibraryExtension androidLib = target.android
        androidLib.compileSdkVersion(30)
        androidLib.buildToolsVersion('30.0.2')
        androidLib.defaultConfig.versionCode = Integer.parseInt(new Date().format('yyyyMMdd'))
        androidLib.defaultConfig.minSdkVersion 14
        androidLib.defaultConfig.targetSdkVersion 30
        androidLib.buildTypes.release.minifyEnabled false
        androidLib.buildTypes.release.consumerProguardFiles 'consumer-proguard.pro'
        androidLib.dexOptions.javaMaxHeapSize '2g'

        //formerly in maven.gradle
        target.apply(plugin: 'maven')
        target.apply(plugin: 'signing')

        def signingKey = System.getenv('mavenSigningKeyId')
        def signingPassword = System.getenv('mavenSigningKeyPassword')

        if (signingKey != null) {
            target.extensions.add('signing.keyId', signingKey)
            target.extensions.add('signing.password', signingPassword)

            SigningExtension signing = new SigningExtension(target)
            signing.required = { target.gradle.taskGraph.hasTask("uploadArchives") }
            signing.useInMemoryPgpKeys(signingKey, signingPassword)
            signing.sign target.configurations.archives
        }

        def target_maven_repo = 'local'
        if (target.hasProperty('target_maven_repo')) {
            target_maven_repo = target.property('target_maven_repo')
        }

        target.afterEvaluate {
            ((Upload) target.uploadArchives).repositories {

                mavenDeployer {
                    if (target_maven_repo == 'sonatype') {
                        beforeDeployment {
                            MavenDeployment deployment ->
                                target.signing.useInMemoryPgpKeys(signingKey, signingPassword)
                                target.signing.signPom(deployment)
                        }
                        repository(url: 'https://oss.sonatype.org/service/local/staging/deploy/maven2/') {
                            authentication(userName: System.getenv('sonatypeUsername'), password: System.getenv('sonatypePassword'))
                        }
                    } else if (target_maven_repo == 'sonatype-snapshot') {
                        beforeDeployment {
                            MavenDeployment deployment ->
                                target.signing.useInMemoryPgpKeys(signingKey, signingPassword)
                                target.signing.signPom(deployment)
                        }
                        repository(url: 'https://oss.sonatype.org/content/repositories/snapshots/') {
                            authentication(userName: System.getenv('sonatypeUsername'), password: System.getenv('sonatypePassword'))
                        }
                    } else if (target_maven_repo == 's3') {
                        configuration = target.configurations.deployerJars
                        repository(url: 's3://maven.mparticle.com')
                    } else {
                        repository(url: 'file://' + new File(System.getProperty('user.home'), '.m2/repository').absolutePath)
                    }

                    pom.project {
                        artifactId target.name
                        packaging 'aar'
                        name target.name
                        if (target.mparticle.kitDescription == null) {
                            description target.name + ' for the mParticle SDK'
                        } else {
                            description target.mparticle.kitDescription
                        }
                        url 'https://github.com/mparticle/mparticle-sdk-android'

                        licenses {
                            license {
                                name 'The Apache Software License, Version 2.0'
                                url 'http://www.apache.org/license/LICENSE-2.0.txt'
                            }
                        }

                        scm {
                            url 'https://github.com/mparticle/mparticle-android-sdk'
                            connection 'scm:git:https://github.com/mparticle/mparticle-android-sdk'
                            developerConnection 'scm:git:git@github.com:mparticle/mparticle-android-sdk.git'
                        }

                        developers {
                            developer {
                                id 'mParticle'
                                name 'mParticle Inc.'
                                email 'developers@mparticle.com'
                            }
                        }
                    }
                }
            }
        }

    }
}