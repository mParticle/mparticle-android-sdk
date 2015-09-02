package com.mparticle.internal.embedded;

import android.app.Activity;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.internal.MPActivityCallbacks;

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
class EmbeddedBranchMetrics extends EmbeddedProvider implements MPActivityCallbacks, ClientSideForwarder {

    private String BRANCH_APP_KEY = "branchKey";

    public EmbeddedBranchMetrics(int id, EmbeddedKitManager ekManager) {
        super(id, ekManager);
        setRunning(false);
    }

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
        if (!mEkManager.getAppStateManager().isBackgrounded()) {
           if (!isRunning()) {
               setRunning(getBranch().initSession());
           }
        }
        return this;
    }


    @Override
    public List<ReportingMessage> logEvent(MPEvent event) throws Exception {
        Map<String, String> attributes = event.getInfo();
        JSONObject jsonAttributes = null;
        if (attributes != null && attributes.size() > 0) {
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
        MPEvent event = new MPEvent.Builder("Viewed " + screenName, MParticle.EventType.Other)
                .info(eventAttributes)
                .build();
        return logEvent(event);
    }

    @Override
    public List<ReportingMessage> logTransaction(MPProduct transaction) throws Exception {
        return null;
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityResumed(Activity activity, int activityCount) {
        //there's no hurt in calling this here just in case the EK is init'd too late for onStart
        setRunning(getBranch().initSession());
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {

    }

    private Branch getBranch() {
        return Branch.getInstance(context, properties.get(BRANCH_APP_KEY));
    }

    @Override
    public void onActivityStopped(Activity activity, int activityCount) {
        getBranch().closeSession();
    }

    @Override
    public void onActivityStarted(Activity activity, int activityCount) {
        setRunning(getBranch().initSession());
    }

    @Override
    void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if (identityType == MParticle.IdentityType.CustomerId) {
            getBranch().setIdentity(id);
        }
    }

    @Override
    List<ReportingMessage> logout() {
        getBranch().logout();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.logoutMessage(this));
        return messageList;
    }
}