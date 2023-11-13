package com.mparticle

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.mparticle.internal.Logger
import com.mparticle.uploadbatching.BatchUploadingJob

const val UPLOAD_BATCH_JOB = 123

private fun Int.minutesToMillis(): Long = this * 60000L

fun scheduleBatchUploading(context: Context, legacyAction: (delay: Long) -> Unit) {
    val delay = 15.minutesToMillis()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val job =
            JobInfo.Builder(
                UPLOAD_BATCH_JOB,
                ComponentName(context, BatchUploadingJob::class.java.name)
            )
                .setMinimumLatency(delay)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
        context.scheduleJob(job)
    } else {
        legacyAction.invoke(delay)
    }
}


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun Context.scheduleJob(job: JobInfo) {
    val jobScheduler = this.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler?
    val jobRunning = jobScheduler?.allPendingJobs?.firstOrNull { it.id == job.id } != null
    if (!jobRunning) {
        //Schedule / Re-schedule the job if its not running/scheduled to run at the system service
        jobScheduler?.schedule(job)
    } else {
        Logger.debug("Trying to schedule job in uploadBatch service. Service ALREADY RUNNING")
    }
}


