package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
        if (kitConfigs == null) {
            providers.clear();
        }else{
            HashSet<Integer> activeIds = new HashSet<Integer>();
            for (int i = 0; i < kitConfigs.length(); i++) {
                try {
                    JSONObject current = kitConfigs.getJSONObject(i);
                    int currentId = current.getInt(EmbeddedProvider.KEY_ID);
                    activeIds.add(currentId);
                    if (!providers.containsKey(currentId)) {
                        providers.put(currentId, new BaseEmbeddedKitFactory().createInstance(currentId, context));
                    }
                    providers.get(currentId).parseConfig(current).update();
                    if (!providers.get(currentId).optedOut()) {
                        providers.get(currentId).setUserAttributes(MParticle.getInstance().mUserAttributes);
                        syncUserIdentities(providers.get(currentId));
                    }
                } catch (JSONException jse) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Exception while parsing embedded kit configuration: " + jse.getMessage());
                } catch (ClassNotFoundException cnfe) {
                    //this should already be logged in the EmbeddedProvider, but I want to bubble up the exception.
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Exception while started embedded kit: " + e.getMessage());
                }
            }

            Iterator<Integer> ids = providers.keySet().iterator();
            while (ids.hasNext()) {
                Integer id = ids.next();
                if (!activeIds.contains(id)) {
                    ids.remove();
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
    public void logEvent(MParticle.EventType type, String name, Map<String, String> eventAttributes) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut() && provider.shouldLogEvent(type, name)) {
                    provider.logEvent(type, name, provider.filterEventAttributes(type, name, provider.mAttributeFilters, eventAttributes));
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logEvent for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logTransaction(MPProduct product) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.logTransaction(product);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logTransaction for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventAttributes) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut() && provider.shouldLogScreen(screenName)) {
                    provider.logScreen(screenName, provider.filterEventAttributes(null, screenName, provider.mScreenAttributeFilters, eventAttributes));
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logScreen for embedded provider: " + provider.getName() + ": " + e.getMessage());
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
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call setLocation for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void setUserAttributes(JSONObject mUserAttributes) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.setUserAttributes(provider.filterAttributes(provider.mUserAttributeFilters, mUserAttributes));
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call setUserAttributes for embedded provider: " + provider.getName() + ": " + e.getMessage());
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
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call removeUserAttribute for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut() && provider.shouldSetIdentity(identityType)) {
                    provider.setUserIdentity(id, identityType);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call setUserIdentity for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logout() {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.logout();
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logout for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void removeUserIdentity(String id) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.removeUserIdentity(id);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call removeUserIdentity for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void handleIntent(Intent intent) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.handleIntent(intent);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call handleIntent for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void startSession() {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.startSession();
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call startSession for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void endSession() {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.endSession();
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call endSession for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider instanceof MPActivityCallbacks) {
                try {
                    if (!provider.optedOut()) {
                        ((MPActivityCallbacks) provider).onActivityCreated(activity, activityCount);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onCreate for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity, int currentCount) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider instanceof MPActivityCallbacks) {
                try {
                    if (!provider.optedOut()) {
                        ((MPActivityCallbacks) provider).onActivityResumed(activity, currentCount);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onResume for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider instanceof MPActivityCallbacks) {
                try {
                    if (!provider.optedOut()) {
                        ((MPActivityCallbacks) provider).onActivityPaused(activity, activityCount);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onResume for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity, int currentCount) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider instanceof MPActivityCallbacks) {
                try {
                    if (!provider.optedOut()) {
                        ((MPActivityCallbacks) provider).onActivityStopped(activity, currentCount);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onResume for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity, int currentCount) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider instanceof MPActivityCallbacks) {
                try {
                    if (!provider.optedOut()) {
                        ((MPActivityCallbacks) provider).onActivityStarted(activity, currentCount);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onResume for embedded provider: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public boolean isEmbeddedKitUri(String url) {
        for (EmbeddedProvider provider : providers.values()){
            if (provider.isOriginator(url)){
                return true;
            }
        }
        return false;
    }

    public String getActiveModuleIds() {
        if (providers.isEmpty()){
            return "";
        }else {
            Set keys = providers.keySet();
            StringBuilder buffer = new StringBuilder(keys.size() * 3);

            Iterator<Integer> it = keys.iterator();
            while (it.hasNext()) {
                Integer next = it.next();
                buffer.append(next);
                if (it.hasNext()) {
                    buffer.append(",");
                }
            }
            return buffer.toString();
        }
    }


    public static class BaseEmbeddedKitFactory {
        private final static int MAT = 32;
        private final static int KOCHAVA = 37;
        private final static int COMSCORE = 39;
        private final static int KAHUNA = 56;

        protected EmbeddedProvider createInstance(int id, Context context) throws JSONException, ClassNotFoundException{
            switch (id){
                case MAT:
                    return new EmbeddedMAT(context);
                case KOCHAVA:
                    return new EmbeddedKochava(context);
                case COMSCORE:
                    return new EmbeddedComscore(context);
                case KAHUNA:
                    return new EmbeddedKahuna(context);
                default:
                    return null;
            }
        }

        public static ArrayList<Integer> getSupportedKits() {
            ArrayList<Integer> supportedKitIds = new ArrayList<Integer>();
            supportedKitIds.add(MAT);
            supportedKitIds.add(KOCHAVA);
            supportedKitIds.add(COMSCORE);
            supportedKitIds.add(KAHUNA);
            return supportedKitIds;
        }
    }
}
