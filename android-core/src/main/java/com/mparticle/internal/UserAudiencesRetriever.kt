package com.mparticle.internal

import com.mparticle.audience.AudienceResponse
import com.mparticle.audience.AudienceTask
import com.mparticle.audience.BaseAudienceTask
import com.mparticle.identity.IdentityApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class UserAudiencesRetriever(apiClient: MParticleApiClient) {

    private val mApiClient: MParticleApiClient = apiClient
    fun fetchAudiences(mpId: Long, featureFlagEnabled: Boolean): AudienceTask<AudienceResponse> {
        val task = BaseAudienceTask()
        if (featureFlagEnabled) {
            CoroutineScope(Dispatchers.IO).launch {
                mApiClient.fetchUserAudience(task, mpId)
            }
        } else {
            task.setFailed(
                AudienceResponse(
                    IdentityApi.UNKNOWN_ERROR,
                    "Audience API call forbidden: Audience API is not enabled for your account"
                )
            )
        }
        return task
    }
}
