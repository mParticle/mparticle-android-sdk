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

enum class SchedulingBatchingType { PERIODIC, ONE_SHOT; }

fun cancelScheduledUploadBatchJob(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val jobScheduler = context.getJobScheduler()
            val jobRunning = context.getScheduledJob(UPLOAD_BATCH_JOB)
            jobRunning?.let { jobScheduler?.cancel(UPLOAD_BATCH_JOB) }
        }
    } catch (e: Exception) {
    }
}

fun scheduleBatchUploading(
    context: Context,
    delayInMillis: Long,
    type: SchedulingBatchingType,
    legacyAction: (delay: Long) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        try {
            val jobId = UPLOAD_BATCH_JOB
            val jobRunning = context.getScheduledJob(jobId) != null

            if (!jobRunning) {
                val builder = JobInfo.Builder(
                    jobId,
                    ComponentName(context, BatchUploadingJob::class.java.name)
                )
                if (type == SchedulingBatchingType.PERIODIC) {
                    builder.setPeriodic(delayInMillis)
                } else if (type == SchedulingBatchingType.ONE_SHOT) {
                    builder.setMinimumLatency(delayInMillis)
                }
                builder.apply {
                    setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                }
                context.scheduleJob(builder.build())
                Logger.debug("Scheduling batch upload with interval ${delayInMillis/1000} sec")
            } else {
                Logger.debug("Trying to schedule job in uploadBatch service. Service ALREADY RUNNING")
            }
        } catch (e: Exception) {
            Logger.warning("Service ${BatchUploadingJob::class.java.name} should be added to the manifest")
            legacyAction.invoke(delayInMillis)
        }
    } else {
        Logger.debug("Sending post delayed message for batch uploading")
        legacyAction.invoke(delayInMillis)
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun Context.getJobScheduler(): JobScheduler? =
    this.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler?

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun Context.getScheduledJob(jobId: Int): JobInfo? =
    this.getJobScheduler()?.allPendingJobs?.firstOrNull { it.id == jobId }

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun Context.scheduleJob(job: JobInfo) {
    //Schedule / Re-schedule the job if its not running/scheduled to run at the system service
    this.getJobScheduler()?.schedule(job)
}


