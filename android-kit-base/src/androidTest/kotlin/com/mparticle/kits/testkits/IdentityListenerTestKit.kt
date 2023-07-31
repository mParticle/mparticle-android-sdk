package com.mparticle.kits.testkits

import com.mparticle.identity.MParticleUser
import com.mparticle.kits.FilteredIdentityApiRequest
import com.mparticle.kits.KitIntegration

open class IdentityListenerTestKit : ListenerTestKit(), KitIntegration.IdentityListener {
    var onIdentifyCompleted: ((mParticleUser: MParticleUser?, identityApiRequest: FilteredIdentityApiRequest?) -> Unit)? =
        null
    var onLoginCompleted: ((mParticleUser: MParticleUser?, identityApiRequest: FilteredIdentityApiRequest?) -> Unit)? =
        null
    var onLogoutCompleted: ((mParticleUser: MParticleUser?, identityApiRequest: FilteredIdentityApiRequest?) -> Unit)? =
        null
    var onModifyCompleted: ((mParticleUser: MParticleUser?, identityApiRequest: FilteredIdentityApiRequest?) -> Unit)? =
        null
    var onUserIdentified: ((mParticleUser: MParticleUser?) -> Unit)? = null

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?
    ) {
        onLogoutCompleted?.invoke(mParticleUser, identityApiRequest)
        onUserReceived?.invoke(mParticleUser)
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?
    ) {
        onLoginCompleted?.invoke(mParticleUser, identityApiRequest)
        onUserReceived?.invoke(mParticleUser)
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?
    ) {
        onIdentifyCompleted?.invoke(mParticleUser, identityApiRequest)
        onUserReceived?.invoke(mParticleUser)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?
    ) {
        onModifyCompleted?.invoke(mParticleUser, identityApiRequest)
        onUserReceived?.invoke(mParticleUser)
    }

    override fun onUserIdentified(mParticleUser: MParticleUser?) {
        onUserIdentified?.invoke(mParticleUser)
    }
}
