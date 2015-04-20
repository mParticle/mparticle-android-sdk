package com.mparticle.internal;

import com.mparticle.MParticle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class Session {
    public int mEventCount = 0;
    public String mSessionID = Constants.NO_SESSION_ID;
    public long mSessionStartTime = 0;
    public long mLastEventTime = 0;
    private long mTimeInBackground = 0;
    public JSONObject mSessionAttributes = new JSONObject();


    public Session() {
        super();
    }


    public Session(Session session) {
        super();
        mEventCount = session.mEventCount;
        mSessionID = session.mSessionID;
        mSessionStartTime = session.mSessionStartTime;
        mLastEventTime = session.mLastEventTime;
        mTimeInBackground = session.mTimeInBackground;
        try {
            mSessionAttributes = new JSONObject(session.mSessionAttributes.toString());
        }catch (JSONException jse){

        }
    }

    public boolean isActive() {
        return mSessionStartTime > 0 && !Constants.NO_SESSION_ID.equals(mSessionID);
    }

    public Session start() {
        mLastEventTime = mSessionStartTime = System.currentTimeMillis();
        mSessionID = UUID.randomUUID().toString();
        mSessionAttributes = new JSONObject();
        mEventCount = 0;
        mTimeInBackground = 0;
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

    public long getLength() {
        long time = mLastEventTime - mSessionStartTime;
        if (time >= 0){
            return time;
        }
        return time;
    }

    public long getBackgroundTime() {
        return mTimeInBackground;
    }

    public long getForegroundTime() {
        return getLength() - getBackgroundTime();
    }

    public void updateBackgroundTime(AtomicLong lastStoppedTime, long currentTime) {
        long time = lastStoppedTime.get();
        if (time >= mSessionStartTime){
            mTimeInBackground += (currentTime - time);
        }
    }
}
