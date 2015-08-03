package com.mparticle.commerce;


import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class CommerceEvent {
    public static final String ADD_TO_CART = "add_to_cart";
    public static final String REMOVE_FROM_CART = "remove_from_cart";
    public static final String ADD_TO_WISHLIST = "add_to_wishlist";
    public static final String REMOVE_FROM_WISHLIST = "remove_from_wishlist";
    public static final String CHECKOUT = "checkout";
    public static final String CLICK = "click";
    public static final String DETAIL = "view_detail";
    public static final String PURCHASE = "purchase";
    public static final String REFUND = "refund";
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
        mImpressions = builder.mImpressions;
        mNonIteraction = builder.mNonIteraction;

        boolean devMode = MParticle.getInstance().getEnvironment().equals(MParticle.Environment.Development);

        if (MPUtility.isEmpty(mProductAction)
                && MPUtility.isEmpty(mPromotionAction)
                && (mImpressions == null || mImpressions.size() == 0)) {
            if (devMode) {
                throw new IllegalStateException("CommerceEvent must be created with either a product action. promotion action, or an impression");
            } else {
                ConfigManager.log(MParticle.LogLevel.ERROR, "CommerceEvent must be created with either a product action, promotion action, or an impression");
            }
        }

        if (mProductAction != null) {
            if (mProductAction.equalsIgnoreCase(CommerceEvent.PURCHASE) ||
                    mProductAction.equalsIgnoreCase(CommerceEvent.REFUND)) {
                if (mTransactionAttributes == null || MPUtility.isEmpty(mTransactionAttributes.getId())) {
                    String message = "CommerceEvent with action " + mProductAction + " must include a TransactionAttributes object with a transaction ID.";
                    if (devMode) {
                        throw new IllegalStateException(message);
                    } else {
                        ConfigManager.log(MParticle.LogLevel.ERROR, message);
                    }
                }
            }
            if (promotionList != null && promotionList.size() > 0) {
                if (devMode) {
                    throw new IllegalStateException("Product CommerceEvent should not contain Promotions.");
                } else {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Product CommerceEvent should not contain Promotions.");
                }
            }
        }else if (mPromotionAction != null ) {
            if (productList != null && productList.size() > 0) {
                if (devMode) {
                    throw new IllegalStateException("Promotion CommerceEvent should not contain Products.");
                }else {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Promotion CommerceEvent should not contain Products.");
                }
            }

        }else {
            if (productList != null && productList.size() > 0) {
                if (devMode) {
                    throw new IllegalStateException("Impression CommerceEvent should not contain Products.");
                }else {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Impression CommerceEvent should not contain Products.");
                }
            }
            if (promotionList != null && promotionList.size() > 0) {
                if (devMode) {
                    throw new IllegalStateException("Impression CommerceEvent should not contain Promotions.");
                } else {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Impression CommerceEvent should not contain Promotions.");
                }
            }
        }


        if (mTransactionAttributes == null || mTransactionAttributes.getRevenue() == null) {
            double transactionRevenue = 0;
            if (mTransactionAttributes == null) {
                mTransactionAttributes = new TransactionAttributes();
            } else {
                Double shipping = mTransactionAttributes.getShipping();
                Double tax = mTransactionAttributes.getTax();
                transactionRevenue += (shipping != null ? shipping : 0);
                transactionRevenue += (tax != null ? tax : 0);
            }
            if (productList != null) {
                for (Product product : productList) {
                    if (product != null) {
                        double price = product.getUnitPrice();
                        price *= product.getQuantity();
                        transactionRevenue += price;
                    }
                }
            }
            mTransactionAttributes.setRevenue(transactionRevenue);
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
                    if (impression.mListName != null) {
                        impressionJson.put("pil", impression.mListName);
                    }
                    if (impression.mProducts != null && impression.mProducts.size() > 0) {
                        JSONArray productsJson = new JSONArray();
                        impressionJson.put("pl", productsJson);
                        for (Product product : impression.mProducts) {
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
        private List<Impression> mImpressions;

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
                    if (product != null) {
                        productList.add(product);
                    }
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
                for (Promotion promotion : promotions) {
                    if (promotion != null) {
                        promotionList.add(promotion);
                    }
                }
            }
        }

        public Builder(Impression impression) {
            addImpression(impression);
            mPromotionAction = null;
            mProductAction = null;
        }

        public Builder(CommerceEvent event) {
            mProductAction = event.getProductAction();
            mPromotionAction = event.getPromotionAction();
            if (event.getCustomAttributes() != null) {
                Map<String, String> shallowCopy = new HashMap<String, String>();
                shallowCopy.putAll(event.getCustomAttributes());
                customAttributes = shallowCopy;
            }
            if (event.getPromotions() != null) {
                for (Promotion promotion : event.getPromotions()) {
                    addPromotion(new Promotion(promotion));
                }
            }
            if (event.getProducts() != null) {
                for (Product product : event.getProducts()) {
                    addProduct(new Product.Builder(product).build());
                }
            }
            mCheckoutStep = event.getCheckoutStep();
            mCheckoutOptions = event.getCheckoutOptions();
            mProductListName = event.getProductListName();
            mProductListSource = event.getProductListSource();
            mCurrency = event.getCurrency();
            if (event.getTransactionAttributes() != null) {
                mTransactionAttributes = new TransactionAttributes(event.getTransactionAttributes());
            }
            mScreen = event.mScreen;
            mNonIteraction = event.mNonIteraction;
            if (event.getImpressions() != null) {
                for (Impression impression : event.getImpressions()) {
                    addImpression(new Impression(impression));
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

        public Builder checkoutStep(Integer step) {
            if (step == null || step >= 0) {
                mCheckoutStep = step;
            }
            return this;
        }

        public Builder addImpression(Impression impression) {
            if (impression != null) {
                if (mImpressions == null) {
                    mImpressions = new LinkedList<Impression>();
                }
                mImpressions.add(impression);
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

        public Builder products(List<Product> products) {
            productList = products;
            return this;
        }

        public Builder impressions(List<Impression> impressions) {
            mImpressions = impressions;
            return this;
        }

        public Builder promotions(List<Promotion> promotions) {
            promotionList = promotions;
            return this;
        }
    }

    public static class Impression {
        private String mListName = null;
        private List<Product> mProducts;

        public Impression(String listName, Product product) {
            super();
            mListName = listName;
            addProduct(product);
        }

        public String getListName() {
            return mListName;
        }

        public List<Product> getProducts() {
            return mProducts;
        }

        public Impression addProduct(Product product) {
            if (mProducts == null) {
                mProducts = new LinkedList<Product>();
            }
            if (product != null) {
                mProducts.add(product);
            }
            return this;
        }

        public Impression(Impression impression) {
            mListName = impression.mListName;
            if (impression.mProducts != null) {
                mProducts = impression.mProducts;
            }
        }
    }
}
