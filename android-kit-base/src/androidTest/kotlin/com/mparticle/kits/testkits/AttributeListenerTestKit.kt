package com.mparticle.kits.testkits

import com.mparticle.MParticle
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.ReportingMessage

open class AttributeListenerTestKit : ListenerTestKit(), KitIntegration.AttributeListener {
    var setUserAttribute: ((attributeKey: String?, attributeValue: String?) -> Unit)? = null
    var setUserAttributeList: ((attributeKey: String?, attributeValueList: List<String?>?) -> Unit)? =
        null
    var supportsAttributeLists: (() -> Boolean)? = null
    var setAllUserAttributes: ((userAttributes: Map<String, String>?, userAttributeLists: Map<String, List<String>>?) -> Unit)? =
        null
    var removeUserAttribute: ((key: String?) -> Unit)? = null
    var setUserIdentity: ((identityType: MParticle.IdentityType?, identity: String?) -> Unit)? =
        null
    var removeUserIdentity: ((identityType: MParticle.IdentityType?) -> Unit)? = null
    var logout: (() -> List<ReportingMessage>)? = null

    override fun supportsAttributeLists() = supportsAttributeLists?.invoke() ?: true

    override fun setUserAttributeList(
        attributeKey: String,
        attributeValueList: MutableList<String>
    ) {
        setUserAttributeList?.invoke(attributeKey, attributeValueList)
        onAttributeReceived?.invoke(attributeKey, attributeValueList)
    }

    override fun setAllUserAttributes(
        userAttributes: Map<String, String>,
        userAttributeLists: Map<String, MutableList<String>>
    ) {
        setAllUserAttributes?.invoke(userAttributes, userAttributeLists)
        userAttributes.forEach { onAttributeReceived?.invoke(it.key, it.value) }
        userAttributeLists.forEach { onAttributeReceived?.invoke(it.key, it.value) }
    }

    override fun setUserAttribute(attributeKey: String, attributeValue: String?) {
        setUserAttribute?.invoke(attributeKey, attributeValue)
        onAttributeReceived?.invoke(attributeKey, attributeValue)
    }

    override fun setUserIdentity(identityType: MParticle.IdentityType, identity: String?) {
        setUserIdentity?.invoke(identityType, identity)
        onIdentityReceived?.invoke(identityType, identity)
    }

    override fun removeUserIdentity(identityType: MParticle.IdentityType) {
        removeUserIdentity?.invoke(identityType)
        onIdentityReceived?.invoke(identityType, null)
    }

    override fun removeUserAttribute(key: String) {
        removeUserAttribute?.invoke(key)
        onAttributeReceived?.invoke(key, null)
    }

    override fun logout(): List<ReportingMessage> {
        return logout?.invoke() ?: listOf()
    }
}
