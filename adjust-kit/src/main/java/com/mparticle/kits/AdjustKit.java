package com.mparticle.kits;

import android.app.Activity;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.LogLevel;
import com.mparticle.MParticle;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p/>
 * Embedded implementation of the Adjust SDK 4.0.6
 * <p/>
 */
public class AdjustKit extends AbstractKit implements ActivityLifecycleForwarder {

    private static final String APP_TOKEN = "appToken";
    private static final String HOST = "app.adjust.io";
    boolean initialized = false;
    private AtomicBoolean hasResumed = new AtomicBoolean(false);
    //check once per run to make sure we've set the referrer.
    private boolean referrerSet = false;


    private void initAdjust(){
        if (!initialized) {
            boolean production = MParticle.Environment.Production.equals(MParticle.getInstance().getEnvironment());

            AdjustConfig config = new AdjustConfig(context,
                    properties.get(APP_TOKEN),
                    production ? AdjustConfig.ENVIRONMENT_PRODUCTION : AdjustConfig.ENVIRONMENT_SANDBOX);
            if (!production){
                config.setLogLevel(LogLevel.VERBOSE);
            }
            config.setEventBufferingEnabled(false);
            Adjust.onCreate(config);
            setRunning(true);
            if (!isBackgrounded()) {
                if (!hasResumed.get()) {
                    Adjust.onResume();
                    hasResumed.set(true);
                }
            }
            initialized = true;
        }
    }

    @Override
    protected AbstractKit update() {
        initAdjust();

        if (!referrerSet) {
            String installReferrer = MParticle.getInstance().getInstallReferrer();
            if (installReferrer != null) {
                referrerSet = true;
                MParticle.getInstance().setInstallReferrer(installReferrer);
            }
        }
        return this;
    }

    @Override
    public String getName() {
        return "Adjust";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.toLowerCase().contains(HOST);
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity, int currentCount) {
        if (!hasResumed.get()) {
            Adjust.onResume();
            hasResumed.set(true);
            List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
            messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
            );
            return messageList;
        }
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity, int activityCount) {
        Adjust.onPause();
        hasResumed.set(false);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int activityCount) {
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        Adjust.setEnabled(!optOutStatus);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null)
                .setOptOut(optOutStatus)
        );
        return messageList;
    }
}
