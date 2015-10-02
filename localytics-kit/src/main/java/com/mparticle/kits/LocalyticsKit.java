package com.mparticle.kits;

import android.location.Location;
import android.text.TextUtils;

import com.localytics.android.Localytics;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LocalyticsKit extends AbstractKit implements ClientSideForwarder, ECommerceForwarder {
    private static final String API_KEY = "appKey";
    private static final String CUSTOM_DIMENSIONS = "customDimensions";
    private static final String RAW_LTV = "trackClvAsRawValue";
    private JSONArray customDimensionJson = null;
    private boolean trackAsRawLtv = false;

    @Override
    protected AbstractKit update() {
        Localytics.integrate(context, properties.get(API_KEY));
        try{
            customDimensionJson = new JSONArray(properties.get(CUSTOM_DIMENSIONS));
        }catch (JSONException jse) {

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
        Iterator<String> keys = mUserAttributes.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = mUserAttributes.optString(key);
            if (key.equalsIgnoreCase(MParticle.UserAttributes.FIRSTNAME)) {
                Localytics.setCustomerFirstName(value);
            } else if (key.equalsIgnoreCase(MParticle.UserAttributes.LASTNAME)) {
                Localytics.setCustomerFirstName(value);
            } else {
                Localytics.setProfileAttribute(key, value);
            }
        }
        if (customDimensionJson != null) {
            try {
                for (int i = 0; i < customDimensionJson.length(); i++) {
                    JSONObject dimension = customDimensionJson.getJSONObject(i);
                    if (dimension.getString("maptype").equals("UserAttributeClass.Name")) {
                        String attributeName = dimension.getString("map");
                        String attributeValue = mUserAttributes.optString(attributeName);
                        if (!TextUtils.isEmpty(attributeValue)) {
                            int dimensionIndex = Integer.parseInt(dimension.getString("value").substring("Dimension ".length()));
                            Localytics.setCustomDimension(dimensionIndex, attributeValue);
                        }
                    }
                }
            }catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Exception while mapping mParticle user attributes to Localytics custom dimensions: " + e.toString());
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
    public List<ReportingMessage> logTransaction(MPProduct transaction) throws Exception {
        int multiplier = trackAsRawLtv ? 1 : 100;
        Localytics.tagEvent("purchase", transaction, (long) transaction.getTotalAmount() * multiplier);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.fromTransaction(this, transaction));
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
        if (!TextUtils.isEmpty(event.getProductAction()) &&
                event.getProductAction().equalsIgnoreCase(Product.PURCHASE) &&
                event.getProducts().size() > 0) {
            List<Product> productList = event.getProducts();
            Map<String, String> eventAttributes = new HashMap<String, String>();
            CommerceEventUtil.extractActionAttributes(event, eventAttributes);
            double totalAmount = 0;
            int multiplier = trackAsRawLtv ? 1 : 100;
            for (Product product : productList) {
                totalAmount += (product.getTotalAmount() * multiplier);
            }
            Localytics.tagEvent(CommerceEventUtil.getEventTypeString(event), eventAttributes, (long) totalAmount);
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
}
