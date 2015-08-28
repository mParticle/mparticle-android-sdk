package com.mparticle.internal.embedded;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;

import com.kahuna.sdk.KahunaAnalytics;
import com.kahuna.sdk.KahunaPushReceiver;
import com.kahuna.sdk.KahunaPushService;
import com.kahuna.sdk.KahunaUserAttributesKeys;
import com.kahuna.sdk.KahunaUserCredentialKeys;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MPReceiver;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPActivityCallbacks;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.embedded.adjust.sdk.Adjust;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
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

    public EmbeddedBranchMetrics(EmbeddedKitManager ekManager) {
        super(ekManager);
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
    public void logEvent(MPEvent event) throws Exception {
        Map<String, String> attributes = event.getInfo();
        JSONObject jsonAttributes = null;
        if (attributes != null && attributes.size() > 0) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                jsonAttributes.put(entry.getKey(), entry.getValue());
            }
        }
        getBranch().userCompletedAction(event.getEventName(), jsonAttributes);

    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        logEvent(
                new MPEvent.Builder("Viewed " + screenName, MParticle.EventType.Other)
                        .info(eventAttributes)
                        .build()
        );
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
    void logout() {
        getBranch().logout();
    }
}