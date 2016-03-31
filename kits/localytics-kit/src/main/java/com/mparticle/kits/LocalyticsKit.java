package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;

import com.localytics.android.Localytics;
import com.localytics.android.LocalyticsActivityLifecycleCallbacks;
import com.localytics.android.PushReceiver;
import com.localytics.android.ReferralReceiver;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalyticsKit extends KitIntegration implements KitIntegration.EventListener, KitIntegration.CommerceListener, KitIntegration.AttributeListener, KitIntegration.PushListener {
    private static final String API_KEY = "appKey";
    private static final String CUSTOM_DIMENSIONS = "customDimensions";
    private static final String RAW_LTV = "trackClvAsRawValue";
    private JSONArray customDimensionJson = null;
    private boolean trackAsRawLtv = false;
    private LocalyticsActivityLifecycleCallbacks callbacks;

    @Override
    public String getName() {
        return "Localytics";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        if (callbacks == null) {
            callbacks = new LocalyticsActivityLifecycleCallbacks(getContext(), getSettings().get(API_KEY));
        }

        try {
            customDimensionJson = new JSONArray(properties.get(CUSTOM_DIMENSIONS));
        } catch (Exception jse) {
        }
        trackAsRawLtv = Boolean.parseBoolean(getSettings().get(RAW_LTV));
        return null;
    }

    @Override
    public void setLocation(Location location) {
        Localytics.setLocation(location);
    }

    @Override
    public void setInstallReferrer(Intent intent) {
        new ReferralReceiver().onReceive(getContext(), intent);
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String id) {
        if (identityType.equals(MParticle.IdentityType.Email)) {
            Localytics.setCustomerEmail(id);
        } else if (identityType.equals(MParticle.IdentityType.CustomerId)) {
            Localytics.setCustomerId(id);
        } else {
            Localytics.setIdentifier(identityType.name(), id);
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        if (identityType.equals(MParticle.IdentityType.Email)) {
            Localytics.setCustomerEmail("");
        } else if (identityType.equals(MParticle.IdentityType.CustomerId)) {
            Localytics.setCustomerId("");
        } else {
            Localytics.setIdentifier(identityType.name(), "");
        }
    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    @Override
    public void setUserAttribute(String key, String value) {
        HashSet<String> attributeKeys = null;
        if (customDimensionJson != null) {
            try {
                for (int i = 0; i < customDimensionJson.length(); i++) {
                    JSONObject dimension = customDimensionJson.getJSONObject(i);
                    if (dimension.getString("maptype").equals("UserAttributeClass.Name")) {
                        String attributeName = dimension.getString("map");
                        if (!TextUtils.isEmpty(value)) {
                            if (attributeKeys == null) {
                                attributeKeys = new HashSet<String>();
                            }
                            attributeKeys.add(attributeName);
                            int dimensionIndex = Integer.parseInt(dimension.getString("value").substring("Dimension ".length()));
                            Localytics.setCustomDimension(dimensionIndex, value);
                        }
                    }
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Exception while mapping mParticle user attributes to Localytics custom dimensions: " + e.toString());
            }
        }

        if (attributeKeys == null || !attributeKeys.contains(key)) {
            if (key.equalsIgnoreCase(MParticle.UserAttributes.FIRSTNAME)) {
                Localytics.setCustomerFirstName(value);
            } else if (key.equalsIgnoreCase(MParticle.UserAttributes.LASTNAME)) {
                Localytics.setCustomerLastName(value);
            } else {
                Localytics.setProfileAttribute(KitUtils.sanitizeAttributeKey(key), value);
            }
        }
    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        Localytics.setOptedOut(optOutStatus);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null));
        return messageList;
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
        Localytics.tagEvent(event.getEventName(), event.getInfo());
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.fromEvent(this, event));
        return messageList;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) {
        Localytics.tagScreen(screenName);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), eventAttributes)
                        .setScreenName(screenName)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal totalValue, String eventName, Map<String, String> contextInfo) {
        int multiplier = trackAsRawLtv ? 1 : 100;
        Localytics.tagEvent(eventName, contextInfo, (long) valueIncreased.doubleValue() * multiplier);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.fromEvent(this, new MPEvent.Builder(eventName, MParticle.EventType.Transaction).info(contextInfo).build()));
        return messageList;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
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
    public boolean willHandlePushMessage(Set<String> keyset) {
        return keyset.contains("ll");
    }

    @Override
    public void onPushMessageReceived(Context context, Intent extras) {
        new PushReceiver().onReceive(context, extras);
    }

    @Override
    public boolean onPushRegistration(String token, String senderId) {
        Localytics.setPushRegistrationId(token);
        return true;
    }
}
