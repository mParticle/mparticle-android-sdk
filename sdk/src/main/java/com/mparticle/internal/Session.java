package com.mparticle.internal;

import com.mparticle.ConfigManager;
import com.mparticle.MParticle;

import java.util.UUID;

/**
 * Created by sdozor on 3/30/15.
 */
public class Session {
    public int mEventCount = 0;
    public String mSessionID = Constants.NO_SESSION_ID;
    public long mSessionStartTime = 0;
    public long mLastEventTime = 0;

    public boolean isActive() {
        return mSessionStartTime > 0 && !Constants.NO_SESSION_ID.equals(mSessionID);
    }

    public Session start() {
        mLastEventTime = mSessionStartTime = System.currentTimeMillis();
        mSessionID = UUID.randomUUID().toString();
        mEventCount = 0;
        return this;
    }

    public Boolean checkEventLimit() {
        if (mEventCount < Constants.EVENT_LIMIT) {
            mEventCount++;
            return true;
        } else {
            ConfigManager.log(MParticle.LogLevel.WARNING, "The event limit has been exceeded for this session.");
            return false;
        }
    }

    public boolean isTimedOut(int sessionTimeout) {
        return sessionTimeout < (System.currentTimeMillis() - mLastEventTime);
    }
}
