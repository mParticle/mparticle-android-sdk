package com.mparticle.identity

import com.mparticle.networking.MParticleBaseClient

interface MParticleIdentityClient : MParticleBaseClient {
    @Throws(Exception::class)
    fun login(request: IdentityApiRequest): IdentityHttpResponse

    @Throws(Exception::class)
    fun logout(request: IdentityApiRequest): IdentityHttpResponse

    @Throws(Exception::class)
    fun identify(request: IdentityApiRequest): IdentityHttpResponse

    @Throws(Exception::class)
    fun modify(request: IdentityApiRequest): IdentityHttpResponse
}
