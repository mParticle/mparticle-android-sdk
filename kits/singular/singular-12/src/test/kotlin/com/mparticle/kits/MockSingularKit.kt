package com.mparticle.kits

import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import org.json.JSONException
import org.json.JSONObject

class MockSingularKit : SingularKit() {
    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> =
        mutableListOf(
            ReportingMessage(
                this,
                commerceEvent.eventName,
                System.currentTimeMillis(),
                HashMap<String, String>(),
            ),
        )

    override fun logEvent(mpEvent: MPEvent): List<ReportingMessage> =
        mutableListOf(
            ReportingMessage(
                this,
                mpEvent.eventType.toString(),
                System.currentTimeMillis(),
                HashMap<String, String>(),
            ),
        )

    override fun getConfiguration(): KitConfiguration? {
        try {
            return KitConfiguration.createKitConfiguration(
                JSONObject().put(
                    "id",
                    MParticle.ServiceProviders.SINGULAR,
                ),
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }
}
