package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
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
                if (!providers.get(current.getInt(EmbeddedProvider.KEY_ID)).optedOut()) {
                    providers.get(current.getInt(EmbeddedProvider.KEY_ID)).setUserAttributes(MParticle.getInstance().mUserAttributes);
                    syncUserIdentities(providers.get(current.getInt(EmbeddedProvider.KEY_ID)));
                }
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

    private void syncUserIdentities(EmbeddedProvider provider) {
        JSONArray identities = MParticle.getInstance().mUserIdentities;
        if (identities != null) {
            for (int i = 0; i < identities.length(); i++) {
                try {
                    JSONObject identity = identities.getJSONObject(i);
                    MParticle.IdentityType type = MParticle.IdentityType.parseInt(identity.getInt(Constants.MessageKey.IDENTITY_NAME));
                    String id = identity.getString(Constants.MessageKey.IDENTITY_VALUE);
                    provider.setUserIdentity(id, type);
                }catch (JSONException jse){
                    //swallow
                }
            }
        }
    }


    @Override
    public void logEvent(MParticle.EventType type, String name, JSONObject eventAttributes) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.logEvent(type, name, eventAttributes);
                }
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
                if (!provider.optedOut()) {
                    provider.logTransaction(transaction);
                }
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
                if (!provider.optedOut()) {
                    provider.logScreen(screenName, eventAttributes);
                }
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call logScreen for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void setLocation(Location location) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.setLocation(location);
                }
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call setLocation for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void setUserAttributes(JSONObject mUserAttributes) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.setUserAttributes(mUserAttributes);
                }
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call setUserAttributes for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void removeUserAttribute(String key) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.removeUserAttribute(key);
                }
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call removeUserAttribute for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.setUserIdentity(id, identityType);
                }
            } catch (Exception e) {
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Failed to call setUserIdentity for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider instanceof MPActivityCallbacks) {
                try {
                    if (!provider.optedOut()) {
                        ((MPActivityCallbacks) provider).onActivityCreated(activity);
                    }
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
                    if (!provider.optedOut()) {
                        ((MPActivityCallbacks) provider).onActivityResumed(activity);
                    }
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
