package com.mparticle.modernization

import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.modernization.identity.IdentityCallback

interface MParticleIdentity : MParticleComponent {

    fun login(callback: IdentityCallback)
    fun logout(callback: IdentityCallback)

    //Will return the user with id or current user
    fun getUser(mpId: Long?, callback: IdentityCallback)
    fun getUsers(callback: MParticleCallback<List<MParticleUser>, Unit>)
}

internal interface InternalIdentity : MParticleIdentity {
    fun identify(request: IdentityApiRequest, callback: IdentityCallback)
    fun modify(request: IdentityApiRequest, callback: IdentityCallback)

    fun logout(request: IdentityApiRequest, callback: IdentityCallback)
    fun login(request: IdentityApiRequest, callback: IdentityCallback)
}