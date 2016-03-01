package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FlurryKit extends KitIntegration implements KitIntegration.AttributeListener, KitIntegration.EventListener, KitIntegration.ActivityListener {
    private static final String API_KEY = "apiKey";
    private static final String HASH_ID = "hashCustomerId";
    private static final String CAPTURE_EXCEPTIONS = "captureExceptions";
    private static final String INCLUDE_LOCATION = "includeLocation";
    private boolean includeLocation = true;

    @Override
    public String getName() {
        return "Flurry";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        if (MParticle.getInstance().getEnvironment().equals(MParticle.Environment.Development)) {
        FlurryAgent.setLogLevel(Log.VERBOSE);
        FlurryAgent.setLogEnabled(true);
    }else{
        FlurryAgent.setLogEnabled(false);
    }
        if (getSettings().containsKey(INCLUDE_LOCATION) && !Boolean.parseBoolean(getSettings().get(INCLUDE_LOCATION))) {
            includeLocation = false;
        }
        FlurryAgent.setReportLocation(includeLocation);
        FlurryAgent.setCaptureUncaughtExceptions(Boolean.parseBoolean(getSettings().get(CAPTURE_EXCEPTIONS)));
        FlurryAgent.init(getContext(), getSettings().get(API_KEY));
        return null;
    }

    @Override
    public void setLocation(Location location) {
        if (includeLocation) {
            FlurryAgent.setLocation((float) location.getLatitude(), (float) location.getLongitude());
        }
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String id) {
       if (identityType.equals(MParticle.IdentityType.CustomerId)) {
           if (!getSettings().containsKey(HASH_ID) || Boolean.parseBoolean(getSettings().get(HASH_ID))){
               id = KitUtils.hashFnv1a(id.getBytes()).toString();
           }
           FlurryAgent.setUserId(id);
       }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        if (identityType.equals(MParticle.IdentityType.CustomerId)) {
            FlurryAgent.setUserId("");
        }
    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    @Override
    public void setUserAttribute(String key, String value) {
        if (key.equals(MParticle.UserAttributes.AGE)) {
            try {
                FlurryAgent.setAge(Integer.parseInt(value));
            } catch (NumberFormatException nfe) {

            }
        }
        if(key.equals(MParticle.UserAttributes.GENDER)) {
            boolean female = value.toLowerCase().equalsIgnoreCase("female");
            FlurryAgent.setGender(female ? Constants.FEMALE : Constants.MALE);
        }
    }

    @Override
    public void removeUserAttribute(String key) {
        //there's no way to un-set age/gender
    }

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
        if (event.getInfo() == null) {
            FlurryAgent.logEvent(event.getEventName());
        }else{
            FlurryAgent.logEvent(event.getEventName(), event.getInfo());
        }
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.fromEvent(this,event));
        return messageList;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) {
        FlurryAgent.onPageView();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), eventAttributes)
                        .setScreenName(screenName)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        FlurryAgent.setLogEvents(!optOutStatus);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null)
                        .setOptOut(optOutStatus)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity) {
        FlurryAgent.onEndSession(activity.getApplicationContext());
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
        FlurryAgent.onStartSession(activity.getApplicationContext());
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
}
