package com.mparticle.internal.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;

import com.mparticle.AppStateManager;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPActivityCallbacks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EmbeddedKitManager implements MPActivityCallbacks {
    private ConfigManager mConfigManager;
    private AppStateManager mAppStateManager;
    private ConcurrentHashMap<Integer,EmbeddedProvider> providers = new ConcurrentHashMap<Integer, EmbeddedProvider>(0);

    Context context;

    public EmbeddedKitManager(Context context){
        this.context = context;
    }

    //called from a background thread by the ConfigManager when we get new configuration
    public void updateKits(JSONArray kitConfigs){
        if (kitConfigs == null) {
            providers.clear();
        }else{
            HashSet<Integer> activeIds = new HashSet<Integer>();
            EmbeddedKitFactory ekFactory = new EmbeddedKitFactory();
            for (int i = 0; i < kitConfigs.length(); i++) {
                try {
                    JSONObject current = kitConfigs.getJSONObject(i);
                    int currentId = current.getInt(EmbeddedProvider.KEY_ID);
                    if (ekFactory.isSupported(currentId)) {
                        activeIds.add(currentId);
                        if (!providers.containsKey(currentId)) {
                            providers.put(currentId, ekFactory.createInstance(currentId, this));
                        }
                        providers.get(currentId).parseConfig(current).update();
                        if (!providers.get(currentId).optedOut()) {
                            providers.get(currentId).setUserAttributes(MParticle.getInstance().getUserAttributes());
                            syncUserIdentities(providers.get(currentId));
                        }
                    }
                } catch (JSONException jse) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Exception while parsing embedded kit configuration: " + jse.getMessage());
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
        JSONArray identities = MParticle.getInstance().getUserIdentities();
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

    public void logEvent(MPEvent event) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut() && provider.shouldLogEvent(event.getEventType(), event.getEventName())) {
                    provider.logEvent(event, provider.filterEventAttributes(event.getEventType(), event.getEventName(), provider.mAttributeFilters, event.getInfo()));
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logEvent for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

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

    public void setUserAttributes(JSONObject userAttributes) {
        for (EmbeddedProvider provider : providers.values()){
            try {
                if (!provider.optedOut()) {
                    provider.setUserAttributes(provider.filterAttributes(provider.mUserAttributeFilters, userAttributes));
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call setUserAttributes for embedded provider: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

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

    public Uri getSurveyUrl(int serviceId, JSONObject userAttributes) {
        EmbeddedProvider provider = providers.get(serviceId);
        if (provider instanceof ISurveyProvider) {
            return ((ISurveyProvider)provider).getSurveyUrl(provider.filterAttributes(provider.mUserAttributeFilters, userAttributes));
        } else{
            return null;
        }
    }

    public Context getContext() {
        return context;
    }

    public ConfigManager getConfigurationManager() {
        return mConfigManager;
    }

    public AppStateManager getAppStateManager() {
        return mAppStateManager;
    }

    public void setConfigManager(ConfigManager configManager) {
        mConfigManager = configManager;
    }

    public void setAppStateManager(AppStateManager appStateManager) {
        this.mAppStateManager = appStateManager;
    }
}
