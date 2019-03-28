package com.mparticle;

import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.InternalSession;

import org.json.JSONObject;

import java.util.List;

public interface SdkListener {

    enum Endpoint {
        IDENTITY_LOGIN,
        IDENTITY_LOGOUT,
        IDENTITY_IDENTIFY,
        IDENTITY_MODIFY,
        EVENTS,
        CONFIG
    }

    enum DatabaseTable {
        ATTRIBUTES,
        BREADCRUMBS,
        MESSAGES,
        REPORTING,
        SESSIONS,
        UPLOADS,
        UNKNOWN
    }
}
