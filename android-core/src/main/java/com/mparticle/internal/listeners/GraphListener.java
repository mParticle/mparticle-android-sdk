package com.mparticle.internal.listeners;

import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.SdkListener;

interface GraphListener extends SdkListener {
    void onCompositeObjects(@Nullable Object child, @Nullable Object parent);
    void onThreadMessage(@NonNull String handlerName, @Nullable Message msg, boolean onNewThread, @Nullable StackTraceElement[] stackTrace);
}
