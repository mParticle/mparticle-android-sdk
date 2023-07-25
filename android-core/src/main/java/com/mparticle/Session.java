package com.mparticle;

import androidx.annotation.Nullable;

import com.mparticle.internal.MPUtility;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * Public Session API exposing characteristics of a given session.
 */
public class Session {

    private final String mUUID;
    private final Long mSessionStartTime;

    private Session() {
        mUUID = null;
        mSessionStartTime = null;
    }

    Session(String uuid, Long sessionStartTime) {
        this.mUUID = uuid;
        this.mSessionStartTime = sessionStartTime;
    }

    /**
     * Query for the session UUID.
     *
     * @return returns a UUID v4 capitalized string representing the unique session ID.
     */
    @Nullable
    public String getSessionUUID() {
        if (this.mUUID == null) {
            return null;
        } else {
            return this.mUUID.toUpperCase(Locale.US);
        }
    }

    @Nullable
    public Long getSessionStartTime() {
        return mSessionStartTime;
    }

    /**
     * Query for the hashed session ID.
     *
     * @return returns the fnv1a 64-bit hash of the session ID utf-16le bytes.
     */
    public long getSessionID() {
        String sessionUuid = getSessionUUID();
        if (sessionUuid == null) {
            return 0;
        }
        try {
            return MPUtility.hashFnv1A(sessionUuid.getBytes("UTF-16LE")).longValue();
        } catch (UnsupportedEncodingException ignore) {

        }
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Session)) {
            return false;
        }
        String sessionId = getSessionUUID();
        Long sessionStartTime = getSessionStartTime();
        Session comparisonSession = ((Session) obj);
        String comparisonSessionId = comparisonSession.getSessionUUID();
        Long comparisonSessionStartTime = comparisonSession.getSessionStartTime();
        if (sessionId == comparisonSessionId &&
                sessionStartTime == comparisonSessionStartTime
        ) {
            return true;
        }
        if (sessionId == null || sessionStartTime == null) {
            return false;
        }
        return sessionId.equals(comparisonSessionId) && sessionStartTime.equals(comparisonSessionStartTime);
    }
}
