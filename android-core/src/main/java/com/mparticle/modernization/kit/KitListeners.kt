package com.mparticle.modernization.kit

import android.content.Context
import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent

/**
 * Listeners for the kits
 */
interface CommerceListener {
    fun commerceEventLogged(event : CommerceEvent)
//    fun ltvIncreaseEventLogged(event : LtvIncreaseEvent)
}
interface EventListener{
    fun eventLogged(event : MPEvent)
    fun errorLogged(message: String, params: Map<String, String>?, exception: Exception?)
    //breadcrumbLogged, screen logged, etc.
}
interface MPLifecycle {
    fun onKitCreate(settings : Map<String, String>, context : Context) {}
}
interface IdentityListener {}
interface UserProfileListener {}
interface PushListener {}
interface ActivityListener{}
