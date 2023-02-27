package com.mparticle.modernization.identity

import com.mparticle.MParticleOptions
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.modernization.InternalIdentity
import com.mparticle.modernization.MParticleCallback
import com.mparticle.modernization.MParticleMediator
import java.math.BigDecimal

internal class MParticleFakeIdentityImpl(private val mediator : MParticleMediator) : InternalIdentity {
    override fun identify(request: IdentityApiRequest, callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override fun modify(request: IdentityApiRequest, callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override fun logout(request: IdentityApiRequest, callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override fun logout(callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override fun login(request: IdentityApiRequest, callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override fun login(callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override fun getUser(mpId: Long?, callback: IdentityCallback) {
        TODO("Not yet implemented")
    }

    override fun getUsers(callback: MParticleCallback<List<MParticleUser>, Unit>) {
        TODO("Not yet implemented")
    }

}