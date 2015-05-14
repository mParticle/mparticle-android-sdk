package com.mparticle.extended;

import android.app.Activity;

import com.appboy.Appboy;
import com.appboy.ui.inappmessage.AppboyInAppMessageManager;
import com.mparticle.internal.embedded.EmbeddedAppboy;
import com.mparticle.internal.embedded.EmbeddedKitManager;

public class AppboyExtended extends EmbeddedAppboy {
    private boolean mRefreshData;

    public AppboyExtended(EmbeddedKitManager ekManager) {
        super(ekManager);
    }

    @Override
    public void onActivityResumed(Activity activity, int activityCount) {
        super.onActivityResumed(activity, activityCount);
        AppboyInAppMessageManager.getInstance().registerInAppMessageManager(activity);
        if (mRefreshData) {
            Appboy.getInstance(activity).requestInAppMessageRefresh();
            mRefreshData = false;
        }
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {
        super.onActivityPaused(activity, activityCount);
        AppboyInAppMessageManager.getInstance().unregisterInAppMessageManager(activity);
    }

    @Override
    public void onActivityStarted(Activity activity, int activityCount) {
        super.onActivityStarted(activity, activityCount);
        mRefreshData = true;
    }
}
