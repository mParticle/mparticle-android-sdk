package com.mparticle.internal.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.comscore.analytics.comScore;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.internal.MPActivityCallbacks;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p/>
 * Embedded implementation of the Comscore SDK, tested against Comscore 2.14.0923.
 * <p/>
 */
class EmbeddedComscore extends EmbeddedProvider implements MPActivityCallbacks {
    //Server config constants defined for this provider
    //keys to provide access to the comscore account.
    private static final String CLIENT_ID = "CustomerC2Value";
    private static final String PUBLISHER_SECRET = "PublisherSecret";
    private static final String USE_HTTPS = "UseHttps";
    private static final String PRODUCT = "product";
    private static final String APPNAME = "appName";
    private static final String AUTOUPDATE_MODE_KEY = "autoUpdateMode";
    private static final String AUTOUPDATE_INTERVAL = "autoUpdateInterval";

    private static final String AUTOUPDATE_MODE_FOREONLY = "foreonly";
    private static final String AUTOUPDATE_MODE_FOREBACK = "foreback";
    private static final String COMSCORE_DEFAULT_LABEL_KEY = "name";
    private String clientId;
    private String publisherSecret;
    private String autoUpdateMode;
    private int autoUpdateInterval = 60;

    private static final String HOST = "scorecardresearch.com";
    private boolean isEnterprise;

    EmbeddedComscore(EmbeddedKitManager ekManager) {
        super(ekManager);
        comScore.setAppContext(context);
    }

    @Override
    public void logEvent(MPEvent event, Map<String, String> attributes) {
        if (isEnterprise) {
            HashMap<String, String> comscoreLabels;
            if (attributes == null) {
                comscoreLabels = new HashMap<String, String>();
            }else if (!(attributes instanceof HashMap)){
                comscoreLabels = new HashMap<String, String>();
                for (Map.Entry<String, String> entry : attributes.entrySet())
                {
                    comscoreLabels.put(entry.getKey(), entry.getValue());
                }
            }else {
                comscoreLabels = (HashMap<String, String>) attributes;
            }
            comscoreLabels.put(COMSCORE_DEFAULT_LABEL_KEY, event.getEventName());
            if (MParticle.EventType.Navigation.equals(event.getEventType())){
                comScore.view(comscoreLabels);
            }else{
                comScore.hidden(comscoreLabels);
            }
        }
    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        logEvent(
                new MPEvent.Builder(screenName, MParticle.EventType.Navigation).build(),
                eventAttributes
        );
    }

    @Override
    public void setUserAttributes(JSONObject userAttributes) {
        if (isEnterprise && userAttributes != null){
            HashMap<String, String> comScoreAttributes = new HashMap<String, String>();
            Iterator<String> keysItr = userAttributes.keys();
            while(keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = userAttributes.opt(key);
                if (value != null){
                    try {
                        comScoreAttributes.put(key, userAttributes.getString(key));
                    }catch (JSONException jse){
                        try {
                            comScoreAttributes.put(key, Double.toString(userAttributes.getDouble(key)));
                        }catch (JSONException jse2){
                            try {
                                comScoreAttributes.put(key, Boolean.toString(userAttributes.getBoolean(key)));
                            }catch (JSONException jse3){

                            }
                        }
                    }
                }else{
                    comScoreAttributes.put(key, "");
                }
            }
            comScore.setLabels(comScoreAttributes);
        }
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if (isEnterprise){
            comScore.setLabel(identityType.toString(), id);
        }
    }

    @Override
    protected EmbeddedProvider update() {
        if (needsRestart()){
            clientId = properties.get(CLIENT_ID);
            comScore.setCustomerC2(clientId);
            publisherSecret = properties.get(PUBLISHER_SECRET);
            comScore.setPublisherSecret(publisherSecret);
        }

        int tempUpdateInterval = 60;
        try {
            tempUpdateInterval = Integer.parseInt(properties.get(AUTOUPDATE_INTERVAL));
        }catch (NumberFormatException nfe){

        }
        if (!properties.get(AUTOUPDATE_MODE_KEY).equals(autoUpdateMode) || tempUpdateInterval != autoUpdateInterval){
            autoUpdateInterval = tempUpdateInterval;
            autoUpdateMode = properties.get(AUTOUPDATE_MODE_KEY);
            if (AUTOUPDATE_MODE_FOREBACK.equals(autoUpdateMode)){
                comScore.enableAutoUpdate(autoUpdateInterval, false);
            }else if (AUTOUPDATE_MODE_FOREONLY.equals(autoUpdateMode)){
                comScore.enableAutoUpdate(autoUpdateInterval, true);
            }else {
                comScore.disableAutoUpdate();
            }
        }

        boolean useHttps = Boolean.parseBoolean(properties.get(USE_HTTPS));
        comScore.setSecure(useHttps);
        comScore.setDebug(mEkManager.getConfigurationManager().getEnvironment() == MParticle.Environment.Development);
        isEnterprise = "enterprise".equals(properties.get(PRODUCT));
        String appName = properties.get(APPNAME);
        if (appName != null){
            comScore.setAppName(appName);
        }
        return this;
    }

    @Override
    public String getName() {
        return "Comscore";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.toLowerCase().contains(HOST);
    }

    private boolean needsRestart() {
        return !properties.get(CLIENT_ID).equals(clientId) || !properties.get(PUBLISHER_SECRET).equals(publisherSecret);
    }

    @Override
    public void onActivityStopped(Activity activity, int currentCount) {
        comScore.onExitForeground();
    }

    @Override
    public void onActivityStarted(Activity activity, int currentCount) {
        comScore.onEnterForeground();
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityResumed(Activity activity, int currentCount) {

    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {
    }


}
