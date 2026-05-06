package com.mparticle.modernization.identity

import com.mparticle.identity.IdentityApiResult
import com.mparticle.modernization.MParticleCallback

/**
 * Example of the identity callback extending the standarized behavior of the MParticleCallback
 * that defines success and error
 */
internal open class IdentityCallback : MParticleCallback<IdentityApiResult, Throwable>()
