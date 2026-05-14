package com.mparticle.modernization.kit.example

import com.mparticle.modernization.core.MParticleMediator
import com.mparticle.modernization.kit.MParticleKitInternal
import com.mparticle.modernization.launch
import com.mparticle.modernization.uploading.example.MParticleUploaderTypes

internal class MpKit(private val mediator: MParticleMediator) : MParticleKitInternal() {

    override fun leaveBreadcrumb(breadcrumb: String) {
        mediator.launch {
            mediator.dataUploader?.upload(breadcrumb, MParticleUploaderTypes.BREADCRUMB.type)
        }
    }
}
