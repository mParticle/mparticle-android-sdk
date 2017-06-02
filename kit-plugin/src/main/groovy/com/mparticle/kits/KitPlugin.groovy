package com.mparticle.kits
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.tasks.Upload
import org.gradle.plugins.signing.SigningExtension

class KitPlugin implements Plugin<Project> {
    void apply(Project target) {

        //formerly in kit-common.gradle
        target.apply(plugin: 'com.android.library')
        target.group = 'com.mparticle'
        target.buildscript.repositories.add(target.repositories.mavenLocal())
        target.repositories.add(target.repositories.mavenLocal())
        target.buildscript.repositories.add(target.repositories.jcenter())
        target.repositories.add(target.repositories.jcenter())
        target.dependencies.add('compile', 'com.mparticle:android-kit-base:'+target.version)
        target.dependencies.add('testCompile', 'junit:junit:4.12')
        target.dependencies.add('testCompile', 'org.mockito:mockito-core:1.10.19')
        target.extensions.create("mparticle", MParticlePluginExtension)
        LibraryExtension androidLib = target.android
        androidLib.compileSdkVersion(25)
        androidLib.buildToolsVersion('25.0.2')
        androidLib.defaultConfig.versionCode = Integer.parseInt(new Date().format('yyyyMMdd'))
        androidLib.defaultConfig.minSdkVersion 9
        androidLib.defaultConfig.targetSdkVersion 25
        androidLib.buildTypes.release.minifyEnabled false
        androidLib.buildTypes.release.consumerProguardFiles 'consumer-proguard.pro'

        //formerly in maven.gradle
        target.apply(plugin: 'maven')
        target.apply(plugin: 'signing')

        def keyId = System.getenv('mavenSigningKeyId')
        def secretRing = System.getenv('mavenSigningKeyRingFile')
        def password = System.getenv('mavenSigningKeyPassword')

        if (keyId != null) {
            target.extensions.add('signing.keyId', keyId)
            target.extensions.add('signing.secretKeyRingFile', secretRing)
            target.extensions.add('signing.password', password)

            SigningExtension signing = new SigningExtension(target)
            signing.required = { target.gradle.taskGraph.hasTask("uploadArchives") }
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
                            MavenDeployment deployment -> target.signing.signPom(deployment)
                        }
                        repository(url: 'https://oss.sonatype.org/service/local/staging/deploy/maven2/') {
                            authentication(userName: System.getenv('sonatypeUsername'), password: System.getenv('sonatypePassword'))
                        }
                    } else if (target_maven_repo == 'sonatype-snapshot') {
                        beforeDeployment {
                            MavenDeployment deployment -> target.signing.signPom(deployment)
                        }
                        repository(url: 'https://oss.sonatype.org/content/repositories/snapshots/') {
                            authentication(userName: System.getenv('sonatypeUsername'), password: System.getenv('sonatypePassword'))
                        }
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
                                email 'dev@mparticle.com'
                            }
                        }
                    }
                }
            }
        }

    }
}