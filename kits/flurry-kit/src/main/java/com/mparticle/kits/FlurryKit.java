package com.mparticle.kits;

import android.app.Activity;
import android.location.Location;
import android.util.Log;

import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FlurryKit extends AbstractKit implements  ClientSideForwarder, ActivityLifecycleForwarder {
    private static final String API_KEY = "apiKey";
    private static final String HASH_ID = "hashCustomerId";
    private static final String CAPTURE_EXCEPTIONS = "captureExceptions";
    private static final String INCLUDE_LOCATION = "includeLocation";
    private boolean includeLocation = true;

    @Override
    protected AbstractKit update() {
        if (MParticle.getInstance().getEnvironment().equals(MParticle.Environment.Development)) {
            FlurryAgent.setLogLevel(Log.VERBOSE);
            FlurryAgent.setLogEnabled(true);
        }else{
            FlurryAgent.setLogEnabled(false);
        }
        if (properties.containsKey(INCLUDE_LOCATION) && !Boolean.parseBoolean(properties.get(INCLUDE_LOCATION))) {
            includeLocation = false;
        }
        FlurryAgent.setReportLocation(includeLocation);
        FlurryAgent.setCaptureUncaughtExceptions(Boolean.parseBoolean(properties.get(CAPTURE_EXCEPTIONS)));
        FlurryAgent.init(context, properties.get(API_KEY));
        return this;
    }

    @Override
    public String getName() {
        return "Flurry";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.toLowerCase().contains("flurry");
    }

    @Override
    public void setLocation(Location location) {
        if (includeLocation) {
            FlurryAgent.setLocation((float) location.getLatitude(), (float) location.getLongitude());
        }
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
       if (identityType.equals(MParticle.IdentityType.CustomerId)) {
           if (!properties.containsKey(HASH_ID) || Boolean.parseBoolean(properties.get(HASH_ID))){
               id = AbstractKit.hashFnv1a(id.getBytes()).toString();
           }
           FlurryAgent.setUserId(id);
       }
    }

    @Override
    void setUserAttributes(JSONObject mUserAttributes) {
        if (mUserAttributes.has(MParticle.UserAttributes.AGE)) {
            try {
                FlurryAgent.setAge(Integer.parseInt(mUserAttributes.getString(MParticle.UserAttributes.AGE)));
            } catch (JSONException e) {

            } catch (NumberFormatException nfe) {

            }
        }
        if(mUserAttributes.has(MParticle.UserAttributes.GENDER)) {
            String genderString = null;
            try {
                genderString = mUserAttributes.getString(MParticle.UserAttributes.GENDER);
                boolean female = genderString.toLowerCase().equalsIgnoreCase("female");
                FlurryAgent.setGender(female ? Constants.FEMALE : Constants.MALE);
            } catch (JSONException e) {
            }
        }
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) throws Exception {
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
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        FlurryAgent.onPageView();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), eventAttributes)
                        .setScreenName(screenName)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity, int activityCount) {
        return null;
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
    public List<ReportingMessage> onActivityStopped(Activity activity, int activityCount) {
        FlurryAgent.onEndSession(activity.getApplicationContext());
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int activityCount) {
        FlurryAgent.onStartSession(activity.getApplicationContext());
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }
}
