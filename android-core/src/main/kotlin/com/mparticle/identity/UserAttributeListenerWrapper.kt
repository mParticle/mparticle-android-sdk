package com.mparticle.identity

import com.mparticle.TypedUserAttributeListener
import com.mparticle.UserAttributeListener
import com.mparticle.UserAttributeListenerType

class UserAttributeListenerWrapper(val listener: UserAttributeListenerType) {
    fun onUserAttributesReceived(
        singles: Map<String, Any?>?,
        lists: Map<String, List<String?>?>?,
        mpid: Long?
    ) {
        when (listener) {
            is UserAttributeListener -> (singles ?: mutableMapOf<String, String?>())
                .entries
                .associate { it.key to it.value?.toString() }
                .let { listener.onUserAttributesReceived(it, lists, mpid) }
            is TypedUserAttributeListener -> mpid?.let {
                listener.onUserAttributesReceived(
                    singles ?: mutableMapOf(), lists ?: mutableMapOf(), it
                )
            }
        }
    }
}