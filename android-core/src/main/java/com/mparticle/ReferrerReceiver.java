package com.mparticle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;


/**
 * <code>BroadcastReceiver</code> required to capture attribution data via the Google Play install referrer broadcast.
 *
 *
 * When Google Play installs an application it will broadcast an <code>Intent</code> with the <code>com.android.vending.INSTALL_REFERRER</code> action.
 * From this <code>Intent</code>, mParticle will extract any available referral data for use in measuring the success of advertising or install campaigns.
 *
 *
 * This {@code BroadcastReceiver} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 *
 *
 * <pre>
 * {@code
 *  <receiver
 *      android:name="com.mparticle.ReferrerReceiver"
 *      android:exported="true">
 *      <intent-filter>
 *          <action android:name="com.android.vending.INSTALL_REFERRER"/>
 *      </intent-filter>
 *
 * </receiver> }</pre>
 *
 */
public class ReferrerReceiver extends BroadcastReceiver {
    
    @Override
    public final void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
        setInstallReferrer(context, intent);
    }

    static void setInstallReferrer(Context context, Intent intent) {
        if (context == null) {
            Logger.error("ReferrerReceiver Context can not be null.");
            return;
        }
        if (intent == null) {
            Logger.error("ReferrerReceiver intent can not be null.");
            return;
        }
        if ("com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
            String installReferrer = intent.getStringExtra(Constants.REFERRER);
            InstallReferrerHelper.setInstallReferrer(context, installReferrer);
        }
    }

    @Nullable
    public static Intent getMockInstallReferrerIntent(@NonNull String referrer) {
        if (!MPUtility.isEmpty(referrer)) {
            Intent fakeReferralIntent = new Intent("com.android.vending.INSTALL_REFERRER");
            fakeReferralIntent.putExtra(com.mparticle.internal.Constants.REFERRER, referrer);
            return fakeReferralIntent;
        } else {
            return null;
        }
    }

}
