package com.mparticle.audience

fun interface AudienceTaskSuccessListener {
    fun onSuccess(result: AudienceResponse)
}