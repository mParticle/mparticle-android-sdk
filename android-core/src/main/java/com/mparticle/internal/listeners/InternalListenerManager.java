package com.mparticle.internal.listeners;

import android.content.ContentValues;
import android.content.Context;
import android.os.Message;
import android.support.annotation.NonNull;

import com.mparticle.SdkListener;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InternalListenerManager {

    public static boolean isEnabled() {
        return false;
    }

    @NonNull
    public static InternalListener getListener() {
        return InternalListener.EMPTY;
    }
}
