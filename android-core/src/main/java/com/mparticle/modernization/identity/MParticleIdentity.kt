package com.mparticle.modernization.identity

import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityApiResult
import com.mparticle.identity.MParticleUser
import com.mparticle.modernization.MParticleCallback
import com.mparticle.modernization.core.MParticleComponent
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

interface MParticleIdentity : MParticleComponent {

    /**
     * Login
     * @param callback
     */
    fun login(@NotNull callback: IdentityCallback)

    /**
     * Logout
     * @param callback
     */
    fun logout(@NotNull callback: IdentityCallback)

    /**
     * Returns the user by id or the current user if [mpId] is null
     * @param mpId if null returns the current user, if not the user by id
     * @param callback
     */
    fun getUser(@Nullable mpId: Long?, @NotNull callback: IdentityCallback)

    /**
     * Return all users
     * @param callback
     */
    fun getUsers(@NotNull callback: MParticleCallback<List<MParticleUser>, Unit>)

    /**
     * Returns the user by id or the current user if [mpId] is null
     * @param mpId if null returns the current user, if not the user by id
     * @return identityApiResult
     */
    suspend fun getUser(@Nullable mpId: Long?): IdentityApiResult

    /**
     * Return all users
     * @return mParicleUsers in a list
     */
    suspend fun getUsers(): List<MParticleUser>

    /**
     * Login
     * @return identityApiResult
     */
    suspend fun login(): IdentityApiResult

    /**
     * Logout
     * @return identityApiResult
     */
    suspend fun logout(): IdentityApiResult
}

internal interface InternalIdentity : MParticleIdentity {
    /**
     * Identify api request call
     * @param request
     * @return identityApiResult
     */
    suspend fun identify(@NotNull request: IdentityApiRequest): IdentityApiResult

    /**
     * Modify api request call
     * @param request
     * @return identityApiResult
     */
    suspend fun modify(@NotNull request: IdentityApiRequest): IdentityApiResult

    /**
     * Logout api request call
     * @param request
     * @return identityApiResult
     */
    suspend fun logout(@NotNull request: IdentityApiRequest): IdentityApiResult

    /**
     * Login api request call
     * @param request
     * @return identityApiResult
     */
    suspend fun login(@NotNull request: IdentityApiRequest): IdentityApiResult
}

interface IdentityListener