package com.mparticle.internal.messages;

import android.location.Location;
import androidx.annotation.Nullable;

import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.Constants;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MPCommerceMessage extends BaseMPMessage {

    protected MPCommerceMessage(Builder builder, InternalSession session, @Nullable Location location, long mpId) throws JSONException {
        super(builder, session, location, mpId);
    }

    public static class Builder extends BaseMPMessageBuilder {

        public Builder(CommerceEvent commerceEvent) {
            super(Constants.MessageType.COMMERCE_EVENT);
            addCommerceEventInfo(this, commerceEvent);
        }

        @Override
        public BaseMPMessageBuilder timestamp(long timestamp) {
            return super.timestamp(timestamp);
        }

        @Nullable
        public String getProductAction() {
            JSONObject productActionObj = optJSONObject(Constants.Commerce.PRODUCT_ACTION_OBJECT);
            if (productActionObj != null) {
                return productActionObj.optString(Constants.Commerce.PRODUCT_ACTION);
            }
            return null;
        }

        public BaseMPMessage build(InternalSession session, @Nullable Location location, long mpId) throws JSONException {
            return new MPCommerceMessage(this, session, location, mpId);
        }

        private static void addCommerceEventInfo(JSONObject message, CommerceEvent event) {
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
                if (event.getCustomAttributeStrings() != null) {
                    message.put(Constants.Commerce.ATTRIBUTES, MPUtility.mapToJson(event.getCustomAttributeStrings()));
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

    }
}