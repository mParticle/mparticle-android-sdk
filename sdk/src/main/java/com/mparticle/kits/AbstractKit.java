package com.mparticle.kits;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.SparseBooleanArray;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base AbstractKit - all EKs subclass this. The primary function is to parse the common EK configuration structures
 * such as filters.
 */
public abstract class AbstractKit {

    final static String KEY_ID = "id";
    private final static String KEY_PROPERTIES = "as";
    private final static String KEY_FILTERS = "hs";
    private final static String KEY_BRACKETING = "bk";
    private final static String KEY_EVENT_TYPES_FILTER = "et";
    private final static String KEY_EVENT_NAMES_FILTER = "ec";
    private final static String KEY_EVENT_ATTRIBUTES_FILTER = "ea";
    private final static String KEY_SCREEN_NAME_FILTER = "svec";
    private final static String KEY_SCREEN_ATTRIBUTES_FILTER = "svea";
    private final static String KEY_USER_IDENTITY_FILTER = "uid";
    private final static String KEY_USER_ATTRIBUTE_FILTER = "ua";
    private final static String KEY_BRACKETING_LOW = "lo";
    private final static String KEY_BRACKETING_HIGH = "hi";
    private final static String KEY_COMMERCE_ATTRIBUTE_FILTER = "cea";
    private final static String KEY_COMMERCE_ENTITY_FILERS = "ent";
    private final static String KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS = "afa";

    //If set to true, our sdk honor user's optout wish. If false, we still collect data on opt-ed out users, but only for reporting
    private static final String HONOR_OPT_OUT = "honorOptOut";
    private static final String KEY_PROJECTIONS = "pr";
    protected KitManager mEkManager;

    protected HashMap<String, String> properties = new HashMap<String, String>(0);
    protected SparseBooleanArray mTypeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mNameFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mScreenNameFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mScreenAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mUserIdentityFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mUserAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mCommerceAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mCommerceEntityFilters = new SparseBooleanArray(0);
    protected Map<Integer, SparseBooleanArray> mCommerceEntityAttributeFilters = new HashMap<Integer, SparseBooleanArray>(0);
    protected int lowBracket = 0;
    protected int highBracket = 101;

    protected Context context;
    private boolean mRunning = true;
    LinkedList<Projection> projectionList;
    Projection defaultProjection = null;
    Projection defaultScreenProjection = null;
    Projection defaultCommerceProjection = null;
    private int kitId;

    protected final boolean isBackgrounded() {
        return mEkManager.getAppStateManager().isBackgrounded();
    }

    protected AbstractKit parseConfig(JSONObject json) throws JSONException {

        if (json.has(KEY_PROPERTIES)) {
            JSONObject propJson = json.getJSONObject(KEY_PROPERTIES);
            for (Iterator<String> iterator = propJson.keys(); iterator.hasNext(); ) {
                String key = iterator.next();
                properties.put(key, propJson.optString(key));
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
            if (filterJson.has(KEY_COMMERCE_ENTITY_FILERS)) {
                mCommerceEntityFilters = convertToSparseArray(filterJson.getJSONObject(KEY_COMMERCE_ENTITY_FILERS));
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
        }

        if (json.has(KEY_BRACKETING)) {
            JSONObject bracketing = json.getJSONObject(KEY_BRACKETING);
            lowBracket = bracketing.optInt(KEY_BRACKETING_LOW, 0);
            highBracket = bracketing.optInt(KEY_BRACKETING_HIGH, 101);
        } else {
            lowBracket = 0;
            highBracket = 101;
        }
        projectionList = new LinkedList<Projection>();
        defaultProjection = null;
        if (json.has(KEY_PROJECTIONS)) {
            JSONArray projections = json.getJSONArray(KEY_PROJECTIONS);
            for (int i = 0; i < projections.length(); i++) {
                Projection projection = new Projection(projections.getJSONObject(i));
                if (projection.isDefault()) {
                    if (projection.getMessageType() == 4) {
                        defaultProjection = projection;
                    } else if (projection.getMessageType() == 3) {
                        defaultScreenProjection = projection;
                    } else {
                        defaultCommerceProjection = projection;
                    }
                } else {
                    projectionList.add(projection);
                }
            }
        }

        return this;
    }

    public static BigInteger hashFnv1a(byte[] bytes) {
        return MPUtility.hashFnv1A(bytes);
    }

    protected SparseBooleanArray convertToSparseArray(JSONObject json) {
        SparseBooleanArray map = new SparseBooleanArray();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext(); ) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getInt(key) == 1);
            } catch (JSONException jse) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "Issue while parsing kit configuration: " + jse.getMessage());
            }
        }
        return map;
    }

    private boolean shouldHonorOptOut() {
        if (properties.containsKey(HONOR_OPT_OUT)) {
            String optOut = properties.get(HONOR_OPT_OUT);
            return Boolean.parseBoolean(optOut);
        }
        return true;
    }

    public boolean disabled() {
        return !passesBracketing()
                || (shouldHonorOptOut() && !mEkManager.getConfigurationManager().isEnabled());
    }

    private boolean passesBracketing() {
        int userBucket = mEkManager.getConfigurationManager().getUserBucket();
        return userBucket >= lowBracket && userBucket < highBracket;
    }

    protected boolean shouldLogEvent(MPEvent event) {
        int typeHash = MPUtility.mpHash(event.getEventType().ordinal() + "");
        return mTypeFilters.get(typeHash, true) && mNameFilters.get(event.getEventHash(), true);
    }

    protected CommerceEvent filterCommerceEvent(CommerceEvent event) {
        if (mTypeFilters != null &&
                !mTypeFilters.get(MPUtility.mpHash(CommerceEventUtil.getEventType(event) + ""), true)) {
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
                case Constants.Commerce.ENTITY_PRODUCT:
                    if (filteredEvent.getProducts() != null && filteredEvent.getProducts().size() > 0) {
                        List<Product> filteredProducts = new LinkedList<Product>();
                        for (Product product : filteredEvent.getProducts()) {
                            Product.Builder productBuilder = new Product.Builder(product);
                            if (product.getCustomAttributes() != null && product.getCustomAttributes().size() > 0) {
                                HashMap<String, String> filteredCustomAttributes = new HashMap<String, String>(product.getCustomAttributes().size());
                                for (Map.Entry<String, String> customAttribute : product.getCustomAttributes().entrySet()) {
                                    if (filters.get(MPUtility.mpHash(customAttribute.getKey()), true)) {
                                        filteredCustomAttributes.put(customAttribute.getKey(), customAttribute.getValue());
                                    }
                                }
                                productBuilder.customAttributes(filteredCustomAttributes);
                            }
                            if (!MPUtility.isEmpty(product.getCouponCode()) && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PRODUCT_COUPON_CODE), true)) {
                                productBuilder.couponCode(product.getCouponCode());
                            } else {
                                productBuilder.couponCode(null);
                            }
                            if (product.getPosition() != null && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PRODUCT_POSITION), true)) {
                                productBuilder.position(product.getPosition());
                            } else {
                                productBuilder.position(null);
                            }
                            if (!MPUtility.isEmpty(product.getVariant()) && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PRODUCT_VARIANT), true)) {
                                productBuilder.variant(product.getVariant());
                            } else {
                                productBuilder.variant(null);
                            }
                            if (!MPUtility.isEmpty(product.getCategory()) && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PRODUCT_CATEGORY), true)) {
                                productBuilder.category(product.getCategory());
                            } else {
                                productBuilder.category(null);
                            }
                            if (!MPUtility.isEmpty(product.getBrand()) && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PRODUCT_BRAND), true)) {
                                productBuilder.brand(product.getBrand());
                            } else {
                                productBuilder.brand(null);
                            }
                            filteredProducts.add(productBuilder.build());
                        }
                        builder.products(filteredProducts);
                    }
                    break;
                case Constants.Commerce.ENTITY_PROMOTION:
                    if (filteredEvent.getPromotions() != null && filteredEvent.getPromotions().size() > 0) {
                        List<Promotion> filteredPromotions = new LinkedList<Promotion>();
                        for (Promotion promotion : filteredEvent.getPromotions()) {
                            Promotion filteredPromotion = new Promotion();
                            if (!MPUtility.isEmpty(promotion.getId()) && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PROMOTION_ID), true)) {
                                filteredPromotion.setId(promotion.getId());
                            }
                            if (!MPUtility.isEmpty(promotion.getCreative()) && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PROMOTION_CREATIVE), true)) {
                                filteredPromotion.setCreative(promotion.getCreative());
                            }
                            if (!MPUtility.isEmpty(promotion.getName()) && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PROMOTION_NAME), true)) {
                                filteredPromotion.setName(promotion.getName());
                            }
                            if (!MPUtility.isEmpty(promotion.getPosition()) && filters.get(MPUtility.mpHash(Constants.Commerce.ATT_PROMOTION_POSITION), true)) {
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

    private CommerceEvent filterCommerceEntities(CommerceEvent filteredEvent) {
        if (mCommerceEntityFilters == null || mCommerceEntityFilters.size() == 0) {
            return filteredEvent;
        }
        CommerceEvent.Builder builder = new CommerceEvent.Builder(filteredEvent);

        boolean removeProducts = !mCommerceEntityFilters.get(Constants.Commerce.ENTITY_PRODUCT, true);
        boolean removePromotions = !mCommerceEntityFilters.get(Constants.Commerce.ENTITY_PROMOTION, true);
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

    private CommerceEvent filterCommerceEventAttributes(CommerceEvent filteredEvent) {
        String eventType = Integer.toString(CommerceEventUtil.getEventType(filteredEvent));
        if (mCommerceAttributeFilters == null || mCommerceAttributeFilters.size() == 0) {
            return filteredEvent;
        }
        CommerceEvent.Builder builder = new CommerceEvent.Builder(filteredEvent);
        Map<String, String> customAttributes = filteredEvent.getCustomAttributes();
        if (customAttributes != null) {
            Map<String, String> filteredCustomAttributes = new HashMap<String, String>(customAttributes.size());
            for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
                if (mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + entry.getKey()), true)) {
                    filteredCustomAttributes.put(entry.getKey(), entry.getValue());
                }
            }
            builder.customAttributes(filteredCustomAttributes);
        }

        if (filteredEvent.getCheckoutStep() != null &&
                !mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + Constants.Commerce.ATT_ACTION_CHECKOUT_STEP), true)) {
            builder.checkoutStep(null);
        }
        if (filteredEvent.getCheckoutOptions() != null &&
                !mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + Constants.Commerce.ATT_ACTION_CHECKOUT_OPTIONS), true)) {
            builder.checkoutOptions(null);
        }
        TransactionAttributes attributes = filteredEvent.getTransactionAttributes();
        if (attributes != null) {
            if (attributes.getCouponCode() != null &&
                    !mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + Constants.Commerce.ATT_TRANSACTION_COUPON_CODE), true)) {
                attributes.setCouponCode(null);
            }
            if (attributes.getShipping() != null &&
                    !mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + Constants.Commerce.ATT_SHIPPING), true)) {
                attributes.setShipping(null);
            }
            if (attributes.getTax() != null &&
                    !mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + Constants.Commerce.ATT_TAX), true)) {
                attributes.setTax(null);
            }
            if (attributes.getRevenue() != null &&
                    !mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + Constants.Commerce.ATT_TOTAL), true)) {
                attributes.setRevenue(null);
            }
            if (attributes.getId() != null &&
                    !mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + Constants.Commerce.ATT_TRANSACTION_ID), true)) {
                attributes.setId(null);
            }
            if (attributes.getAffiliation() != null &&
                    !mCommerceAttributeFilters.get(MPUtility.mpHash(eventType + Constants.Commerce.ATT_AFFILIATION), true)) {
                attributes.setAffiliation(null);
            }
            builder.transactionAttributes(attributes);
        }

        return builder.build();
    }

    public boolean shouldLogScreen(String screenName) {
        int nameHash = MPUtility.mpHash("0" + screenName);
        if (mScreenNameFilters.size() > 0 && !mScreenNameFilters.get(nameHash, true)) {
            return false;
        }
        return true;
    }

    protected Map<String, String> filterEventAttributes(MPEvent event, SparseBooleanArray filter) {
        return filterEventAttributes(event.getEventType(), event.getEventName(), filter, event.getInfo());
    }

    protected Map<String, String> filterEventAttributes(MParticle.EventType eventType, String eventName, SparseBooleanArray filter, Map<String, String> eventAttributes) {
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
                int hash = MPUtility.mpHash(eventTypeStr + eventName + key);
                if (filter.get(hash, true)) {
                    newAttributes.put(key, entry.getValue());
                }
            }
            return newAttributes;
        } else {
            return eventAttributes;
        }
    }

    public abstract String getName();

    public abstract boolean isOriginator(String uri);

    protected abstract AbstractKit update();

    public JSONObject filterAttributes(SparseBooleanArray attributeFilters, JSONObject attributes) {
        if (attributes != null && attributeFilters != null && attributeFilters.size() > 0
                && attributes.length() > 0) {
            Iterator<String> attIterator = attributes.keys();
            JSONObject newAttributes = new JSONObject();
            while (attIterator.hasNext()) {
                String entry = attIterator.next();
                int hash = MPUtility.mpHash(entry);
                if (attributeFilters.get(hash, true)) {
                    try {
                        newAttributes.put(entry, attributes.getString(entry));
                    } catch (JSONException jse) {

                    }
                }
            }
            return newAttributes;
        } else {
            return attributes;
        }
    }

    void setLocation(Location location) {

    }

    void setUserAttributes(JSONObject mUserAttributes) {

    }

    void removeUserAttribute(String key) {

    }

    void setUserIdentity(String id, MParticle.IdentityType identityType) {

    }

    List<ReportingMessage> logout() {
        return null;
    }

    void removeUserIdentity(String id) {

    }

    List<ReportingMessage> handleIntent(Intent intent) {
        return null;
    }

    void startSession() {

    }

    void endSession() {

    }

    public boolean isRunning() {
        return mRunning;
    }

    public boolean shouldSetIdentity(MParticle.IdentityType identityType) {
        return mUserIdentityFilters == null || mUserIdentityFilters.size() == 0 || mUserIdentityFilters.get(identityType.getValue(), true);
    }

    public void setRunning(boolean running) {
        this.mRunning = running;
    }

    public List<Projection.ProjectionResult> projectEvents(MPEvent event) {
        return projectEvents(event, false);
    }

    public List<Projection.ProjectionResult> projectEvents(CommerceEvent event) {
        if (CommerceEventUtil.getEventType(event) == Constants.Commerce.EVENT_TYPE_IMPRESSION) {
            return null;
        }
        List<Projection.ProjectionResult> events = new LinkedList<Projection.ProjectionResult>();
        EventWrapper.CommerceEventWrapper wrapper = new EventWrapper.CommerceEventWrapper(event);
        for (int i = 0; i < projectionList.size(); i++) {
            Projection projection = projectionList.get(i);
            if (projection.isMatch(wrapper)) {
                List<Projection.ProjectionResult> results = projection.project(wrapper);
                if (results != null) {
                    events.addAll(events);
                }
            }
        }
        if (events.isEmpty()) {
            if (defaultCommerceProjection != null) {
                events.addAll(defaultCommerceProjection.project(wrapper));
            } else {
                return null;
            }
        }
        return events;
    }

    public List<Projection.ProjectionResult> projectEvents(MPEvent event, boolean isScreenEvent) {
        List<Projection.ProjectionResult> events = new LinkedList<Projection.ProjectionResult>();

        EventWrapper.MPEventWrapper wrapper = new EventWrapper.MPEventWrapper(event, isScreenEvent);
        for (int i = 0; i < projectionList.size(); i++) {
            Projection projection = projectionList.get(i);
            if (projection.isMatch(wrapper)) {
                List<Projection.ProjectionResult> newEvents = projection.project(wrapper);
                if (newEvents != null) {
                    events.addAll(newEvents);
                }
            }
        }

        if (events.isEmpty()) {
            if (isScreenEvent) {
                if (defaultScreenProjection != null) {
                    events.addAll(defaultScreenProjection.project(wrapper));
                } else {
                    return null;
                }
            } else {
                if (defaultProjection != null) {
                    events.addAll(defaultProjection.project(wrapper));
                } else {
                    return null;
                }
            }
        }

        return events;
    }


    public int getKitId() {
        return kitId;
    }

    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        return null;
    }

    public final AbstractKit setKitManager(KitManager kitManager) {
        this.context = kitManager.getContext();
        this.mEkManager = kitManager;
        return this;
    }

    public final AbstractKit setId(int id) {
        this.kitId = id;
        return this;
    }
}
