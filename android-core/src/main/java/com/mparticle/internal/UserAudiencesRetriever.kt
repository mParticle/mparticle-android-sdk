package com.mparticle.internal

import com.mparticle.identity.audience.AudienceResponse
import com.mparticle.identity.audience.AudienceTask
import com.mparticle.identity.audience.BaseAudienceTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class UserAudiencesRetriever(private val apiClient: MParticleApiClient) {

    private val mApiClient: MParticleApiClient = apiClient
    fun fetchAudience(): AudienceTask<AudienceResponse> {
        val task = BaseAudienceTask()
        CoroutineScope(Dispatchers.IO).launch {
            mApiClient.fetchUserAudience(task)
        }
        return task
    }
}
