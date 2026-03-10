package com.mparticle.publish

import org.gradle.api.provider.Property

interface MParticleMavenPublishExtension {
    val groupId: Property<String>
    val artifactId: Property<String>
    val description: Property<String>
}
