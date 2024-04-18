package com.mparticle.identity.audience

fun interface AudienceTaskFailureListener {
    fun onFailure(result: AudienceResponse?)
}