package com.mparticle.modernization.identity.example

import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityApiResult
import com.mparticle.identity.MParticleUser
import com.mparticle.modernization.MParticleCallback
import com.mparticle.modernization.core.MParticleMediator
import com.mparticle.modernization.identity.IdentityCallback
import com.mparticle.modernization.identity.InternalIdentity

internal class MParticleIdentityImpl(private val mediator: MParticleMediator) :
    InternalIdentity {
    override suspend fun identify(request: IdentityApiRequest): IdentityApiResult {
        TODO("Not yet implemented")
    }

    override suspend fun modify(request: IdentityApiRequest): IdentityApiResult {
        TODO("Not yet implemented")
    }

    override suspend fun logout(request: IdentityApiRequest): IdentityApiResult {
        TODO("Not yet implemented")
    }

    override fun logout(callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override suspend fun logout(): IdentityApiResult {
        TODO("Not yet implemented")
    }

    override suspend fun login(request: IdentityApiRequest): IdentityApiResult {
        TODO("Not yet implemented")
    }

    override fun login(callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override suspend fun login(): IdentityApiResult {
        TODO("Not yet implemented")
    }

    override fun getUser(mpId: Long?, callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override suspend fun getUser(mpId: Long?): IdentityApiResult {
        TODO("Not yet implemented")
    }

    override fun getUsers(callback: MParticleCallback<List<MParticleUser>, Unit>) {
        TODO("Not yet implemented")
    }

    override suspend fun getUsers(): List<MParticleUser> {
        TODO("Not yet implemented")
    }
}
