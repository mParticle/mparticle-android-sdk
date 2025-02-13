package com.mparticle.audience

fun interface AudienceTaskFailureListener {
    fun onFailure(result: AudienceResponse?)
}