package com.mparticle.modernization.data_uploader

import com.mparticle.modernization.MParticleComponent

interface MParticleDataUploader : MParticleComponent {

    fun upload() {}
}