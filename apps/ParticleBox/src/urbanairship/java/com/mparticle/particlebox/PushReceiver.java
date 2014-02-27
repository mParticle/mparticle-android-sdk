package com.mparticle.particlebox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mparticle.MParticle;
import com.urbanairship.push.PushManager;

/**
 * Created by sdozor on 2/13/14.
 */
public class PushReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(MParticle.Push.BROADCAST_NOTIFICATION_TAPPED)){
            Log.d("Particlebox", "mParticle Tap detected");
        }else if (intent.getAction().equals(MParticle.Push.BROADCAST_NOTIFICATION_RECEIVED)){
            Log.d("Particlebox", "mParticle Notification detected");
        }else if (intent.getAction().equals(PushManager.ACTION_NOTIFICATION_OPENED)){
            Log.d("Particlebox", "UA Tap detected");
        }
    }
}
