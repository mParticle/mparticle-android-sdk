package com.mparticle.internal;

import android.location.Location;

import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class MPMessage extends JSONObject{
    private long mpId;

    private MPMessage(){}

    private MPMessage(Builder builder) throws JSONException{
        mpId = builder.mMPId;
        put(Constants.MessageKey.TYPE, builder.mMessageType);
        put(Constants.MessageKey.TIMESTAMP, builder.mTimestamp);
        if (Constants.MessageType.SESSION_START == builder.mMessageType) {
            put(Constants.MessageKey.ID, builder.mSession.mSessionID);
        } else {
            put(Constants.MessageKey.SESSION_ID, builder.mSession.mSessionID);

            if (builder.mSession.mSessionStartTime > 0) {
                put(Constants.MessageKey.SESSION_START_TIMESTAMP, builder.mSession.mSessionStartTime);
            }
        }

        if (builder.mName != null) {
            put(Constants.MessageKey.NAME, builder.mName);
        }

        if (builder.mCustomFlags != null) {
            JSONObject flagsObject = new JSONObject();
            for (Map.Entry<String, List<String>> entry : builder.mCustomFlags.entrySet()) {
                List<String> values = entry.getValue();
                JSONArray valueArray = new JSONArray(values);
                flagsObject.put(entry.getKey(), valueArray);
            }
            put(Constants.MessageKey.EVENT_FLAGS, flagsObject);
        }

        if (builder.mLength != null){
            put(Constants.MessageKey.EVENT_DURATION, builder.mLength);
            if (builder.mAttributes == null){
                builder.mAttributes = new JSONObject();
            }
            if (!builder.mAttributes.has("EventLength")) {
                //can't be longer than max int milliseconds
                builder.mAttributes.put("EventLength", Integer.toString(builder.mLength.intValue()));
            }
        }

        if (builder.mAttributes != null) {
            put(Constants.MessageKey.ATTRIBUTES, builder.mAttributes);
        }

        if (builder.mDataConnection != null) {
            put(Constants.MessageKey.STATE_INFO_DATA_CONNECTION, builder.mDataConnection);
        }

        if (!(Constants.MessageType.ERROR.equals(builder.mMessageType) &&
                !(Constants.MessageType.OPT_OUT.equals(builder.mMessageType)))) {
            if (builder.mLocation != null) {
                JSONObject locJSON = new JSONObject();
                locJSON.put(Constants.MessageKey.LATITUDE, builder.mLocation .getLatitude());
                locJSON.put(Constants.MessageKey.LONGITUDE, builder.mLocation .getLongitude());
                locJSON.put(Constants.MessageKey.ACCURACY, builder.mLocation .getAccuracy());
                put(Constants.MessageKey.LOCATION, locJSON);
            }
        }
        if (builder.commerceEvent != null){
            addCommerceEventInfo(this, builder.commerceEvent);
        }
    }

    private static void addCommerceEventInfo(MPMessage message, CommerceEvent event) {
        try {

            if (event.getScreen() != null) {
                message.put(Constants.Commerce.SCREEN_NAME, event.getScreen());
            }
            if (event.getNonInteraction() != null) {
                message.put(Constants.Commerce.NON_INTERACTION, event.getNonInteraction().booleanValue());
            }
            if (event.getCurrency() != null) {
                message.put(Constants.Commerce.CURRENCY, event.getCurrency());
            }
            if (event.getCustomAttributes() != null) {
                JSONObject attrs = new JSONObject();
                for (Map.Entry<String, String> entry : event.getCustomAttributes().entrySet()) {
                    attrs.put(entry.getKey(), entry.getValue());
                }
                message.put(Constants.Commerce.ATTRIBUTES, attrs);
            }
            if (event.getProductAction() != null) {
                JSONObject productAction = new JSONObject();
                message.put(Constants.Commerce.PRODUCT_ACTION_OBJECT, productAction);
                productAction.put(Constants.Commerce.PRODUCT_ACTION, event.getProductAction());
                if (event.getCheckoutStep() != null) {
                    productAction.put(Constants.Commerce.CHECKOUT_STEP, event.getCheckoutStep());
                }
                if (event.getCheckoutOptions() != null) {
                    productAction.put(Constants.Commerce.CHECKOUT_OPTIONS, event.getCheckoutOptions());
                }
                if (event.getProductListName() != null) {
                    productAction.put(Constants.Commerce.PRODUCT_LIST_NAME, event.getProductListName());
                }
                if (event.getProductListSource() != null) {
                    productAction.put(Constants.Commerce.PRODUCT_LIST_SOURCE, event.getProductListSource());
                }
                if (event.getTransactionAttributes() != null) {
                    TransactionAttributes transactionAttributes = event.getTransactionAttributes();
                    if (transactionAttributes.getId() != null) {
                        productAction.put(Constants.Commerce.TRANSACTION_ID, transactionAttributes.getId());
                    }
                    if (transactionAttributes.getAffiliation() != null) {
                        productAction.put(Constants.Commerce.TRANSACTION_AFFILIATION, transactionAttributes.getAffiliation());
                    }
                    if (transactionAttributes.getRevenue() != null) {
                        productAction.put(Constants.Commerce.TRANSACTION_REVENUE, transactionAttributes.getRevenue());
                    }
                    if (transactionAttributes.getTax() != null) {
                        productAction.put(Constants.Commerce.TRANSACTION_TAX, transactionAttributes.getTax());
                    }
                    if (transactionAttributes.getShipping() != null) {
                        productAction.put(Constants.Commerce.TRANSACTION_SHIPPING, transactionAttributes.getShipping());
                    }
                    if (transactionAttributes.getCouponCode() != null) {
                        productAction.put(Constants.Commerce.TRANSACTION_COUPON_CODE, transactionAttributes.getCouponCode());
                    }
                }
                if (event.getProducts() != null && event.getProducts().size() > 0) {
                    JSONArray products = new JSONArray();
                    for (int i = 0; i < event.getProducts().size(); i++) {
                        products.put(new JSONObject(event.getProducts().get(i).toString()));
                    }
                    productAction.put(Constants.Commerce.PRODUCT_LIST, products);
                }
                if (event.getProductAction().equals(Product.PURCHASE)) {
                    MParticle.getInstance().Commerce().cart().clear();
                }
                JSONObject cartJsonObject = new JSONObject(MParticle.getInstance().Commerce().cart().toString());
                if (cartJsonObject.length() > 0) {
                    message.put("sc", cartJsonObject);
                }

            }
            if (event.getPromotionAction() != null) {
                JSONObject promotionAction = new JSONObject();
                message.put(Constants.Commerce.PROMOTION_ACTION_OBJECT, promotionAction);
                promotionAction.put(Constants.Commerce.PROMOTION_ACTION, event.getPromotionAction());
                if (event.getPromotions() != null && event.getPromotions().size() > 0) {
                    JSONArray promotions = new JSONArray();
                    for (int i = 0; i < event.getPromotions().size(); i++) {
                        promotions.put(getPromotionJson(event.getPromotions().get(i)));
                    }
                    promotionAction.put(Constants.Commerce.PROMOTION_LIST, promotions);
                }
            }
            if (event.getImpressions() != null && event.getImpressions().size() > 0) {
                JSONArray impressions = new JSONArray();
                for (Impression impression : event.getImpressions()) {
                    JSONObject impressionJson = new JSONObject();
                    if (impression.getListName() != null) {
                        impressionJson.put(Constants.Commerce.IMPRESSION_LOCATION, impression.getListName());
                    }
                    if (impression.getProducts() != null && impression.getProducts().size() > 0) {
                        JSONArray productsJson = new JSONArray();
                        impressionJson.put(Constants.Commerce.IMPRESSION_PRODUCT_LIST, productsJson);
                        for (Product product : impression.getProducts()) {
                            productsJson.put(new JSONObject(product.toString()));
                        }
                    }
                    if (impressionJson.length() > 0) {
                        impressions.put(impressionJson);
                    }
                }
                if (impressions.length() > 0) {
                    message.put(Constants.Commerce.IMPRESSION_OBJECT, impressions);
                }
            }



        } catch (Exception jse) {

        }
    }

    private static JSONObject getPromotionJson(Promotion promotion) {
        JSONObject json = new JSONObject();
        try {
            if (!MPUtility.isEmpty(promotion.getId())) {
                json.put("id", promotion.getId());
            }
            if (!MPUtility.isEmpty(promotion.getName())) {
                json.put("nm", promotion.getName());
            }
            if (!MPUtility.isEmpty(promotion.getCreative())) {
                json.put("cr", promotion.getCreative());
            }
            if (!MPUtility.isEmpty(promotion.getPosition())) {
                json.put("ps", promotion.getPosition());
            }
        } catch (JSONException jse) {

        }
        return json;
    }

    public JSONObject getAttributes(){
        return optJSONObject(Constants.MessageKey.ATTRIBUTES);
    }

    public long getTimestamp(){
        try {
            return getLong(Constants.MessageKey.TIMESTAMP);
        }catch (JSONException jse){

        }
        return 0;
    }

    public String getSessionId() {
        if (Constants.MessageType.SESSION_START.equals(getMessageType())) {
            return optString(Constants.MessageKey.ID, Constants.NO_SESSION_ID);
        } else {
            return optString(Constants.MessageKey.SESSION_ID, Constants.NO_SESSION_ID);
        }
    }

    public String getMessageType() {
        return optString(Constants.MessageKey.TYPE);
    }

    public int getTypeNameHash() {
        return MPUtility.mpHash(getType() + getName());
    }

    public String getType() {
        return optString(Constants.MessageKey.TYPE);
    }

    public String getName() {
        return optString(Constants.MessageKey.NAME);
    }

    public long getMpId() {
        return mpId;
    }

    public static class Builder {
        private final String mMessageType;
        private final Session mSession;
        private CommerceEvent commerceEvent = null;
        private long mTimestamp;
        private String mName;
        private JSONObject mAttributes;
        private Location mLocation;
        private String mDataConnection;
        private Double mLength = null;
        private Map<String, List<String>> mCustomFlags;
        private long mMPId;

        public Builder(String messageType, Session session, Location location, long mpId){
            mMessageType = messageType;
            mSession = new Session(session);
            mLocation = location;
            mMPId = mpId;
        }

        public Builder(CommerceEvent event, Session session, Location location, long mpId) {
            this(Constants.MessageType.COMMERCE_EVENT, session, location, mpId);
            commerceEvent = event;
        }

        public Builder timestamp(long timestamp){
            mTimestamp = timestamp;
            return this;
        }
        public Builder name(String name){
            mName = name;
            return this;
        }
        public Builder attributes(JSONObject attributes){
            mAttributes = attributes;
            return this;
        }

        public MPMessage build() throws JSONException {
            return new MPMessage(this);
        }

        public Builder dataConnection(String dataConnection) {
            mDataConnection = dataConnection;
            return this;
        }

        public Builder length(Double length) {
            mLength = length;
            return this;
        }

        public Builder flags(Map<String, List<String>> customFlags) {
            mCustomFlags = customFlags;
            return this;
        }
    }
}
