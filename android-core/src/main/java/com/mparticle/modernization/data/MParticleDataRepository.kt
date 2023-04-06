package com.mparticle.modernization.data

import com.mparticle.internal.messages.BaseMPMessage

internal class MParticleDataRepository {

    suspend fun updateSession(data: BaseMPMessage) {}

    suspend fun insertBreadcrumb(data: BaseMPMessage) {}
}
