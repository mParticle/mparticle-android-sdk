package com.mparticle.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

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

    @Override
    public final void handleMessage(Message msg) {
        if (disabled) {
            return;
        }
        handling = true;
        try {
            handleMessageImpl(msg);
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
