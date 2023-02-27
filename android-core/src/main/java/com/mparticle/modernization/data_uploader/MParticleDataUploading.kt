package com.mparticle.modernization.data_uploader

import com.mparticle.modernization.MParticleComponent

interface MParticleDataUploading : MParticleComponent {

    fun upload() {}
}