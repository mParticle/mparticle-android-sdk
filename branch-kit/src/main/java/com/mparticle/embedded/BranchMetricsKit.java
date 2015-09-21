package com.mparticle.embedded;

import android.app.Activity;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;

import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.branch.referral.Branch;

/**
 * <p/>
 * Embedded implementation of the Branch Metrics SDK
 * <p/>
 */
public class BranchMetricsKit extends EmbeddedProvider implements ActivityLifecycleForwarder, ClientSideForwarder {

    private String BRANCH_APP_KEY = "branchKey";
    private final String FORWARD_SCREEN_VIEWS = "forwardScreenViews";
    private boolean mSendScreenEvents;

    @Override
    public String getName() {
        return "Branch Metrics";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri.contains("api.branch.io");
    }

    @Override
    protected EmbeddedProvider update() {
        if (!isBackgrounded()) {
           if (!isRunning()) {
               setRunning(getBranch().initSession());
           }
        }
        String sendScreenEvents = properties.get(FORWARD_SCREEN_VIEWS);
        mSendScreenEvents = sendScreenEvents != null && sendScreenEvents.equalsIgnoreCase("true");
        return this;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) throws Exception {
        Map<String, String> attributes = event.getInfo();
        JSONObject jsonAttributes = null;
        if (attributes != null && attributes.size() > 0) {
            jsonAttributes = new JSONObject();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                jsonAttributes.put(entry.getKey(), entry.getValue());
            }
        }
        getBranch().userCompletedAction(event.getEventName(), jsonAttributes);
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        if (mSendScreenEvents){
            MPEvent event = new MPEvent.Builder("Viewed " + screenName, MParticle.EventType.Other)
                    .info(eventAttributes)
                    .build();
            return logEvent(event);
        }else {
            return null;
        }
    }

    @Override
    public List<ReportingMessage> logTransaction(MPProduct transaction) throws Exception {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity, int activityCount) {
        //there's no hurt in calling this here just in case the EK is init'd too late for onStart
        setRunning(getBranch().initSession());
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity, int activityCount) {
        return null;
    }

    private Branch getBranch() {
        return Branch.getInstance(context, properties.get(BRANCH_APP_KEY));
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity, int activityCount) {
        getBranch().closeSession();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int activityCount) {
        if (getBranch().initSession()) {
            setRunning(true);
            List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
            messageList.add(
                    new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
            );
            return messageList;
        }else {
            setRunning(false);
            return null;
        }
    }

    @Override
    protected void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if (identityType == MParticle.IdentityType.CustomerId) {
            getBranch().setIdentity(id);
        }
    }

    @Override
    protected List<ReportingMessage> logout() {
        getBranch().logout();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.logoutMessage(this));
        return messageList;
    }
}