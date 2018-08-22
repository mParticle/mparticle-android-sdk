package com.mparticle;

import com.mparticle.internal.MPUtility;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * Public Session API exposing characteristics of a given session.
 *
 */
public class Session {

    private final String mUUID;

    private Session() {
        mUUID = null;
    }
    Session(String uuid) {
        this.mUUID = uuid;
    }

    /**
     * Query for the session UUID.
     *
     * @return returns a UUID v4 capitalized string representing the unique session ID.
     */
    public String getSessionUUID() {
        if (this.mUUID == null) {
            return null;
        } else {
            return this.mUUID.toUpperCase(Locale.US);
        }
    }

    /**
     * Query for the hashed session ID
     *
     * @return returns the fnv1a 64-bit hash of the session ID utf-16le bytes.
     */
    long getSessionID() {
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
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Session)) {
            return false;
        }
        String sessionId = getSessionUUID();
        String comparisonSessionId = ((Session) obj).getSessionUUID();
        if (sessionId == null && comparisonSessionId == null) {
            return true;
        }
        if (sessionId == null || comparisonSessionId == null) {
            return false;
        }
        return sessionId.equals(comparisonSessionId);
    }
}
