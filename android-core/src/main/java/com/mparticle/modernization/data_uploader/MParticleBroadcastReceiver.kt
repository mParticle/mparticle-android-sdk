package com.mparticle.modernization.data_uploader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mparticle.modernization.MParticle

class MParticleBroadcastReceiver : BroadcastReceiver(){
    val uploader : MParticleDataUploader? = MParticle.getInstance().DataUploading()

    override fun onReceive(context: Context?, intent: Intent?) {
      
    }
}