package com.mparticle.internal.embedded;

import android.content.Intent;

import com.mparticle.internal.ReportingMessage;
import com.mparticle.messaging.AbstractCloudMessage;

import java.util.List;

public interface PushProvider {
    List<ReportingMessage> handleGcmMessage(Intent intent);
}
