package com.mparticle.internal

import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.messages.BaseMPMessage

interface MessageManagerCallbacks {
    @get:Throws(MParticleApiClientImpl.MPNoConfigException::class)
    val apiKey: String?

    @get:Throws(MParticleApiClientImpl.MPNoConfigException::class)
    val uploadSettings: UploadSettings?

    fun delayedStart()

    fun endUploadLoop()

    fun checkForTrigger(message: BaseMPMessage?)

    fun logNotification(contentId: Int, payload: String?, appState: String?, behavior: Int)

    fun attributeRemoved(key: String?, mpId: Long)

    fun logUserAttributeChangeMessage(
        userAttributeKey: String?,
        newValue: Any?,
        oldValue: Any?,
        deleted: Boolean,
        isNewAttribute: Boolean,
        time: Long,
        mpId: Long,
    ): BaseMPMessage?

    val deviceAttributes: DeviceAttributes?

    fun messagesClearedForUpload()
}
