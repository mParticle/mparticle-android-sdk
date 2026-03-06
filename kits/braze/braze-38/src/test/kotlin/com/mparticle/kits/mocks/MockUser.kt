package com.mparticle.kits.mocks

import com.mparticle.MParticle.IdentityType
import com.mparticle.UserAttributeListenerType
import com.mparticle.audience.AudienceResponse
import com.mparticle.audience.AudienceTask
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser

class MockUser(
    var identities: Map<IdentityType, String>,
) : MParticleUser {
    override fun getId(): Long = 0

    override fun getUserAttributes(): Map<String, Any> = mapOf()

    override fun getUserAttributes(p0: UserAttributeListenerType?): MutableMap<String, Any>? = null

    override fun setUserAttributes(map: Map<String, Any>): Boolean = false

    override fun getUserIdentities(): Map<IdentityType, String> = identities

    override fun setUserAttribute(
        s: String,
        o: Any,
    ): Boolean = false

    override fun setUserAttributeList(
        s: String,
        o: Any,
    ): Boolean = false

    override fun incrementUserAttribute(
        p0: String,
        p1: Number?,
    ): Boolean = false

    override fun removeUserAttribute(s: String): Boolean = false

    override fun setUserTag(s: String): Boolean = false

    override fun getConsentState(): ConsentState = consentState

    override fun setConsentState(consentState: ConsentState?) {}

    override fun isLoggedIn(): Boolean = false

    override fun getFirstSeenTime(): Long = 0

    override fun getLastSeenTime(): Long = 0

    override fun getUserAudiences(): AudienceTask<AudienceResponse> = throw NotImplementedError("getUserAudiences() is not implemented")
}
