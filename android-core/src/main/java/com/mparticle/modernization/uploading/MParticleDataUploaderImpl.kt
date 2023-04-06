package com.mparticle.modernization.uploading

import com.mparticle.modernization.config.UploadingConfiguration
import com.mparticle.modernization.core.MParticleMediator
import com.mparticle.modernization.launch

 internal class MParticleDataUploaderImpl(
    private val mediator: MParticleMediator,
    private val strategies: List<MParticleUploadingStrategy>,
    private val uploadingConfiguration: UploadingConfiguration? = null
) : MParticleDataUploader {
    init {
        mediator.launch { configure() }
    }

    override suspend fun upload(data: Any, type: Int, immediateUpload: Boolean) {
        type.getStrategy()?.upload(data, immediateUpload, uploadingConfiguration)
    }

    override suspend fun configure() {
        // setup uploading every X based on uploadingConfiguration rules and server settings we might inject
    }

    private fun Int.getStrategy(): MParticleUploadingStrategy? =
        strategies.firstOrNull { it.type() == this }
}
