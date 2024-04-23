package com.mparticle.internal

import com.mparticle.audience.AudienceResponse
import com.mparticle.audience.AudienceTask
import com.mparticle.audience.BaseAudienceTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class UserAudiencesRetriever(apiClient: MParticleApiClient) {

    private val mApiClient: MParticleApiClient = apiClient
    fun fetchAudience(mpId: Long): AudienceTask<AudienceResponse> {
        val task = BaseAudienceTask()
        CoroutineScope(Dispatchers.IO).launch {
            mApiClient.fetchUserAudience(task, mpId)
        }
        return task
    }
}
