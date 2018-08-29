package com.mparticle.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import java.util.concurrent.CountDownLatch;

public class BaseHandler extends Handler {
    private volatile boolean disabled;
    private volatile boolean handling;

    public BaseHandler() {}

    public BaseHandler(Looper looper) {
        super(looper);
    }

    public void disable(boolean disable) {
        this.disabled = disable;
        removeCallbacksAndMessages(null);
        while (handling) {}
    }

    public boolean isDisabled() {
        return disabled;
    }

    void await(CountDownLatch latch) {
        this.sendMessage(obtainMessage(-1, latch));
    }

    @Override
    public final void handleMessage(Message msg) {
        if (disabled) {
            return;
        }
        handling = true;
        try {
            if (msg != null && msg.what == -1 && msg.obj instanceof CountDownLatch) {
                ((CountDownLatch)msg.obj).countDown();
            } else {
                handleMessageImpl(msg);
            }
        }
        finally {
            handling = false;
        }
    }

    @Override
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        if (disabled) {
            return false;
        }
        return super.sendMessageAtTime(msg, uptimeMillis);
    }

    //Override this in order to handle messages
    public void handleMessageImpl(Message msg) {}
}
