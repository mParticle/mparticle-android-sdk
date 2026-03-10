import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.10"
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions { jvmTarget = JavaVersion.VERSION_17.toString() }
}

dependencies {
    implementation("com.android.tools.build:gradle:8.3.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.31.0")
}

gradlePlugin {
    plugins {
        register("androidLibraryPublish") {
            id = "mparticle.android.library.publish"
            implementationClass = "AndroidLibraryMavenCentralPublishPlugin"
        }
    }
}
