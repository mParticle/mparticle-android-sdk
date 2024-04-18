package com.mparticle.identity.audience

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BaseAudienceTask : AudienceTask<AudienceResponse>() {

    private var isCompleted: Boolean = false
    private var isSuccessful: Boolean = false
    private var result: AudienceResponse? = null
    private var successListeners: MutableSet<AudienceTaskSuccessListener> = java.util.HashSet()
    private var failureListeners: MutableSet<AudienceTaskFailureListener> = HashSet()

    fun setFailed(errorResponse: AudienceResponse) {
        isCompleted = true
        isSuccessful = false
        CoroutineScope(Dispatchers.Main).launch {
            for (listener in failureListeners) {
                listener.onFailure(errorResponse)
            }
        }
    }

    fun setSuccessful(successResponse: AudienceResponse) {
        isCompleted = true
        isSuccessful = true
        CoroutineScope(Dispatchers.Main).launch {
            for (listener in successListeners) {
                listener.onSuccess(successResponse)
            }
            println(successListeners.size)
        }
    }

    override fun isComplete(): Boolean {
        return isCompleted
    }

    override fun isSuccessful(): Boolean {
        return isSuccessful
    }

    override fun getResult(): AudienceResponse? {
        return result
    }

    override fun addSuccessListener(listener: AudienceTaskSuccessListener): BaseAudienceTask {
        if (listener != null) {
            successListeners.add(listener)
        }
        return this
    }

    override fun addFailureListener(listener: AudienceTaskFailureListener): BaseAudienceTask {
        if (listener != null) {
            failureListeners.add(listener)
        }
        return this
    }


}

