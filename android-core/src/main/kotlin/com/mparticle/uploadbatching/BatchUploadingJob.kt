package com.mparticle.uploadbatching

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
import com.mparticle.MParticle
import com.mparticle.internal.Logger

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BatchUploadingJob : JobService() {

    // Whenever the contraints are satisfied this will get fired.
    override fun onStartJob(params: JobParameters?): Boolean {
        Logger.debug("uploadBatching onStart service ")
        MParticle.getInstance()?.let {
            //Do if there is a non-null mParticle instance, force upload messages
            it.upload()
            Logger.debug("Triggering event upload on uploadBatching service")
        } ?: run {
            Logger.debug("MParticle instance null while trying to call uploadBatching:upload")
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}