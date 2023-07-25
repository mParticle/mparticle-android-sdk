package com.mparticle.kits.testkits

import com.mparticle.consent.ConsentState
import com.mparticle.kits.FilteredMParticleUser
import com.mparticle.kits.KitIntegration

open class UserAttributeListenerTestKit : ListenerTestKit(), KitIntegration.UserAttributeListener {
    var onIncrementUserAttribute: ((key: String?, incrementedBy: Number, value: String?, user: FilteredMParticleUser?) -> Unit)? =
        null
    var onRemoveUserAttribute: ((key: String?, user: FilteredMParticleUser?) -> Unit)? = null
    var onSetUserAttribute: ((key: String?, value: Any?, user: FilteredMParticleUser?) -> Unit)? =
        null
    var onSetUserTag: ((key: String?, user: FilteredMParticleUser?) -> Unit)? = null
    var onSetUserAttributeList: ((attributeKey: String?, attributeValueList: List<String?>?, user: FilteredMParticleUser?) -> Unit)? =
        null
    var onSetAllUserAttributes: ((userAttributes: Map<String?, String?>?, userAttributeLists: Map<String?, List<String?>?>?, user: FilteredMParticleUser?) -> Unit)? =
        null
    var supportsAttributeLists: (() -> Boolean)? = null
    var onConsentStateUpdated: ((oldState: ConsentState?, newState: ConsentState?, user: FilteredMParticleUser?) -> Unit)? =
        null

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        user: FilteredMParticleUser?
    ) {
        onConsentStateUpdated?.invoke(oldState, newState, user)
    }

    override fun onSetAllUserAttributes(
        userAttributes: Map<String?, String?>?,
        userAttributeLists: Map<String?, List<String>>?,
        user: FilteredMParticleUser?
    ) {
        onSetAllUserAttributes?.invoke(userAttributes, userAttributeLists, user)
        userAttributes?.forEach { onAttributeReceived?.invoke(it.key, it.value) }
    }

    override fun onSetUserAttribute(key: String?, value: Any?, user: FilteredMParticleUser?) {
        onSetUserAttribute?.invoke(key, value, user)
        onAttributeReceived?.invoke(key, value)
        onUserReceived?.invoke(user)
    }

    override fun onSetUserTag(key: String?, user: FilteredMParticleUser?) {
        onSetUserTag?.invoke(key, user)
        onAttributeReceived?.invoke(key, null)
        onUserReceived?.invoke(user)
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number,
        value: String?,
        user: FilteredMParticleUser?
    ) {
        onIncrementUserAttribute?.invoke(key, incrementedBy, value, user)
        onAttributeReceived?.invoke(key, value)
        onUserReceived?.invoke(user)
    }

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: MutableList<String>?,
        user: FilteredMParticleUser?
    ) {
        onSetUserAttributeList?.invoke(attributeKey, attributeValueList, user)
        onAttributeReceived?.invoke(attributeKey, attributeValueList)
        onUserReceived?.invoke(user)
    }

    override fun onRemoveUserAttribute(key: String?, user: FilteredMParticleUser?) {
        onRemoveUserAttribute?.invoke(key, user)
        onAttributeReceived?.invoke(key, null)
        onUserReceived?.invoke(user)
    }

    override fun supportsAttributeLists() = supportsAttributeLists?.invoke() ?: true
}
