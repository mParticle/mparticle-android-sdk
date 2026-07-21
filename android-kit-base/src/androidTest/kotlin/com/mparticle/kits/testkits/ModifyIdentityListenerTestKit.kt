package com.mparticle.kits.testkits

import com.mparticle.MParticle
import com.mparticle.consent.ConsentState
import com.mparticle.kits.KitIntegration.LogoutListener
import com.mparticle.kits.KitIntegration.ModifyIdentityListener
import com.mparticle.kits.KitIntegration.UserAttributeListener
import com.mparticle.kits.ReportingMessage

open class ModifyIdentityListenerTestKit :
    ListenerTestKit(),
    UserAttributeListener,
    ModifyIdentityListener,
    LogoutListener {
    var setUserAttributeCallback: ((attributeKey: String?, attributeValue: String?) -> Unit)? = null
    var setUserAttributeList: ((attributeKey: String?, attributeValueList: List<String>?) -> Unit)? =
        null
    var supportsAttributeLists: (() -> Boolean)? = null
    var setAllUserAttributes: ((userAttributes: Map<String, String>?, userAttributeLists: Map<String, List<String>>?) -> Unit)? =
        null
    var removeUserAttributeListener: ((key: String?) -> Unit)? = null
    var setUserIdentity: ((identityType: MParticle.IdentityType?, identity: String?) -> Unit)? =
        null
    var removeUserIdentity: ((identityType: MParticle.IdentityType?) -> Unit)? = null
    var logout: (() -> List<ReportingMessage>)? = null

    override fun supportsAttributeLists() = supportsAttributeLists?.invoke() ?: true

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: List<String>?,
    ) {
        setUserAttributeList?.invoke(attributeKey, attributeValueList)
        onAttributeReceived?.invoke(attributeKey, attributeValueList)
    }

    override fun onSetAllUserAttributes(
        userAttributes: Map<String, String>,
        userAttributeLists: Map<String, List<String>>,
    ) {
        setAllUserAttributes?.invoke(userAttributes, userAttributeLists)
        userAttributes.forEach { onAttributeReceived?.invoke(it.key, it.value) }
        userAttributeLists.forEach { onAttributeReceived?.invoke(it.key, it.value) }
    }

    override fun setUserIdentity(
        identityType: MParticle.IdentityType,
        identity: String?,
    ) {
        setUserIdentity?.invoke(identityType, identity)
        onIdentityReceived?.invoke(identityType, identity)
    }

    override fun removeUserIdentity(identityType: MParticle.IdentityType) {
        removeUserIdentity?.invoke(identityType)
        onIdentityReceived?.invoke(identityType, null)
    }

    override fun onRemoveUserAttribute(
        key: String,
    ) {
        removeUserAttributeListener?.invoke(key)
        onAttributeReceived?.invoke(key, null)
    }

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
    ) {
        if (key == null || value == null || value !is String) {
            return
        }
        setUserAttributeCallback?.invoke(key, value)
        onAttributeReceived?.invoke(key, value)
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
    ) {
    }

    override fun onSetUserTag(
        key: String?,
    ) {
    }

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
    ) {
    }

    override fun logout(): List<ReportingMessage> = logout?.invoke() ?: listOf()
}
