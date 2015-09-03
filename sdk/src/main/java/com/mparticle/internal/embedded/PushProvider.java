package com.mparticle.internal.embedded;

import android.content.Intent;

import java.util.List;

public interface PushProvider {
    List<ReportingMessage> handleGcmMessage(Intent intent);
}
