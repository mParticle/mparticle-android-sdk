package com.mparticle.kits.testkits

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.ReportingMessage

abstract class ListenerTestKit : BaseTestKit() {
    var onUserReceived: ((MParticleUser?) -> Unit)? = null
    var onIdentityReceived: ((MParticle.IdentityType, String?) -> Unit)? = null
    var onAttributeReceived: ((String?, Any?) -> Unit)? = null

    var onKitCreate: ((settings: Map<String, String>?, context: Context) -> List<ReportingMessage>)? =
        null
    var setOptOut: ((optedOut: Boolean) -> List<ReportingMessage>)? = null
    var getName: (() -> String)? = null

    override fun getName() = getName?.invoke() ?: "Test Kit thing"
    override fun setOptOut(optedOut: Boolean) = setOptOut?.invoke(optedOut)
        ?: listOf()

    override fun onKitCreate(
        settings: Map<String, String>?,
        context: Context
    ): List<ReportingMessage> = onKitCreate?.invoke(settings, context)
        ?: listOf()
}
