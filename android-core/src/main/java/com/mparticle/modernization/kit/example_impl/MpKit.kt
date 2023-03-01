package com.mparticle.modernization.kit.example_impl

import com.mparticle.modernization.core.MParticleMediator
import com.mparticle.modernization.data_uploader.example_impl.MParticleUploaderTypes
import com.mparticle.modernization.kit.MParticleKitInternal
import com.mparticle.modernization.launch

class MpKit(private val mediator: MParticleMediator) : MParticleKitInternal() {

    override fun leaveBreadcrumb(breadcrumb: String) {
        mediator.launch {
            mediator.dataUploader?.upload(breadcrumb, MParticleUploaderTypes.BREADCRUMB.type)
        }
    }

}