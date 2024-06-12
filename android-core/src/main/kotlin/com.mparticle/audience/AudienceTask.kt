package com.mparticle.audience


abstract class AudienceTask<AudienceTaskResult> {

    abstract fun isComplete(): Boolean

    abstract fun isSuccessful(): Boolean

    abstract fun getResult(): AudienceTaskResult?

    abstract fun addSuccessListener(listener: AudienceTaskSuccessListener): AudienceTask<AudienceTaskResult>

    abstract fun addFailureListener(listener: AudienceTaskFailureListener): AudienceTask<AudienceTaskResult>
}