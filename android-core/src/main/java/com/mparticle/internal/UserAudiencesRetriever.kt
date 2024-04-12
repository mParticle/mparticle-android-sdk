package com.mparticle.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


internal class UserAudiencesRetriever(private val apiClient: MParticleApiClient) {


    private val mApiClient: MParticleApiClient = apiClient
    fun fetchAudience(){
        Logger.debug("Mansi  fetchUserAudiences  " + this.javaClass.name)

        CoroutineScope(Dispatchers.IO).launch {
            val audienceApi=apiClient.fetchUserAudience()
        }
    }

}
