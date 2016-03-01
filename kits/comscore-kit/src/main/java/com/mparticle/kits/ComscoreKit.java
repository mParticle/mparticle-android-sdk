package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.comscore.analytics.comScore;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p/>
 * Embedded implementation of the Comscore SDK, tested against Comscore 2.14.0923.
 * <p/>
 */
public class ComscoreKit extends KitIntegration implements KitIntegration.EventListener, KitIntegration.AttributeListener, KitIntegration.ActivityListener {
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
    private boolean isEnterprise;

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) {
        return null;
    }

    @Override
    public List<ReportingMessage> logError(String message, Map<String, String> errorAttributes) {
        return null;
    }

    @Override
    public List<ReportingMessage> logException(Exception exception, Map<String, String> exceptionAttributes, String message) {
        return null;
    }

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
    public List<ReportingMessage>  logScreen(String screenName, Map<String, String> eventAttributes) {
        return logEvent(
                new MPEvent.Builder(screenName, MParticle.EventType.Navigation)
                        .info(eventAttributes)
                        .build()
        );
    }

    @Override
    public void setUserAttribute(String key, String value) {
        if (isEnterprise){
            comScore.setLabel(KitUtils.sanitizeAttributeKey(key), value);
        }
    }

    @Override
    public void removeUserAttribute(String key) {
        if (isEnterprise){
            comScore.getLabels().remove(KitUtils.sanitizeAttributeKey(key));
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        if (isEnterprise){
            comScore.getLabels().remove(identityType.toString());
        }
    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String id) {
        if (isEnterprise){
            comScore.setLabel(identityType.toString(), id);
        }
    }

    @Override
    public String getName() {
        return "Comscore";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        if (needsRestart()){
            clientId = getSettings().get(CLIENT_ID);
            comScore.setCustomerC2(clientId);
            publisherSecret = getSettings().get(PUBLISHER_SECRET);
            comScore.setPublisherSecret(publisherSecret);
        }

        int tempUpdateInterval = 60;
        try {
            tempUpdateInterval = Integer.parseInt(getSettings().get(AUTOUPDATE_INTERVAL));
        }catch (NumberFormatException nfe){

        }
        if (!getSettings().get(AUTOUPDATE_MODE_KEY).equals(autoUpdateMode) || tempUpdateInterval != autoUpdateInterval){
            autoUpdateInterval = tempUpdateInterval;
            autoUpdateMode = getSettings().get(AUTOUPDATE_MODE_KEY);
            if (AUTOUPDATE_MODE_FOREBACK.equals(autoUpdateMode)){
                comScore.enableAutoUpdate(autoUpdateInterval, false);
            }else if (AUTOUPDATE_MODE_FOREONLY.equals(autoUpdateMode)){
                comScore.enableAutoUpdate(autoUpdateInterval, true);
            }else {
                comScore.disableAutoUpdate();
            }
        }

        boolean useHttps = Boolean.parseBoolean(getSettings().get(USE_HTTPS));
        comScore.setSecure(useHttps);
        comScore.setDebug(getKitManager().getConfigurationManager().getEnvironment() == MParticle.Environment.Development);
        isEnterprise = "enterprise".equals(getSettings().get(PRODUCT));
        String appName = getSettings().get(APPNAME);
        if (appName != null){
            comScore.setAppName(appName);
        }
        return null;
    }

    private boolean needsRestart() {
        return !getSettings().get(CLIENT_ID).equals(clientId) || !getSettings().get(PUBLISHER_SECRET).equals(publisherSecret);
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity) {
        comScore.onExitForeground();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivitySaveInstanceState(Activity activity, Bundle outState) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityDestroyed(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle savedInstanceState) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity) {
        comScore.onEnterForeground();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity) {
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
