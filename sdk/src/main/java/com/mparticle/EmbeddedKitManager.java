package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sdozor on 3/13/14.
 */
class EmbeddedKitManager implements IEmbeddedKit, MPActivityCallbacks{
    private ConcurrentHashMap<Integer,EmbeddedProvider> providers = new ConcurrentHashMap<Integer, EmbeddedProvider>(0);

    Context context;

    EmbeddedKitManager(Context context){
        this.context = context;
    }

    void updateKits(JSONArray kitConfigs){
        for (int i = 0; i < kitConfigs.length(); i++){
            try {
                JSONObject current = kitConfigs.getJSONObject(i);
                if (!providers.containsKey(current.getInt(EmbeddedProvider.KEY_ID))) {
                    providers.put(current.getInt(EmbeddedProvider.KEY_ID), EmbeddedProvider.createInstance(current, context));
                }
                providers.get(current.getInt(EmbeddedProvider.KEY_ID)).parseConfig(current).update();
            }catch (JSONException jse){
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Exception while parsing embedded kit configuration: " + jse.getMessage());
                }
            }catch (ClassNotFoundException cnfe){
                //this should already be logged in the EmbeddedProvider, but I want to bubble up the exception.
            }catch (Exception e){
                if (MParticle.getInstance().getDebugMode()){
                    Log.e(Constants.LOG_TAG, "Exception while started embedded kit: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void logEvent(MParticle.EventType type, String name, JSONObject eventAttributes) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                provider.logEvent(type, name, eventAttributes);
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call logEvent for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void logTransaction(MPTransaction transaction) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                provider.logTransaction(transaction);
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call logTransaction for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void logScreen(String screenName, JSONObject eventAttributes) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                provider.logScreen(screenName, eventAttributes);
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call logScreen for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider instanceof MPActivityCallbacks) {
                try {
                    ((MPActivityCallbacks)provider).onActivityCreated(activity);
                } catch (Exception e) {
                    if (MParticle.getInstance().getDebugMode()) {
                        Log.w(Constants.LOG_TAG, "Failed to call onCreate for embedded provider: " + provider.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider instanceof MPActivityCallbacks) {
                try {
                    ((MPActivityCallbacks)provider).onActivityResumed(activity);
                } catch (Exception e) {
                    if (MParticle.getInstance().getDebugMode()) {
                        Log.w(Constants.LOG_TAG, "Failed to call onResume for embedded provider: " + provider.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }
}
