package com.mparticle.internal.listeners;

import android.content.ContentValues;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.mparticle.SdkListener;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.InternalSession;

import org.json.JSONObject;

public interface InternalListener {
    void onApiCalled(Object... objects);
    void onKitApiCalled(int kitId, Boolean used, Object... objects);
    void onKitApiCalled(String methodName, int kitId, Boolean used, Object... objects);
    void onCompositeObjects(@Nullable Object child, @Nullable Object parent);
    void onThreadMessage(@NonNull String handlerName, @NonNull Message msg, boolean onNewThread);
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    void onEntityStored(Long rowId, String tableName, ContentValues contentValues);
    void onNetworkRequestStarted(SdkListener.Endpoint type, String url, JSONObject body, Object... objects);
    void onNetworkRequestFinished(SdkListener.Endpoint type, String url, JSONObject response, int responseCode);
    void onSessionUpdated(InternalSession internalSession);
    void onKitDetected(int kitId);
    void onKitConfigReceived(int kitId, String configuration);
    void onKitExcluded(int kitId, String reason);
    void onKitStarted(int kitId);

    InternalListener EMPTY = new InternalListener() {
        public void onApiCalled(Object... objects) { }
        public void onKitApiCalled(int kitId, Boolean used, Object... objects) { }
        public void onKitApiCalled(String methodName, int kitId, Boolean used, Object... objects) { }
        public void onEntityStored(Long rowId, String tableName, ContentValues contentValues) { }
        public void onNetworkRequestStarted(SdkListener.Endpoint type, String url, JSONObject body, Object... objects) { }
        public void onNetworkRequestFinished(SdkListener.Endpoint type, String url, JSONObject response, int responseCode) { }
        public void onSessionUpdated(InternalSession internalSession) { }
        public void onKitDetected(int kitId) { }
        public void onKitConfigReceived(int kitId, String configuration) { }
        public void onKitExcluded(int kitId, String reason) { }
        public void onKitStarted(int kitId) { }
        public void onCompositeObjects(Object child, Object parent) { }
        public void onThreadMessage(String handlerName, Message msg, boolean onNewThread) { }
    };
}
