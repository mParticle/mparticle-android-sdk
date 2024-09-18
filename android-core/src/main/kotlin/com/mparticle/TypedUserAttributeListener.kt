package com.mparticle

interface TypedUserAttributeListener : UserAttributeListenerType {
    fun onUserAttributesReceived(
        userAttributes: Map<String, Any?>,
        userAttributeLists: Map<String, List<String?>?>,
        mpid: Long
    )
}