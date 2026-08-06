package com.mparticle.publish

import org.gradle.api.provider.Property

interface MParticleMavenPublishExtension {
    val groupId: Property<String>
    val artifactId: Property<String>
    val description: Property<String>

    // Optional POM branding overrides. When unset, the convention plugin falls back to the
    // mParticle defaults (Apache 2.0 license, mParticle project URL). Used by non-mParticle
    // artifacts published from this repo (e.g. com.rokt:rokt-sdk-plus) so their POM carries
    // the correct project URL and license.
    val pomUrl: Property<String>
    val licenseName: Property<String>
    val licenseUrl: Property<String>
}
