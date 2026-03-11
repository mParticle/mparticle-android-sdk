package com.mparticle.kits.iterable

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class Future<T> private constructor(
    callable: Callable<T>,
) {
    private lateinit var callbackHandler: Handler
    private val successCallbacks: MutableList<SuccessCallback<T>> = ArrayList()
    private val failureCallbacks: MutableList<FailureCallback> = ArrayList()

    private fun handleSuccess(result: T) {
        callbackHandler.post {
            var callbacks: List<SuccessCallback<T>>
            synchronized(successCallbacks) { callbacks = ArrayList(successCallbacks) }
            for (callback: SuccessCallback<T>? in callbacks) {
                callback?.onSuccess(result)
            }
        }
    }

    private fun handleFailure(t: Throwable) {
        callbackHandler.post {
            var callbacks: List<FailureCallback>
            synchronized(failureCallbacks) { callbacks = ArrayList(failureCallbacks) }
            for (callback: FailureCallback? in callbacks) {
                callback?.onFailure(t)
            }
        }
    }

    fun onSuccess(successCallback: SuccessCallback<T>): Future<T> {
        synchronized(successCallbacks) { successCallbacks.add(successCallback) }
        return this
    }

    fun onFailure(failureCallback: FailureCallback): Future<T> {
        synchronized(failureCallbacks) { failureCallbacks.add(failureCallback) }
        return this
    }

    interface SuccessCallback<T> {
        fun onSuccess(result: T)
    }

    interface FailureCallback {
        fun onFailure(throwable: Throwable?)
    }

    companion object {
        private val EXECUTOR = Executors.newCachedThreadPool()

        fun <T> runAsync(callable: Callable<T>): Future<T> = Future(callable)
    }

    init {
        // Set up a Handler for the callback based on the current thread
        var looper = Looper.myLooper()
        looper?.let {
            callbackHandler = Handler((it))
            EXECUTOR.submit {
                try {
                    val result = callable.call()
                    handleSuccess(result)
                } catch (e: Exception) {
                    handleFailure(e)
                }
            } ?: { looper = Looper.getMainLooper() }
        }
    }
}
