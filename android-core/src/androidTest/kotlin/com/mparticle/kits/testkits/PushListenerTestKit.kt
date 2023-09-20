package com.mparticle.kits.testkits

import android.content.Context
import android.content.Intent
import com.mparticle.kits.KitIntegration

class PushListenerTestKit : BaseTestKit(), KitIntegration.PushListener {
    var onPushMessageReceived: (Context, Intent?) -> Unit = { _, _ -> }
    var onPushRegistration: (String?, String?) -> Boolean = { _, _ -> false }
    override fun willHandlePushMessage(intent: Intent?) = true

    override fun onPushMessageReceived(context: Context, pushIntent: Intent?) {
        onPushMessageReceived.invoke(context, pushIntent)
    }

    override fun onPushRegistration(instanceId: String?, senderId: String?): Boolean {
        return onPushRegistration.invoke(instanceId, senderId)
    }
}
