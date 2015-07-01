package com.mparticle.commerce;


import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class CommerceEvent {
    public static final String ADD = "add";
    public static final String CHECKOUT = "checkout";
    public static final String CLICK = "click";
    public static final String DETAIL = "detail";
    public static final String PURCHASE = "purchase";
    public static final String REFUND = "refund";
    public static final String REMOVE = "remove";
    public static final String CHECKOUT_OPTION = "checkout_option";
    private List<Impression> mImpressions;
    private String mProductAction;
    private String mPromotionAction;
    private Map<String, String> customAttributes;
    private List<Promotion> promotionList;
    private List<Product> productList;
    private Integer mCheckoutStep;
    private String mCheckoutOptions;
    private String mProductListName;
    private String mProductListSource;
    private String mCurrency;
    private TransactionAttributes mTransactionAttributes;
    private String mScreen;
    private Boolean mNonIteraction;

    private CommerceEvent(Builder builder) {
        super();
        mProductAction = builder.mProductAction;
        mPromotionAction = builder.mPromotionAction;
        customAttributes = builder.customAttributes;
        promotionList = builder.promotionList;
        productList = builder.productList;
        mCheckoutStep = builder.mCheckoutStep;
        mCheckoutOptions = builder.mCheckoutOptions;
        mProductListName = builder.mProductListName;
        mProductListSource = builder.mProductListSource;
        mCurrency = builder.mCurrency;
        mTransactionAttributes = builder.mTransactionAttributes;
        mScreen = builder.mScreen;
        mImpressions = builder.impressions;
        mNonIteraction = builder.mNonIteraction;

        boolean devMode = MParticle.getInstance().getEnvironment().equals(MParticle.Environment.Development);

        if (MPUtility.isEmpty(mProductAction) && MPUtility.isEmpty(mPromotionAction)){
            if (devMode){
                throw new IllegalStateException("CommerceEvent must be created with either a product action or promotion action");
            }else{
                ConfigManager.log(MParticle.LogLevel.ERROR, "CommerceEvent must be created with either a product action or promotion action");
            }
        }

        if (mProductAction != null){
            if (mProductAction.equalsIgnoreCase(CommerceEvent.PURCHASE) ||
                    mProductAction.equalsIgnoreCase(CommerceEvent.REFUND)){
                if (mTransactionAttributes == null || MPUtility.isEmpty(mTransactionAttributes.getId())) {
                    String message = "CommerceEvent with action " + mPromotionAction + " must include a TransactionAttributes object with a transaction ID.";
                    if (devMode) {
                        throw new IllegalStateException(message);
                    } else {
                        ConfigManager.log(MParticle.LogLevel.ERROR, message);
                    }
                }
            }
        }
    }

    private CommerceEvent() {

    }

    @Override
    public String toString() {
        try {
            JSONObject eventJson = new JSONObject();
            if (mScreen != null) {
                eventJson.put("sn", mScreen);
            }
            if (mNonIteraction != null) {
                eventJson.put("ni", mNonIteraction.booleanValue());
            }
            if (mProductAction != null) {
                JSONObject productAction = new JSONObject();
                eventJson.put("pd", productAction);
                productAction.put("an", mProductAction);
                if (mCheckoutStep != null) {
                    productAction.put("cs", mCheckoutStep);
                }
                if (mCheckoutOptions != null) {
                    productAction.put("co", mCheckoutOptions);
                }
                if (mProductListName != null) {
                    productAction.put("pal", mProductListName);
                }
                if (mProductListSource != null) {
                    productAction.put("pls", mProductListSource);
                }
                if (mTransactionAttributes != null) {
                    if (mTransactionAttributes.getId() != null) {
                        productAction.put("ti", mTransactionAttributes.getId());
                    }
                    if (mTransactionAttributes.getAffiliation() != null) {
                        productAction.put("ta", mTransactionAttributes.getAffiliation());
                    }
                    if (mTransactionAttributes.getRevenue() != null) {
                        productAction.put("tr", mTransactionAttributes.getRevenue());
                    }
                    if (mTransactionAttributes.getTax() != null) {
                        productAction.put("tt", mTransactionAttributes.getTax());
                    }
                    if (mTransactionAttributes.getShipping() != null) {
                        productAction.put("ts", mTransactionAttributes.getShipping());
                    }
                    if (mTransactionAttributes.getCouponCode() != null) {
                        productAction.put("tcc", mTransactionAttributes.getCouponCode());
                    }
                }
                if (productList != null && productList.size() > 0) {
                    JSONArray products = new JSONArray();
                    for (int i = 0; i < productList.size(); i++) {
                        products.put(new JSONObject(productList.get(i).toString()));
                    }
                    productAction.put("pl", products);
                }


            } else {
                JSONObject promotionAction = new JSONObject();
                eventJson.put("pm", promotionAction);
                promotionAction.put("an", mPromotionAction);
                if (promotionList != null && promotionList.size() > 0) {
                    JSONArray promotions = new JSONArray();
                    for (int i = 0; i < promotionList.size(); i++) {
                        promotions.put(new JSONObject(promotionList.get(i).toString()));
                    }
                    promotionAction.put("pl", promotions);
                }
            }
            if (mImpressions != null && mImpressions.size() > 0) {
                JSONArray impressions = new JSONArray();
                for (Impression impression : mImpressions) {
                    JSONObject impressionJson = new JSONObject();
                    if (impression.where != null) {
                        impressionJson.put("pil", impression.where);
                    }
                    if (impression.products != null && impression.products.length > 0) {
                        JSONArray productsJson = new JSONArray();
                        impressionJson.put("pl", productsJson);
                        for (Product product : impression.products) {
                            productsJson.put(new JSONObject(product.toString()));
                        }
                    }
                    if (impressionJson.length() > 0) {
                        impressions.put(impressionJson);
                    }
                }
                if (impressions.length() > 0) {
                    eventJson.put("pi", impressions);
                }
            }
            return eventJson.toString();

        } catch (JSONException jse) {

        }
        return super.toString();
    }

    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }

    public String getScreen() {
        return mScreen;
    }

    public Boolean getNonInteraction() {
        return mNonIteraction;
    }

    public String getProductAction() {
        return mProductAction;
    }

    public Integer getCheckoutStep() {
        return mCheckoutStep;
    }

    public String getCheckoutOptions() {
        return mCheckoutOptions;
    }

    public String getProductListName() {
        return mProductListName;
    }

    public String getProductListSource() {
        return mProductListSource;
    }

    public TransactionAttributes getTransactionAttributes() {
        return mTransactionAttributes;
    }

    public List<Product> getProducts() {
        return productList;
    }

    public String getPromotionAction() {
        return mPromotionAction;
    }

    public List<Promotion> getPromotions() {
        return promotionList;
    }

    public List<Impression> getImpressions() {
        return mImpressions;
    }

    public String getCurrency() {
        return mCurrency;
    }


    public static class Builder {

        private final String mProductAction;
        private final String mPromotionAction;
        private Map<String, String> customAttributes = null;
        private List<Promotion> promotionList = null;
        private List<Product> productList = null;
        private Integer mCheckoutStep = null;
        private String mCheckoutOptions = null;
        private String mProductListName = null;
        private String mProductListSource = null;
        private String mCurrency = null;
        private TransactionAttributes mTransactionAttributes = null;
        private String mScreen = null;
        private Boolean mNonIteraction;
        private List<Impression> impressions;

        private Builder() {
            mProductAction = mPromotionAction = null;
        }

        public Builder(String productAction, Product... products) {
            mProductAction = productAction;
            mPromotionAction = null;
            if (products != null && products.length > 0) {
                if (productList == null) {
                    productList = new LinkedList<Product>();
                }
                for (Product product : products) {
                    productList.add(product);
                }
            }
        }

        public Builder(String promotionAction, Promotion... promotions) {
            mProductAction = null;
            mPromotionAction = promotionAction;
            if (promotions != null && promotions.length > 0) {
                if (promotionList == null) {
                    promotionList = new LinkedList<Promotion>();
                }
                for (Promotion promotion : promotionList) {
                    promotionList.add(promotion);
                }
            }
        }

        public Builder screen(String screenName) {
            mScreen = screenName;
            return this;
        }

        public Builder addProduct(Product product) {
            if (productList == null) {
                productList = new LinkedList<Product>();
            }
            productList.add(product);
            return this;
        }

        public Builder transactionAttributes(TransactionAttributes attributes) {
            mTransactionAttributes = attributes;
            return this;
        }

        public Builder currency(String currency) {
            mCurrency = currency;
            return this;
        }

        public Builder nonInteraction(boolean userTriggered) {
            mNonIteraction = userTriggered;
            return this;
        }

        public Builder customAttributes(Map<String, String> attributes) {
            customAttributes = attributes;
            return this;
        }

        public Builder addPromotion(Promotion promotion) {
            if (promotionList == null) {
                promotionList = new LinkedList<Promotion>();
            }
            promotionList.add(promotion);
            return this;
        }

        public Builder checkoutStep(int step) {
            if (step >= 0) {
                mCheckoutStep = step;
            }
            return this;
        }

        public Builder addImpression(String where, Product... products) {
            Impression impression = new Impression();
            impression.where = where;
            impression.products = products;
            if (impressions == null) {
                impressions = new LinkedList<Impression>();
            }
            impressions.add(impression);
            return this;
        }

        public Builder addImpression(Impression impression) {
            if (impression != null) {
                addImpression(impression.where, impression.products);
            }
            return this;
        }

        public Builder checkoutOptions(String options) {
            mCheckoutOptions = options;
            return this;
        }

        public CommerceEvent build() {
            return new CommerceEvent(this);
        }

        public Builder productListName(String listName) {
            mProductListName = listName;
            return this;
        }

        public Builder productListSource(String listSource) {
            mProductListSource = listSource;
            return this;
        }
    }

    public static class Impression {
        public String where = null;
        public Product[] products;
    }
}
