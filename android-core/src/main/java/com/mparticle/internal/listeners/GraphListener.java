package com.mparticle.internal.listeners;

import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface GraphListener {
    void onCompositeObjects(@Nullable Object child, @Nullable Object parent);
    void onThreadMessage(@NonNull String handlerName, @Nullable Message msg, boolean onNewThread, @Nullable StackTraceElement[] stackTrace);
}
