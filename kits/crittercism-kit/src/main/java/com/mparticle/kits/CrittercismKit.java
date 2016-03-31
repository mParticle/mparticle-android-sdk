package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;

import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Crittercism Kit for version 5.5.x of the Crittercism SDK.
 */
public class CrittercismKit extends KitIntegration implements KitIntegration.CommerceListener, KitIntegration.EventListener, KitIntegration.AttributeListener {

    private static final String APP_ID = "appid";
    private static final String SERVICE_MONITORING = "service_monitoring_enabled";
    private JSONObject mUserAttributes;

    @Override
    public String getName() {
        return "Crittercism";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        onCreate();
        return null;
    }

    /**
     * Crittercism checks the stack trace for "onCreate" || "onResume" and will print a warning if not found.
     *
     * This method is a hack to prevent that warning from showing up in logcat.
     */
    private void onCreate() {
        if (mUserAttributes == null) {
            mUserAttributes = new JSONObject();
        }
        if (MParticle.getInstance().getEnvironment() == MParticle.Environment.Development) {
            Crittercism.setLoggingLevel(Crittercism.LoggingLevel.Info);
        }
        CrittercismConfig config = new CrittercismConfig();
        config.setServiceMonitoringEnabled(Boolean.parseBoolean(getSettings().get(SERVICE_MONITORING)));

        Crittercism.initialize(getContext(), getSettings().get(APP_ID), config);
    }

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) {
        Crittercism.leaveBreadcrumb(breadcrumb);
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(new ReportingMessage(this, ReportingMessage.MessageType.BREADCRUMB, System.currentTimeMillis(), null));
        return messages;
    }

    @Override
    public List<ReportingMessage> logError(String message, Map<String, String> errorAttributes) {
        return null;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
        if (event.getProductAction().equals(Product.PURCHASE) || event.getProductAction().equals(Product.REFUND)) {
            Crittercism.beginTransaction(event.getProductAction());
            Crittercism.setTransactionValue(event.getProductAction(), (int) (event.getTransactionAttributes().getRevenue() * 100));
            if (event.getProductAction().equals(Product.REFUND)) {
                Crittercism.failTransaction(event.getProductAction());
            } else {
                Crittercism.endTransaction(event.getProductAction());
            }
        } else {
            List<MPEvent> eventList = CommerceEventUtil.expand(event);
            for (MPEvent mpEvent : eventList) {
                logEvent(mpEvent);
            }
        }
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String identity) {
        if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            Crittercism.setUsername(identity);
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            Crittercism.setUsername("");
        }
    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    @Override
    public void setUserAttribute(String key, String value) {
        try {
            mUserAttributes.put(KitUtils.sanitizeAttributeKey(key), value);
        } catch (JSONException e) {

        }
        Crittercism.setMetadata(mUserAttributes);
    }

    @Override
    public void removeUserAttribute(String key) {
        mUserAttributes.remove(KitUtils.sanitizeAttributeKey(key));
        Crittercism.setMetadata(mUserAttributes);
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        Crittercism.setOptOutStatus(optOutStatus);
        return Arrays.asList(new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null));
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        Crittercism.leaveBreadcrumb(event.getEventName());
        return Arrays.asList(ReportingMessage.fromEvent(this, event));
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) {
        Crittercism.leaveBreadcrumb(screenName);
        return Arrays.asList(new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), null).setScreenName(screenName));
    }

    @Override
    public List<ReportingMessage> logException(Exception exception, Map<String, String> eventData, String message) {
        Crittercism.logHandledException(exception);

        ReportingMessage reportingMessage = new ReportingMessage(this, ReportingMessage.MessageType.ERROR, System.currentTimeMillis(), eventData);
        if (exception != null) {
            reportingMessage.setExceptionClassName(exception.getClass().getCanonicalName());
        }

        return Arrays.asList(reportingMessage);
    }

    @Override
    public List<ReportingMessage> logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {
        URL critUrl = null;
        try {
            critUrl = new URL(url);
        } catch (MalformedURLException e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Invalid URL sent to logNetworkPerformance: " + url);
        }
        Crittercism.logNetworkRequest(method, critUrl, length, bytesReceived, bytesSent, responseCode, null);
        ReportingMessage message = new ReportingMessage(this, ReportingMessage.MessageType.NETWORK_PERFORMNACE, System.currentTimeMillis(), null);
        return Arrays.asList(message);
    }
}
