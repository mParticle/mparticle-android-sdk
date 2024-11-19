package com.mparticle.internal.listeners

import android.os.Message

interface GraphListener {
    fun onCompositeObjects(child: Any?, parent: Any?)

    fun onThreadMessage(
        handlerName: String,
        msg: Message?,
        onNewThread: Boolean,
        stackTrace: Array<StackTraceElement?>?
    )
}
