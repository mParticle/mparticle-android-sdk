package com.mparticle.utils

import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.api.InstallType
import com.mparticle.api.events.toMPEvent
import com.mparticle.api.identity.IdentityType
import com.mparticle.identity.AccessUtils
import com.mparticle.internal.MParticleApiClient
import com.mparticle.internal.MessageManager
import com.mparticle.messages.ConfigResponseMessage
import com.mparticle.testing.BaseTest
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.TestingUtils
import com.mparticle.toMParticleOptions
import com.mparticle.api.identity.toIdentityType as toIdentityTypeCrossPlatform
import com.mparticle.testing.startMParticle as startMParticleCrossPlatform
import com.mparticle.toMParticleOptions as toMParticleOptionsCrossPlatform

fun MParticleOptions.Builder.toMParticleOptions(): com.mparticle.api.MParticleOptions {
    return this.toMParticleOptionsCrossPlatform()
}

fun BaseTest.startMParticle(builder: MParticleOptions.Builder) {
    startMParticleCrossPlatform(builder)
}
fun BaseTest.startMParticle(builder: MParticleOptions.Builder, initialConfigMessage: ConfigResponseMessage) {
    startMParticle(builder.toMParticleOptions(), initialConfigMessage)
}

fun setUserIdentity(value: String?, identityType: MParticle.IdentityType, mpid: Long) {
    AccessUtils.setUserIdentity(value, identityType, mpid)
}

fun IdentityType.toIdentityType(): MParticle.IdentityType = toIdentityTypeCrossPlatform()

fun randomIdentities(count: Int? = null): Map<MParticle.IdentityType, String> = (
    count?.let {
        RandomUtils.getRandomUserIdentities(
            count
        )
    } ?: RandomUtils.getRandomUserIdentities()
    ).entries.associate { it.key.toIdentityType()!! to it.value }

fun getInstallType(messageManager: MessageManager): MParticle.InstallType {
    return com.mparticle.internal.AccessUtils.getInstallType(messageManager)
}

fun randomMPEventRich(): MPEvent {
    return TestingUtils.randomMPEventRich.toMPEvent()
}

fun setCredentialsIfEmpty(builder: MParticleOptions.Builder) {
    com.mparticle.AccessUtils.setCredentialsIfEmpty(builder)
}

fun setMParticleApiClient(apiClient: MParticleApiClient) {
    com.mparticle.internal.AccessUtils.setMParticleApiClient(apiClient)
}
