package com.mparticle.kits.mappings;

import com.mparticle.MPEvent;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.kits.CommerceEventUtils;
import com.mparticle.kits.KitUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class CustomMappingMatch {

    static final String MATCH_TYPE_STRING = "S";
    static final String MATCH_TYPE_HASH = "H";
    public final static String PROPERTY_LOCATION_EVENT_FIELD = "EventField";
    public final static String PROPERTY_LOCATION_EVENT_ATTRIBUTE = "EventAttribute";
    public final static String PROPERTY_LOCATION_PRODUCT_FIELD = "ProductField";
    public final static String PROPERTY_LOCATION_PRODUCT_ATTRIBUTE = "ProductAttribute";
    public final static String PROPERTY_LOCATION_PROMOTION_FIELD = "PromotionField";

    int mMessageType = -1;
    String mMatchType = "String";
    String commerceMatchProperty = null;
    String commerceMatchPropertyName = null;
    Set<String> commerceMatchPropertyValues = null;
    int mEventHash;
    String mEventName = null;
    String mAttributeKey = null;
    Set<String> mAttributeValues = null;

    public CustomMappingMatch(JSONObject match) {
        if (match == null) {
            mEventHash = 0;
            mMessageType = -1;
            mMatchType = "String";
            mEventName = null;
            mAttributeKey = null;
            mAttributeValues = null;
        } else {
            mMessageType = match.optInt("message_type");
            mMatchType = match.optString("event_match_type", "String");
            commerceMatchProperty = match.optString("property", PROPERTY_LOCATION_EVENT_ATTRIBUTE);
            commerceMatchPropertyName = match.optString("property_name", null);
            if (match.has("property_values")) {
                try {
                    JSONArray propertyValues = match.getJSONArray("property_values");
                    commerceMatchPropertyValues = new HashSet<String>(propertyValues.length());
                    for (int i = 0; i < propertyValues.length(); i++) {
                        commerceMatchPropertyValues.add(propertyValues.optString(i).toLowerCase(Locale.US));
                    }
                } catch (JSONException jse) {

                }
            }
            if (mMatchType.startsWith(MATCH_TYPE_HASH)) {
                mEventHash = Integer.parseInt(match.optString("event"));
                mAttributeKey = null;
                mAttributeValues = null;
                mEventName = null;
            } else {
                mEventHash = 0;
                mEventName = match.optString("event");
                mAttributeKey = match.optString("attribute_key");
                try {
                    if (match.has("attribute_values") && match.get("attribute_values") instanceof JSONArray) {
                        JSONArray values = match.getJSONArray("attribute_values");
                        mAttributeValues = new HashSet<String>(values.length());
                        for (int i = 0; i < values.length(); i++) {
                            mAttributeValues.add(values.optString(i).toLowerCase(Locale.US));
                        }
                    } else if (match.has("attribute_values")) {
                        mAttributeValues = new HashSet<String>(1);
                        mAttributeValues.add(match.optString("attribute_values").toLowerCase(Locale.US));
                    } else {
                        mAttributeValues = new HashSet<String>(1);
                        mAttributeValues.add(match.optString("attribute_value").toLowerCase(Locale.US));
                    }

                } catch (JSONException jse) {

                }
            }
        }
    }

    /**
     * This is an optimization - check the basic stuff to see if we have a match before actually trying to do the projection.
     */
    public boolean isMatch(EventWrapper eventWrapper) {
        if (eventWrapper.getMessageType() != mMessageType) {
            return false;
        }
        if (eventWrapper instanceof EventWrapper.MPEventWrapper) {
            if (matchAppEvent((EventWrapper.MPEventWrapper) eventWrapper)) {
                return true;
            }
        } else {
            CommerceEvent commerceEvent = matchCommerceEvent((EventWrapper.CommerceEventWrapper) eventWrapper);
            if (commerceEvent != null) {
                ((EventWrapper.CommerceEventWrapper) eventWrapper).setEvent(commerceEvent);
                return true;
            }
        }
        return false;
    }

    private boolean matchAppEvent(EventWrapper.MPEventWrapper eventWrapper) {
        MPEvent event = eventWrapper.getEvent();
        if (event == null) {
            return false;
        }
        if (mMatchType.startsWith(MATCH_TYPE_HASH) && eventWrapper.getEventHash() == mEventHash) {
            return true;
        } else if (mMatchType.startsWith(MATCH_TYPE_STRING) &&
                event.getEventName().equalsIgnoreCase(mEventName) &&
                event.getCustomAttributeStrings() != null &&
                event.getCustomAttributeStrings().containsKey(mAttributeKey) &&
                getAttributeValues().contains(event.getCustomAttributeStrings().get(mAttributeKey).toLowerCase(Locale.US))) {
            return true;
        } else {
            return false;
        }
    }

    private CommerceEvent matchCommerceEvent(EventWrapper.CommerceEventWrapper eventWrapper) {
        CommerceEvent commerceEvent = eventWrapper.getEvent();
        if (commerceEvent == null) {
            return null;
        }
        if (commerceMatchProperty != null && commerceMatchPropertyName != null) {
            if (commerceMatchProperty.equalsIgnoreCase(PROPERTY_LOCATION_EVENT_FIELD)) {
                if (matchCommerceFields(commerceEvent)) {
                    return commerceEvent;
                } else {
                    return null;
                }
            } else if (commerceMatchProperty.equalsIgnoreCase(PROPERTY_LOCATION_EVENT_ATTRIBUTE)) {
                if (matchCommerceAttributes(commerceEvent)) {
                    return commerceEvent;
                } else {
                    return null;
                }
            } else if (commerceMatchProperty.equalsIgnoreCase(PROPERTY_LOCATION_PRODUCT_FIELD)) {
                return matchProductFields(commerceEvent);
            } else if (commerceMatchProperty.equalsIgnoreCase(PROPERTY_LOCATION_PRODUCT_ATTRIBUTE)) {
                return matchProductAttributes(commerceEvent);
            } else if (commerceMatchProperty.equalsIgnoreCase(PROPERTY_LOCATION_PROMOTION_FIELD)) {
                return matchPromotionFields(commerceEvent);
            }
        }
        if (mMatchType.startsWith(MATCH_TYPE_HASH) && eventWrapper.getEventHash() == mEventHash) {
            return commerceEvent;
        }
        return null;
    }

    private CommerceEvent matchPromotionFields(CommerceEvent event) {
        int hash = Integer.parseInt(commerceMatchPropertyName);
        List<Promotion> promotionList = event.getPromotions();
        if (promotionList == null || promotionList.size() == 0) {
            return null;
        }
        List<Promotion> matchedPromotions = new LinkedList<Promotion>();
        Map<String, String> promotionFields = new HashMap<String, String>();
        for (Promotion promotion : promotionList) {
            promotionFields.clear();
            CommerceEventUtils.extractPromotionAttributes(promotion, promotionFields);
            if (promotionFields != null) {
                for (Map.Entry<String, String> entry : promotionFields.entrySet()) {
                    int attributeHash = KitUtils.hashForFiltering(CommerceEventUtils.getEventType(event) + entry.getKey());
                    if (attributeHash == hash) {
                        if (commerceMatchPropertyValues.contains(entry.getValue().toLowerCase(Locale.US))) {
                            matchedPromotions.add(promotion);
                        }
                    }
                }
            }
        }
        if (matchedPromotions.size() == 0) {
            return null;
        } else if (matchedPromotions.size() != promotionList.size()) {
            return new CommerceEvent.Builder(event).promotions(matchedPromotions).build();
        } else {
            return event;
        }
    }

    private CommerceEvent matchProductFields(CommerceEvent event) {
        int hash = Integer.parseInt(commerceMatchPropertyName);
        int type = CommerceEventUtils.getEventType(event);
        List<Product> productList = event.getProducts();
        if (productList == null || productList.size() == 0) {
            return null;
        }
        List<Product> matchedProducts = new LinkedList<Product>();
        Map<String, String> productFields = new HashMap<String, String>();
        for (Product product : productList) {
            productFields.clear();
            CommerceEventUtils.extractProductFields(product, productFields);
            if (productFields != null) {
                for (Map.Entry<String, String> entry : productFields.entrySet()) {
                    int attributeHash = KitUtils.hashForFiltering(type + entry.getKey());
                    if (attributeHash == hash) {
                        if (commerceMatchPropertyValues.contains(entry.getValue().toLowerCase(Locale.US))) {
                            matchedProducts.add(product);
                        }
                    }
                }
            }
        }
        if (matchedProducts.size() == 0) {
            return null;
        } else if (matchedProducts.size() != productList.size()) {
            return new CommerceEvent.Builder(event).products(matchedProducts).build();
        } else {
            return event;
        }
    }

    private CommerceEvent matchProductAttributes(CommerceEvent event) {
        int hash = Integer.parseInt(commerceMatchPropertyName);
        List<Product> productList = event.getProducts();
        if (productList == null || productList.size() == 0) {
            return null;
        }
        List<Product> matchedProducts = new LinkedList<Product>();
        for (Product product : productList) {
            Map<String, String> attributes = product.getCustomAttributes();
            if (attributes != null) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    int attributeHash = KitUtils.hashForFiltering(CommerceEventUtils.getEventType(event) + entry.getKey());
                    if (attributeHash == hash) {
                        if (commerceMatchPropertyValues.contains(entry.getValue().toLowerCase(Locale.US))) {
                            matchedProducts.add(product);
                        }
                    }
                }
            }
        }
        if (matchedProducts.size() == 0) {
            return null;
        } else if (matchedProducts.size() != productList.size()) {
            return new CommerceEvent.Builder(event).products(matchedProducts).build();
        } else {
            return event;
        }
    }

    private boolean matchCommerceAttributes(CommerceEvent event) {
        Map<String, String> attributes = event.getCustomAttributeStrings();
        if (attributes == null || attributes.size() < 1) {
            return false;
        }
        int hash = Integer.parseInt(commerceMatchPropertyName);
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            int attributeHash = KitUtils.hashForFiltering(CommerceEventUtils.getEventType(event) + entry.getKey());
            if (attributeHash == hash) {
                return commerceMatchPropertyValues.contains(entry.getValue().toLowerCase(Locale.US));
            }
        }
        return false;
    }

    private boolean matchCommerceFields(CommerceEvent event) {
        int hash = Integer.parseInt(commerceMatchPropertyName);
        Map<String, String> fields = new HashMap<String, String>();
        CommerceEventUtils.extractActionAttributes(event, fields);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            int fieldHash = KitUtils.hashForFiltering(CommerceEventUtils.getEventType(event) + entry.getKey());
            if (fieldHash == hash) {
                return commerceMatchPropertyValues.contains(entry.getValue().toLowerCase(Locale.US));
            }

        }
        return false;
    }

    public Set<String> getAttributeValues() {
        return mAttributeValues;
    }
}