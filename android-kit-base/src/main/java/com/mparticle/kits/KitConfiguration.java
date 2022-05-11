package com.mparticle.kits;

import android.util.SparseBooleanArray;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.consent.CCPAConsent;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.kits.mappings.CustomMapping;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KitConfiguration {

    private final static int ENTITY_PRODUCT = 1;
    private final static int ENTITY_PROMOTION = 2;
    final static String KEY_ID = "id";
    private final static String KEY_ATTRIBUTE_VALUE_FILTERING = "avf";
    private final static String KEY_PROPERTIES = "as";
    private final static String KEY_FILTERS = "hs";
    private final static String KEY_BRACKETING = "bk";
    private final static String KEY_ATTRIBUTE_VALUE_FILTERING_SHOULD_INCLUDE_MATCHES = "i";
    private final static String KEY_ATTRIBUTE_VALUE_FILTERING_ATTRIBUTE= "a";
    private final static String KEY_ATTRIBUTE_VALUE_FILTERING_VALUE= "v";
    private final static String KEY_EVENT_TYPES_FILTER = "et";
    private final static String KEY_EVENT_NAMES_FILTER = "ec";
    private final static String KEY_EVENT_ATTRIBUTES_FILTER = "ea";
    private final static String KEY_SCREEN_NAME_FILTER = "svec";
    private final static String KEY_SCREEN_ATTRIBUTES_FILTER = "svea";
    private final static String KEY_USER_IDENTITY_FILTER = "uid";
    private final static String KEY_USER_ATTRIBUTE_FILTER = "ua";
    private final static String KEY_EVENT_ATTRIBUTE_ADD_USER = "eaa";
    private final static String KEY_EVENT_ATTRIBUTE_REMOVE_USER = "ear";
    private final static String KEY_EVENT_ATTRIBUTE_SINGLE_ITEM_USER = "eas";
    private final static String KEY_BRACKETING_LOW = "lo";
    private final static String KEY_BRACKETING_HIGH = "hi";
    private final static String KEY_COMMERCE_ATTRIBUTE_FILTER = "cea";
    private final static String KEY_COMMERCE_ENTITY_FILTERS = "ent";
    private final static String KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS = "afa";
    private final static String KEY_CONSENT_FORWARDING_RULES = "crvf";
    private final static String KEY_CONSENT_FORWARDING_RULES_SHOULD_INCLUDE_MATCHES = "i";
    private final static String KEY_CONSENT_FORWARDING_RULES_ARRAY = "v";
    private final static String KEY_CONSENT_FORWARDING_RULES_VALUE_CONSENTED = "c";
    private final static String KEY_CONSENT_FORWARDING_RULES_VALUE_HASH = "h";
    private final static String KEY_EXCLUDE_ANONYMOUS_USERS = "eau";

    //If set to true, our sdk honor user's optout wish. If false, we still collect data on opt-ed out users, but only for reporting.
    private final static String HONOR_OPT_OUT = "honorOptOut";
    private final static String KEY_PROJECTIONS = "pr";
    private boolean avfIsActive = false;
    private boolean avfShouldIncludeMatches = false;
    protected boolean consentForwardingIncludeMatches = false;
    private int avfHashedAttribute = 0;
    private int avfHashedValue = 0;
    private HashMap<String, String> settings = new HashMap<String, String>(0);
    protected SparseBooleanArray mTypeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mNameFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mScreenNameFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mScreenAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mUserIdentityFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mUserAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mCommerceAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mCommerceEntityFilters = new SparseBooleanArray(0);
    protected Map<Integer, String> mAttributeAddToUser = new HashMap<Integer, String>();
    protected Map<Integer, String> mAttributeRemoveFromUser = new HashMap<Integer, String>();
    protected Map<Integer, String> mAttributeSingleItemUser = new HashMap<Integer, String>();
    private Map<Integer, SparseBooleanArray> mCommerceEntityAttributeFilters = new HashMap<Integer, SparseBooleanArray>(0);
    protected Map<Integer, Boolean> mConsentForwardingRules = new HashMap<Integer, Boolean>();
    private int lowBracket = 0;
    private int highBracket = 101;
    private LinkedList<CustomMapping> customMappingList;
    private CustomMapping defaultCustomMapping = null;
    private CustomMapping defaultScreenCustomMapping = null;
    private CustomMapping defaultCommerceCustomMapping = null;
    private int kitId;
    protected boolean mExcludeAnonymousUsers;

    public SparseBooleanArray getEventTypeFilters() {
        return mTypeFilters;
    }

    public SparseBooleanArray getEventNameFilters() {
        return mNameFilters;
    }

    public SparseBooleanArray getEventAttributeFilters() {
        return mAttributeFilters;
    }

    public SparseBooleanArray getScreenNameFilters() {
        return mScreenNameFilters;
    }

    public SparseBooleanArray getScreenAttributeFilters() {
        return mScreenAttributeFilters;
    }

    public static KitConfiguration createKitConfiguration(JSONObject json) throws JSONException{
        return new KitConfiguration().parseConfiguration(json);
    }

    public KitConfiguration() {
        super();
    }

    public KitConfiguration parseConfiguration(JSONObject json) throws JSONException {
        kitId = json.getInt(KEY_ID);
        if (json.has(KEY_ATTRIBUTE_VALUE_FILTERING)) {
            avfIsActive = true;
            try {
                JSONObject avfJson = json.getJSONObject(KEY_ATTRIBUTE_VALUE_FILTERING);
                avfShouldIncludeMatches = avfJson.getBoolean(KEY_ATTRIBUTE_VALUE_FILTERING_SHOULD_INCLUDE_MATCHES);
                avfHashedAttribute = avfJson.getInt(KEY_ATTRIBUTE_VALUE_FILTERING_ATTRIBUTE);
                avfHashedValue = avfJson.getInt(KEY_ATTRIBUTE_VALUE_FILTERING_VALUE);
            } catch (JSONException jse) {
                Logger.error("Issue when parsing attribute value filtering configuration: " + jse.getMessage());
                avfIsActive = false;
            }
        }
        if (json.has(KEY_PROPERTIES)) {
            JSONObject propJson = json.getJSONObject(KEY_PROPERTIES);
            for (Iterator<String> iterator = propJson.keys(); iterator.hasNext(); ) {
                String key = iterator.next();
                if (!propJson.isNull(key)) {
                    settings.put(key, propJson.optString(key));
                }
            }
        }
        if (json.has(KEY_FILTERS)) {
            JSONObject filterJson = json.getJSONObject(KEY_FILTERS);
            if (filterJson.has(KEY_EVENT_TYPES_FILTER)) {
                mTypeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_EVENT_TYPES_FILTER));
            } else {
                mTypeFilters.clear();
            }
            if (filterJson.has(KEY_EVENT_NAMES_FILTER)) {
                mNameFilters = convertToSparseArray(filterJson.getJSONObject(KEY_EVENT_NAMES_FILTER));
            } else {
                mNameFilters.clear();
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTES_FILTER)) {
                mAttributeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_EVENT_ATTRIBUTES_FILTER));
            } else {
                mAttributeFilters.clear();
            }
            if (filterJson.has(KEY_SCREEN_NAME_FILTER)) {
                mScreenNameFilters = convertToSparseArray(filterJson.getJSONObject(KEY_SCREEN_NAME_FILTER));
            } else {
                mScreenNameFilters.clear();
            }
            if (filterJson.has(KEY_SCREEN_ATTRIBUTES_FILTER)) {
                mScreenAttributeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_SCREEN_ATTRIBUTES_FILTER));
            } else {
                mScreenAttributeFilters.clear();
            }
            if (filterJson.has(KEY_USER_IDENTITY_FILTER)) {
                mUserIdentityFilters = convertToSparseArray(filterJson.getJSONObject(KEY_USER_IDENTITY_FILTER));
            } else {
                mUserIdentityFilters.clear();
            }
            if (filterJson.has(KEY_USER_ATTRIBUTE_FILTER)) {
                mUserAttributeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_USER_ATTRIBUTE_FILTER));
            } else {
                mUserAttributeFilters.clear();
            }
            if (filterJson.has(KEY_COMMERCE_ATTRIBUTE_FILTER)) {
                mCommerceAttributeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_COMMERCE_ATTRIBUTE_FILTER));
            } else {
                mCommerceAttributeFilters.clear();
            }
            if (filterJson.has(KEY_COMMERCE_ENTITY_FILTERS)) {
                mCommerceEntityFilters = convertToSparseArray(filterJson.getJSONObject(KEY_COMMERCE_ENTITY_FILTERS));
            } else {
                mCommerceEntityFilters.clear();
            }
            if (filterJson.has(KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS)) {
                JSONObject entityAttributeFilters = filterJson.getJSONObject(KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS);
                mCommerceEntityAttributeFilters.clear();
                Iterator<String> keys = entityAttributeFilters.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    mCommerceEntityAttributeFilters.put(Integer.parseInt(key), convertToSparseArray(entityAttributeFilters.getJSONObject(key)));
                }
            } else {
                mCommerceEntityAttributeFilters.clear();
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTE_ADD_USER)) {
                mAttributeAddToUser = convertToSparseMap(filterJson.getJSONObject(KEY_EVENT_ATTRIBUTE_ADD_USER));
            } else {
                mAttributeAddToUser.clear();
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTE_REMOVE_USER)) {
                mAttributeRemoveFromUser = convertToSparseMap(filterJson.getJSONObject(KEY_EVENT_ATTRIBUTE_REMOVE_USER));
            } else {
                mAttributeRemoveFromUser.clear();
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTE_SINGLE_ITEM_USER)) {
                mAttributeSingleItemUser = convertToSparseMap(filterJson.getJSONObject(KEY_EVENT_ATTRIBUTE_SINGLE_ITEM_USER));
            } else {
                mAttributeSingleItemUser.clear();
            }
        }

        if (json.has(KEY_BRACKETING)) {
            JSONObject bracketing = json.getJSONObject(KEY_BRACKETING);
            lowBracket = bracketing.optInt(KEY_BRACKETING_LOW, 0);
            highBracket = bracketing.optInt(KEY_BRACKETING_HIGH, 101);
        } else {
            lowBracket = 0;
            highBracket = 101;
        }
        customMappingList = new LinkedList<CustomMapping>();
        defaultCustomMapping = null;
        if (json.has(KEY_PROJECTIONS)) {
            JSONArray projections = json.getJSONArray(KEY_PROJECTIONS);
            for (int i = 0; i < projections.length(); i++) {
                CustomMapping customMapping = new CustomMapping(projections.getJSONObject(i));
                if (customMapping.isDefault()) {
                    if (customMapping.getMessageType() == 4) {
                        defaultCustomMapping = customMapping;
                    } else if (customMapping.getMessageType() == 3) {
                        defaultScreenCustomMapping = customMapping;
                    } else {
                        defaultCommerceCustomMapping = customMapping;
                    }
                } else {
                    customMappingList.add(customMapping);
                }
            }
        }
        mConsentForwardingRules.clear();
        if (json.has(KEY_CONSENT_FORWARDING_RULES)) {
            JSONObject consentForwardingRule = json.getJSONObject(KEY_CONSENT_FORWARDING_RULES);
            consentForwardingIncludeMatches = consentForwardingRule.optBoolean(KEY_CONSENT_FORWARDING_RULES_SHOULD_INCLUDE_MATCHES);
            JSONArray rules = consentForwardingRule.getJSONArray(KEY_CONSENT_FORWARDING_RULES_ARRAY);
            for (int i = 0; i < rules.length(); i++) {
                mConsentForwardingRules.put(
                        rules.getJSONObject(i).getInt(KEY_CONSENT_FORWARDING_RULES_VALUE_HASH),
                        rules.getJSONObject(i).getBoolean(KEY_CONSENT_FORWARDING_RULES_VALUE_CONSENTED)
                );
            }
        }
        mExcludeAnonymousUsers = json.optBoolean(KEY_EXCLUDE_ANONYMOUS_USERS, false);
        return this;
    }

    boolean shouldIncludeFromAttributeValueFiltering(Map<String, String> attributes) {
        boolean shouldInclude = true;
        if (avfIsActive) {
            boolean isMatch = false;
            if (attributes != null) {
                Iterator<Map.Entry<String, String>> attIterator = attributes.entrySet().iterator();
                while (attIterator.hasNext()) {
                    Map.Entry<String, String> entry = attIterator.next();
                    String key = entry.getKey();
                    int keyHash = KitUtils.hashForFiltering(key);
                    if (keyHash == avfHashedAttribute) {
                        String value = entry.getValue();
                        int valueHash = KitUtils.hashForFiltering(value);
                        if (valueHash == avfHashedValue) {
                            isMatch = true;
                        }
                        break;
                    }
                }
            }
            shouldInclude = avfShouldIncludeMatches ? isMatch : !isMatch;
        }
        return shouldInclude;
    }


    boolean shouldIncludeFromConsentRules(MParticleUser user) {
        if (mConsentForwardingRules.size() == 0) {
            return true;
        }
        //if we don't have a user, but there are rules, be safe and exclude
        if (user == null) {
            return false;
        }
        boolean isMatch = isConsentStateFilterMatch(user.getConsentState());
        return consentForwardingIncludeMatches == isMatch;
    }

    /**
     * This method indicates if the given consent state matches any of the Consent
     * forwarding rules.
     *
     * @param consentState
     * @return
     */
    public boolean isConsentStateFilterMatch(ConsentState consentState) {
        if (mConsentForwardingRules.size() == 0
                || consentState == null
                || (consentState.getGDPRConsentState().size() == 0 && consentState.getCCPAConsentState() == null)) {
            return false;
        }
        Map<String, GDPRConsent> gdprConsentState = consentState.getGDPRConsentState();
        for (Map.Entry<String, GDPRConsent> gdprConsent : gdprConsentState.entrySet()) {
            int consentPurposeHash = KitUtils.hashForFiltering("1" + gdprConsent.getKey());
            Boolean consented = mConsentForwardingRules.get(consentPurposeHash);
            if (consented != null && consented == gdprConsent.getValue().isConsented()) {
                return true;
            }
        }
        CCPAConsent ccpaConsent = consentState.getCCPAConsentState();
        if (ccpaConsent != null) {
            int consentPurposeHash = KitUtils.hashForFiltering("2" + Constants.MessageKey.CCPA_CONSENT_KEY);
            Boolean consented = mConsentForwardingRules.get(consentPurposeHash);
            if (consented != null && consented == ccpaConsent.isConsented()) {
                return true;
            }
         }
        return false;
    }

    protected CommerceEvent filterCommerceEvent(CommerceEvent event) {
        if (!shouldIncludeFromAttributeValueFiltering(event.getCustomAttributeStrings())) {
            return null;
        }
        if (mTypeFilters != null &&
                !mTypeFilters.get(KitUtils.hashForFiltering(CommerceEventUtils.getEventType(event) + ""), true)) {
            return null;
        }
        CommerceEvent filteredEvent = new CommerceEvent.Builder(event).build();
        filteredEvent = filterCommerceEntities(filteredEvent);
        filteredEvent = filterCommerceEntityAttributes(filteredEvent);
        filteredEvent = filterCommerceEventAttributes(filteredEvent);
        return filteredEvent;
    }


    private CommerceEvent filterCommerceEntityAttributes(CommerceEvent filteredEvent) {
        if (mCommerceEntityAttributeFilters == null || mCommerceEntityAttributeFilters.size() == 0) {
            return filteredEvent;
        }
        CommerceEvent.Builder builder = new CommerceEvent.Builder(filteredEvent);
        for (Map.Entry<Integer, SparseBooleanArray> entry : mCommerceEntityAttributeFilters.entrySet()) {
            int entity = entry.getKey();
            SparseBooleanArray filters = entry.getValue();
            switch (entity) {
                case ENTITY_PRODUCT:
                    if (filteredEvent.getProducts() != null && filteredEvent.getProducts().size() > 0) {
                        List<Product> filteredProducts = new LinkedList<Product>();
                        for (Product product : filteredEvent.getProducts()) {
                            Product.Builder productBuilder = new Product.Builder(product);
                            if (product.getCustomAttributes() != null && product.getCustomAttributes().size() > 0) {
                                HashMap<String, String> filteredCustomAttributes = new HashMap<String, String>(product.getCustomAttributes().size());
                                for (Map.Entry<String, String> customAttribute : product.getCustomAttributes().entrySet()) {
                                    if (filters.get(KitUtils.hashForFiltering(customAttribute.getKey()), true)) {
                                        filteredCustomAttributes.put(customAttribute.getKey(), customAttribute.getValue());
                                    }
                                }
                                productBuilder.customAttributes(filteredCustomAttributes);
                            }
                            if (!MPUtility.isEmpty(product.getCouponCode()) && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PRODUCT_COUPON_CODE), true)) {
                                productBuilder.couponCode(product.getCouponCode());
                            } else {
                                productBuilder.couponCode(null);
                            }
                            if (product.getPosition() != null && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PRODUCT_POSITION), true)) {
                                productBuilder.position(product.getPosition());
                            } else {
                                productBuilder.position(null);
                            }
                            if (!MPUtility.isEmpty(product.getVariant()) && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PRODUCT_VARIANT), true)) {
                                productBuilder.variant(product.getVariant());
                            } else {
                                productBuilder.variant(null);
                            }
                            if (!MPUtility.isEmpty(product.getCategory()) && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PRODUCT_CATEGORY), true)) {
                                productBuilder.category(product.getCategory());
                            } else {
                                productBuilder.category(null);
                            }
                            if (!MPUtility.isEmpty(product.getBrand()) && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PRODUCT_BRAND), true)) {
                                productBuilder.brand(product.getBrand());
                            } else {
                                productBuilder.brand(null);
                            }
                            filteredProducts.add(productBuilder.build());
                        }
                        builder.products(filteredProducts);
                    }
                    break;
                case ENTITY_PROMOTION:
                    if (filteredEvent.getPromotions() != null && filteredEvent.getPromotions().size() > 0) {
                        List<Promotion> filteredPromotions = new LinkedList<Promotion>();
                        for (Promotion promotion : filteredEvent.getPromotions()) {
                            Promotion filteredPromotion = new Promotion();
                            if (!MPUtility.isEmpty(promotion.getId()) && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PROMOTION_ID), true)) {
                                filteredPromotion.setId(promotion.getId());
                            }
                            if (!MPUtility.isEmpty(promotion.getCreative()) && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PROMOTION_CREATIVE), true)) {
                                filteredPromotion.setCreative(promotion.getCreative());
                            }
                            if (!MPUtility.isEmpty(promotion.getName()) && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PROMOTION_NAME), true)) {
                                filteredPromotion.setName(promotion.getName());
                            }
                            if (!MPUtility.isEmpty(promotion.getPosition()) && filters.get(KitUtils.hashForFiltering(CommerceEventUtils.Constants.ATT_PROMOTION_POSITION), true)) {
                                filteredPromotion.setPosition(promotion.getPosition());
                            }
                            filteredPromotions.add(filteredPromotion);
                        }
                        builder.promotions(filteredPromotions);
                    }

                    break;
            }
        }
        return builder.build();
    }

    public final Map<String, String> filterEventAttributes(MPEvent event) {
        return filterEventAttributes(event.getEventType(), event.getEventName(), mAttributeFilters, event.getCustomAttributeStrings());
    }

    public final Map<String, String> filterScreenAttributes(MParticle.EventType eventType, String eventName, Map<String, String> eventAttributes) {
        return filterEventAttributes(eventType, eventName, mScreenNameFilters, eventAttributes);
    }

    public final Map<String, String> filterEventAttributes(MParticle.EventType eventType, String eventName, SparseBooleanArray filter, Map<String, String> eventAttributes) {
        if (eventAttributes != null && eventAttributes.size() > 0 && filter != null && filter.size() > 0) {
            String eventTypeStr = "0";
            if (eventType != null) {
                eventTypeStr = eventType.ordinal() + "";
            }
            Iterator<Map.Entry<String, String>> attIterator = eventAttributes.entrySet().iterator();
            Map<String, String> newAttributes = new HashMap<String, String>();
            while (attIterator.hasNext()) {
                Map.Entry<String, String> entry = attIterator.next();
                String key = entry.getKey();
                int hash = KitUtils.hashForFiltering(eventTypeStr + eventName + key);
                if (filter.get(hash, true)) {
                    newAttributes.put(key, entry.getValue());
                }
            }
            return newAttributes;
        } else {
            return eventAttributes;
        }
    }

    private CommerceEvent filterCommerceEntities(CommerceEvent filteredEvent) {
        if (mCommerceEntityFilters == null || mCommerceEntityFilters.size() == 0) {
            return filteredEvent;
        }
        CommerceEvent.Builder builder = new CommerceEvent.Builder(filteredEvent);

        boolean removeProducts = !mCommerceEntityFilters.get(ENTITY_PRODUCT, true);
        boolean removePromotions = !mCommerceEntityFilters.get(ENTITY_PROMOTION, true);
        if (removeProducts) {
            builder.products(new LinkedList<Product>());
            List<Impression> impressionList = filteredEvent.getImpressions();
            if (impressionList != null) {
                builder.impressions(null);
                for (Impression impression : impressionList) {
                    builder.addImpression(new Impression(impression.getListName(), null));
                }
            }
        }
        if (removePromotions) {
            builder.promotions(new LinkedList<Promotion>());
        }
        return builder.build();
    }

    public static final Map<String, ?> filterAttributes(SparseBooleanArray attributeFilters, Map<String, ?> attributes) {
        if (attributes != null && attributeFilters != null && attributeFilters.size() > 0
                && attributes.size() > 0) {
            Map newAttributes = new HashMap(attributes.size());
            for (Map.Entry<String, ?> attribute : attributes.entrySet()) {
                if ( shouldForwardAttribute(attributeFilters, attribute.getKey())) {
                    newAttributes.put(attribute.getKey(), attribute.getValue());
                }
            }
            return newAttributes;
        } else {
            return attributes;
        }
    }

    public static final boolean shouldForwardAttribute(SparseBooleanArray attributeFilters, String key) {
        int hash = KitUtils.hashForFiltering(key);
        return attributeFilters.get(hash, true);
    }

    private CommerceEvent filterCommerceEventAttributes(CommerceEvent filteredEvent) {
        String eventType = Integer.toString(CommerceEventUtils.getEventType(filteredEvent));
        if (mCommerceAttributeFilters == null || mCommerceAttributeFilters.size() == 0) {
            return filteredEvent;
        }
        CommerceEvent.Builder builder = new CommerceEvent.Builder(filteredEvent);
        Map<String, String> customAttributes = filteredEvent.getCustomAttributeStrings();
        if (customAttributes != null) {
            Map<String, String> filteredCustomAttributes = new HashMap<String, String>(customAttributes.size());
            for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
                if (mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + entry.getKey()), true)) {
                    filteredCustomAttributes.put(entry.getKey(), entry.getValue());
                }
            }
            builder.customAttributes(filteredCustomAttributes);
        }

        if (filteredEvent.getCheckoutStep() != null &&
                !mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + CommerceEventUtils.Constants.ATT_ACTION_CHECKOUT_STEP), true)) {
            builder.checkoutStep(null);
        }
        if (filteredEvent.getCheckoutOptions() != null &&
                !mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + CommerceEventUtils.Constants.ATT_ACTION_CHECKOUT_OPTIONS), true)) {
            builder.checkoutOptions(null);
        }
        TransactionAttributes attributes = filteredEvent.getTransactionAttributes();
        if (attributes != null) {
            if (attributes.getCouponCode() != null &&
                    !mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + CommerceEventUtils.Constants.ATT_TRANSACTION_COUPON_CODE), true)) {
                attributes.setCouponCode(null);
            }
            if (attributes.getShipping() != null &&
                    !mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + CommerceEventUtils.Constants.ATT_SHIPPING), true)) {
                attributes.setShipping(null);
            }
            if (attributes.getTax() != null &&
                    !mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + CommerceEventUtils.Constants.ATT_TAX), true)) {
                attributes.setTax(null);
            }
            if (attributes.getRevenue() != null &&
                    !mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + CommerceEventUtils.Constants.ATT_TOTAL), true)) {
                attributes.setRevenue(0.0);
            }
            if (attributes.getId() != null &&
                    !mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + CommerceEventUtils.Constants.ATT_TRANSACTION_ID), true)) {
                attributes.setId(null);
            }
            if (attributes.getAffiliation() != null &&
                    !mCommerceAttributeFilters.get(KitUtils.hashForFiltering(eventType + CommerceEventUtils.Constants.ATT_AFFILIATION), true)) {
                attributes.setAffiliation(null);
            }
            builder.transactionAttributes(attributes);
        }

        return builder.build();
    }

    public boolean shouldLogScreen(String screenName) {
        int nameHash = KitUtils.hashForFiltering("0" + screenName);
        if (mScreenNameFilters.size() > 0 && !mScreenNameFilters.get(nameHash, true)) {
            return false;
        }
        return true;
    }


    protected boolean shouldLogEvent(MPEvent event) {
        if (!shouldIncludeFromAttributeValueFiltering(event.getCustomAttributeStrings())) {
            return false;
        }
        int typeHash = KitUtils.hashForFiltering(event.getEventType().ordinal() + "");
        return mTypeFilters.get(typeHash, true) && mNameFilters.get(event.getEventHash(), true);
    }

    public boolean passesBracketing(int userBucket) {
        return userBucket >= lowBracket && userBucket < highBracket;
    }

    protected SparseBooleanArray convertToSparseArray(JSONObject json) {
        SparseBooleanArray map = new SparseBooleanArray();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext(); ) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getInt(key) == 1);
            } catch (JSONException jse) {
                Logger.error("Issue while parsing kit configuration: " + jse.getMessage());
            }
        }
        return map;
    }

    protected Map<Integer, String> convertToSparseMap(JSONObject json) {
        Map<Integer, String> map = new HashMap<Integer, String>();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext(); ) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getString(key));
            } catch (JSONException jse) {
                Logger.error("Issue while parsing kit configuration: " + jse.getMessage());
            }
        }
        return map;
    }

    public final List<CustomMapping> getCustomMappingList() {
        return customMappingList;
    }

    public final CustomMapping getDefaultEventProjection() {
        return defaultCustomMapping;
    }

    public final CustomMapping getDefaultCommerceCustomMapping() {
        return defaultCommerceCustomMapping;
    }

    public final CustomMapping getDefaultScreenCustomMapping() {
        return defaultScreenCustomMapping;
    }

    public int getKitId() {
        return kitId;
    }

    public SparseBooleanArray getUserIdentityFilters() {
        return mUserIdentityFilters;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public boolean shouldHonorOptOut() {
        if (settings.containsKey(HONOR_OPT_OUT)) {
            String optOut = settings.get(HONOR_OPT_OUT);
            return Boolean.parseBoolean(optOut);
        }
        return true;
    }

    boolean shouldSetIdentity(MParticle.IdentityType identityType) {
        SparseBooleanArray userIdentityFilters = getUserIdentityFilters();
        return  userIdentityFilters == null ||
                userIdentityFilters.size() == 0 ||
                userIdentityFilters.get(identityType.getValue(), true);
    }

    public SparseBooleanArray getUserAttributeFilters() {
        return mUserAttributeFilters;
    }

    public SparseBooleanArray getCommerceAttributeFilters() {
        return mCommerceAttributeFilters;
    }

    public SparseBooleanArray getCommerceEntityFilters() {
        return mCommerceEntityFilters;
    }

    public Map<Integer, SparseBooleanArray> getCommerceEntityAttributeFilters() {
        return mCommerceEntityAttributeFilters;
    }

    public Map<Integer, String> getEventAttributesAddToUser() {
        return mAttributeAddToUser;
    }

    public Map<Integer, String> getEventAttributesRemoveFromUser() {
        return mAttributeRemoveFromUser;
    }

    public Map<Integer, String> getEventAttributesSingleItemUser() {
        return mAttributeSingleItemUser;
    }

    public boolean isAttributeValueFilteringActive() {
        return avfIsActive;
    }

    public boolean isAvfShouldIncludeMatches() {
        return avfShouldIncludeMatches;
    }

    public int getAvfHashedAttribute() {
        return avfHashedAttribute;
    }

    public int getAvfHashedValue() {
        return avfHashedValue;
    }

    public int getLowBracket() {
        return lowBracket;
    }

    public int getHighBracket() {
        return highBracket;
    }

    boolean excludeAnonymousUsers() {
        return mExcludeAnonymousUsers;
    }

    public boolean shouldExcludeUser(MParticleUser user) {
        return mExcludeAnonymousUsers && (user == null || !user.isLoggedIn());
    }
}
