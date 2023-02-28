package com.mparticle.modernization.identity

import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityApiResult
import com.mparticle.identity.MParticleUser
import com.mparticle.modernization.MParticleCallback
import com.mparticle.modernization.MParticleComponent

interface MParticleIdentity : MParticleComponent {

    fun login(callback: IdentityCallback)
    fun logout(callback: IdentityCallback)

    //Will return the user with id or current user
    fun getUser(mpId: Long?, callback: IdentityCallback)
    fun getUsers(callback: MParticleCallback<List<MParticleUser>, Unit>)

    //Coroutines
    suspend fun getUser(mpId: Long) : IdentityApiResult
    suspend fun getUsers(): List<MParticleUser>
    suspend fun login(): IdentityApiResult
    suspend fun logout(): IdentityApiResult
}

internal interface InternalIdentity : MParticleIdentity {
    suspend fun identify(request: IdentityApiRequest): IdentityApiResult
    suspend fun modify(request: IdentityApiRequest): IdentityApiResult
    suspend fun logout(request: IdentityApiRequest): IdentityApiResult
    suspend fun login(request: IdentityApiRequest): IdentityApiResult
}