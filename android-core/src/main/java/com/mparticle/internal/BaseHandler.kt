package com.mparticle.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.mparticle.internal.listeners.InternalListenerManager.Companion.isEnabled
import com.mparticle.internal.listeners.InternalListenerManager.Companion.listener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

open class BaseHandler : Handler {
    @Volatile
    var isDisabled: Boolean = false
        private set

    @Volatile
    private var handling = false
    private val messageQueue: ConcurrentHashMap<Message, Boolean> = ConcurrentHashMap<Message, Boolean>()

    fun getMessageQueue(): Set<Message?> {
        return messageQueue.keys
    }

    constructor()

    constructor(looper: Looper) : super(looper)

    fun disable(disable: Boolean) {
        this.isDisabled = disable
        removeCallbacksAndMessages(null)
        while (handling) {
        }
    }


    fun await(latch: CountDownLatch?) {
        this.sendMessage(obtainMessage(-1, latch))
    }

    override fun handleMessage(msg: Message) {
        if (isDisabled) {
            Logger.error("Handler: " + javaClass.name + " is destroyed! Message: \"" + msg.toString() + "\" will not be processed")
            return
        }
        handling = true
        try {
                messageQueue.remove(msg)
            if ( msg.what == -1 && msg.obj is CountDownLatch) {
                (msg.obj as CountDownLatch).countDown()
            } else {
                if (isEnabled) {
                    listener.onThreadMessage(javaClass.name, msg, true)
                }
                try {
                    handleMessageImpl(msg)
                } catch (error: OutOfMemoryError) {
                    Logger.error("Out of memory")
                }
            }
        } finally {
            handling = false
        }
    }

    override fun sendMessageAtTime(msg: Message, uptimeMillis: Long): Boolean {
        if (isDisabled) {
            return false
        }
        if (isEnabled) {
            listener.onThreadMessage(javaClass.name, msg, false)
        }

            messageQueue[msg] = true

        return super.sendMessageAtTime(msg, uptimeMillis)
    }

    fun removeMessage(what: Int) {
        val messages: Set<Message?> = messageQueue.keys
        for (message in messages) {
            message?.let {
                if (message.what == what) {
                    messageQueue.remove(message)
                }
            }
        }
        super.removeMessages(what)
    }

    //Override this in order to handle messages
    open fun handleMessageImpl(msg: Message?) {
    }
}
