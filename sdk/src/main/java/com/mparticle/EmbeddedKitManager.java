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
class EmbeddedKitManager implements IEmbeddedKit{
    private ConcurrentHashMap<Integer,EmbeddedProvider> providers = new ConcurrentHashMap<Integer, EmbeddedProvider>(0);

    Context context;

    EmbeddedKitManager(Context context){
        this.context = context;
    }

    void updateWithConfig(JSONArray kitConfigs){
        for (int i = 0; i < kitConfigs.length(); i++){
            try {
                JSONObject current = kitConfigs.getJSONObject(i);
                if (providers.contains(current.getInt(EmbeddedProvider.KEY_ID))){
                    providers.get(current.getInt(EmbeddedProvider.KEY_ID)).parseConfig(current);
                }else{
                    providers.put(current.getInt(EmbeddedProvider.KEY_ID), EmbeddedProvider.createInstance(current, context));
                }
            }catch (JSONException jse){
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Exception while parsing embedded kit configuration: " + jse.getMessage());
                }
            }catch (ClassNotFoundException cnfe){
                //this should already be logged in the EmbeddedProvider, but I want to bubble up the exception.
            }
        }
    }

    @Override
    public void logEvent() {
        for (EmbeddedProvider provider : providers.values()){
            try {
                provider.logEvent();
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call logEvent for embedded provider " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onCreate(Activity activity) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                provider.onCreate(activity);
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call onCreate for embedded provider " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onResume(Activity activity) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                provider.onResume(activity);
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call onResume for embedded provider " + provider.getName() + ": " + e.getMessage());
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
                    Log.w(Constants.LOG_TAG, "Failed to call logTransaction for embedded provider " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }
}
