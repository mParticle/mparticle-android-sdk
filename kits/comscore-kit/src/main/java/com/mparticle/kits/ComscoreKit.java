package com.mparticle.kits;

import android.app.Activity;

import com.comscore.analytics.comScore;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p/>
 * Embedded implementation of the Comscore SDK, tested against Comscore 2.14.0923.
 * <p/>
 */
public class ComscoreKit extends AbstractKit implements ActivityLifecycleForwarder, ClientSideForwarder {
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

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        if (!isEnterprise) {
            return null;
        }
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        HashMap<String, String> comscoreLabels;
        Map<String, String> attributes = event.getInfo();
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
        messages.add(
                ReportingMessage.fromEvent(this,
                        new MPEvent.Builder(event).info(comscoreLabels).build()
                )
        );
        return messages;
    }

    @Override
    public List<ReportingMessage>  logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        return logEvent(
                new MPEvent.Builder(screenName, MParticle.EventType.Navigation).info(eventAttributes).build()
        );
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
        return null;
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
    protected AbstractKit update() {
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
    public List<ReportingMessage> onActivityStopped(Activity activity, int currentCount) {
        comScore.onExitForeground();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int currentCount) {
        comScore.onEnterForeground();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity, int currentCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        comScore.setEnabled(!optOutStatus);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null)
                        .setOptOut(optOutStatus)
        );
        return messageList;
    }

}
