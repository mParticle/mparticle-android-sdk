package com.mparticle.modernization.data

import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.modernization.MpBatch

/**
 * This is based on the repository pattern to manage data sources (apiClients, daos,
 * sharedPreferences, cache, memory, etc).
 * Ideally we would inject our data sources here, and the repository would be incharge of preprocessing
 * the data interacting with the data source, and post-process the data coming from data sources.
 */
internal class MParticleDataRepository {

    suspend fun insertCommerceDTO(data: BaseMPMessage) {
        //TODO parses the event and saves it into the db
    }

    suspend fun getEventsByType() : List<BaseMPMessage>? = emptyList()

    suspend fun getBatch() : MpBatch? = null
}
