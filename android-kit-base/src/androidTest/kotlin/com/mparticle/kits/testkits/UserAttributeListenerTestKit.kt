package com.mparticle.kits.testkits

import com.mparticle.consent.ConsentState
import com.mparticle.kits.KitIntegration

open class UserAttributeListenerTestKit :
    ListenerTestKit(),
    KitIntegration.UserAttributeListener {
    var onIncrementUserAttribute: ((key: String?, incrementedBy: Number, value: String?) -> Unit)? =
        null
    var onRemoveUserAttribute: ((key: String?) -> Unit)? = null
    var onSetUserAttribute: ((key: String?, value: Any?) -> Unit)? =
        null
    var onSetUserTag: ((key: String?) -> Unit)? = null
    var onSetUserAttributeList: ((attributeKey: String?, attributeValueList: List<String>?) -> Unit)? =
        null
    var onSetAllUserAttributes: (
        (
            userAttributes: Map<String?, String?>?,
            userAttributeLists: Map<String?, List<String?>?>?,
        ) -> Unit
    )? = null
    var supportsAttributeLists: (() -> Boolean)? = null
    var onConsentStateUpdated: ((oldState: ConsentState?, newState: ConsentState?) -> Unit)? =
        null

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
    ) {
        onConsentStateUpdated?.invoke(oldState, newState)
    }

    override fun onSetAllUserAttributes(
        userAttributes: Map<String?, String?>?,
        userAttributeLists: Map<String?, List<String>>?,
    ) {
        onSetAllUserAttributes?.invoke(userAttributes, userAttributeLists)
        userAttributes?.forEach { onAttributeReceived?.invoke(it.key, it.value) }
    }

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
    ) {
        onSetUserAttribute?.invoke(key, value)
        onAttributeReceived?.invoke(key, value)
        onUserReceived?.invoke(null)
    }

    override fun onSetUserTag(
        key: String?,
    ) {
        onSetUserTag?.invoke(key)
        onAttributeReceived?.invoke(key, null)
        onUserReceived?.invoke(null)
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number,
        value: String?,
    ) {
        onIncrementUserAttribute?.invoke(key, incrementedBy, value)
        onAttributeReceived?.invoke(key, value)
        onUserReceived?.invoke(null)
    }

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: List<String>?,
    ) {
        onSetUserAttributeList?.invoke(attributeKey, attributeValueList)
        onAttributeReceived?.invoke(attributeKey, attributeValueList)
        onUserReceived?.invoke(null)
    }

    override fun onRemoveUserAttribute(
        key: String?,
    ) {
        onRemoveUserAttribute?.invoke(key)
        onAttributeReceived?.invoke(key, null)
        onUserReceived?.invoke(null)
    }

    override fun supportsAttributeLists() = supportsAttributeLists?.invoke() ?: true
}
