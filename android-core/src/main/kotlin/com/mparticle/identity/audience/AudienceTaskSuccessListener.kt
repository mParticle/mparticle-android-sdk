package com.mparticle.identity.audience

fun interface AudienceTaskSuccessListener {
    fun onSuccess(result: AudienceResponse)
}