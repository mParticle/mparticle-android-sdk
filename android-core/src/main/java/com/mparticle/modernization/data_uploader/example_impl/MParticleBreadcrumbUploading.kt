package com.mparticle.modernization.data_uploader.example_impl

import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.modernization.config.UploadingConfiguration
import com.mparticle.modernization.data.MParticleDataRepository
import com.mparticle.modernization.data_uploader.MParticleUploadingStrategy

class MParticleBreadcrumbUploading(private val dataRepository: MParticleDataRepository) :
    MParticleUploadingStrategy {
    override suspend fun upload(
        data: Any,
        immediateUpload: Boolean,
        uploadingConfiguration: UploadingConfiguration?
    ) {
        data.convert()?.let {
            with(dataRepository) {
                updateSession(it)
                it.addBreadcurmbData()?.let { insertBreadcrumb(it) }
            }
        }
        if (immediateUpload) { sendData() }
    }

    private suspend fun sendData() {}

    private fun BaseMPMessage?.addBreadcurmbData(): BaseMPMessage? = null
    private fun Any.convert(): BaseMPMessage? {
//        val message = BaseMPMessage.Builder(Constants.MessageType.BREADCRUMB).
//            .timestamp(mAppStateManager.getSession().mLastEventTime)
//            .build(mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
//
//        message.put(MessageKey.EVENT_START_TIME, mAppStateManager.getSession().mLastEventTime)
//        message.put(
//            MessageKey.BREADCRUMB_SESSION_COUNTER,
//            mConfigManager.getUserStorage().getCurrentSessionCounter()
//        )
//        message.put(MessageKey.BREADCRUMB_LABEL, breadcrumb)
        return null
    }

    override fun type(): Int = MParticleUploaderTypes.BREADCRUMB.type
}
