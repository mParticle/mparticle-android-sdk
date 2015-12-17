package com.mparticle.kits;

import android.app.Activity;
import android.location.Location;
import android.text.TextUtils;

import com.localytics.android.Localytics;
import com.localytics.android.LocalyticsActivityLifecycleCallbacks;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LocalyticsKit extends AbstractKit implements ClientSideForwarder, ECommerceForwarder, ActivityLifecycleForwarder {
    private static final String API_KEY = "appKey";
    private static final String CUSTOM_DIMENSIONS = "customDimensions";
    private static final String RAW_LTV = "trackClvAsRawValue";
    private JSONArray customDimensionJson = null;
    private boolean trackAsRawLtv = false;
    private LocalyticsActivityLifecycleCallbacks callbacks;

    @Override
    protected AbstractKit update() {
        if (callbacks == null) {
            callbacks = new LocalyticsActivityLifecycleCallbacks(context, properties.get(API_KEY));
        }

        try {
            customDimensionJson = new JSONArray(properties.get(CUSTOM_DIMENSIONS));
        } catch (JSONException jse) {

        }
        trackAsRawLtv = Boolean.parseBoolean(properties.get(RAW_LTV));
        return this;
    }

    @Override
    public String getName() {
        return "Localytics";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.toLowerCase().contains("localytics.com");
    }

    @Override
    public void setLocation(Location location) {
        Localytics.setLocation(location);
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if (identityType.equals(MParticle.IdentityType.Email)) {
            Localytics.setCustomerEmail(id);
        } else if (identityType.equals(MParticle.IdentityType.CustomerId)) {
            Localytics.setCustomerId(id);
        } else {
            Localytics.setIdentifier(identityType.name(), id);
        }
    }

    @Override
    void setUserAttributes(JSONObject mUserAttributes) {
        HashSet<String> attributeKeys = null;
        if (customDimensionJson != null) {
            try {
                for (int i = 0; i < customDimensionJson.length(); i++) {
                    JSONObject dimension = customDimensionJson.getJSONObject(i);
                    if (dimension.getString("maptype").equals("UserAttributeClass.Name")) {
                        String attributeName = dimension.getString("map");
                        String attributeValue = mUserAttributes.optString(attributeName);
                        if (!TextUtils.isEmpty(attributeValue)) {
                            if (attributeKeys == null) {
                                attributeKeys = new HashSet<String>();
                            }
                            attributeKeys.add(attributeName);
                            int dimensionIndex = Integer.parseInt(dimension.getString("value").substring("Dimension ".length()));
                            Localytics.setCustomDimension(dimensionIndex, attributeValue);
                        }
                    }
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Exception while mapping mParticle user attributes to Localytics custom dimensions: " + e.toString());
            }
        }
        Iterator<String> keys = mUserAttributes.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (attributeKeys == null || !attributeKeys.contains(key)) {
                String value = mUserAttributes.optString(key);
                if (key.equalsIgnoreCase(MParticle.UserAttributes.FIRSTNAME)) {
                    Localytics.setCustomerFirstName(value);
                } else if (key.equalsIgnoreCase(MParticle.UserAttributes.LASTNAME)) {
                    Localytics.setCustomerLastName(value);
                } else {
                    Localytics.setProfileAttribute(key, value);
                }
            }
        }
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        Localytics.setOptedOut(optOutStatus);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null));
        return messageList;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) throws Exception {
        Localytics.tagEvent(event.getEventName(), event.getInfo());
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.fromEvent(this, event));
        return messageList;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        Localytics.tagScreen(screenName);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), eventAttributes)
                        .setScreenName(screenName)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
        int multiplier = trackAsRawLtv ? 1 : 100;
        Localytics.tagEvent(eventName, contextInfo, (long) valueIncreased.doubleValue() * multiplier);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.fromEvent(this, new MPEvent.Builder(eventName, MParticle.EventType.Transaction).info(contextInfo).build()));
        return messageList;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) throws Exception {
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        if (!TextUtils.isEmpty(event.getProductAction()) && (
                event.getProductAction().equalsIgnoreCase(Product.PURCHASE)) ||
                event.getProductAction().equalsIgnoreCase(Product.REFUND)) {
            Map<String, String> eventAttributes = new HashMap<String, String>();
            CommerceEventUtil.extractActionAttributes(event, eventAttributes);
            int multiplier = trackAsRawLtv ? 1 : 100;
            if (event.getProductAction().equalsIgnoreCase(Product.REFUND)) {
                multiplier *= -1;
            }
            double total = event.getTransactionAttributes().getRevenue() * multiplier;
            Localytics.tagEvent(String.format("eCommerce - %s", event.getProductAction(), eventAttributes, (long) total));
            messages.add(ReportingMessage.fromEvent(this, event));
            return messages;
        }
        List<MPEvent> eventList = CommerceEventUtil.expand(event);
        if (eventList != null) {
            for (int i = 0; i < eventList.size(); i++) {
                try {
                    logEvent(eventList.get(i));
                    messages.add(ReportingMessage.fromEvent(this, event));
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call tagEvent to Localytics kit: " + e.toString());
                }
            }
        }
        return messages;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, int i) {
        if (callbacks != null) {
            callbacks.onActivityCreated(activity, null);
        }
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity, int i) {
        if (callbacks != null) {
            callbacks.onActivityResumed(activity);
        }
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity, int i) {
        if (callbacks != null) {
            callbacks.onActivityPaused(activity);
        }
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity, int i) {
        if (callbacks != null) {
            callbacks.onActivityStopped(activity);
        }
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int i) {
        if (callbacks != null) {
            callbacks.onActivityStarted(activity);
        }
        return null;
    }
}
