package com.mparticle.internal.embedded;

import android.app.Activity;

import java.util.List;

public interface ActivityLifecycleForwarder {
    List<ReportingMessage> onActivityCreated(Activity activity, int activityCount);
    List<ReportingMessage> onActivityResumed(Activity activity, int activityCount);
    List<ReportingMessage> onActivityPaused(Activity activity, int activityCount);
    List<ReportingMessage> onActivityStopped(Activity activity, int activityCount);
    List<ReportingMessage> onActivityStarted(Activity activity, int activityCount);
}
