package com.mparticle.modernization.data

import com.mparticle.internal.messages.BaseMPMessage

class MParticleDataRepository {

    suspend fun updateSession(data : BaseMPMessage) {}

    suspend fun insertBreadcrumb(data : BaseMPMessage) {}
}