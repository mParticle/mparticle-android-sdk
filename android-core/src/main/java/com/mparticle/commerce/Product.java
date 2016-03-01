package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A Product represents any good or service that a user may purchase, view, or otherwise interact with in your app.
 *
 * The Product class is built to be immutable - use the {@link com.mparticle.commerce.Product.Builder} class to create a Product.
 *
 * Product objects are designed to be used with {@link CommerceEvent} and the {@link Cart}.
 *
 */
public final class Product {

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
    private static EqualityComparator mComparator = null;
    private Map<String, String> mCustomAttributes;
    private String mName = null;
    private String mCategory;
    private String mCouponCode;
    private String mSku;
    private long mTimeAdded;
    private Integer mPosition;
    private double mPrice;
    private double mQuantity;
    private String mBrand;
    private String mVariant;

    /**
     *
     *
     * @return Retrieve the Map of custom attributes set on this Product
     *
     *
     * @see com.mparticle.commerce.Product.Builder#customAttributes(Map)
     */
    public Map<String, String> getCustomAttributes() {
        return mCustomAttributes;
    }

    public double getTotalAmount() {
        return getUnitPrice() * getQuantity();
    }

    /**
     * A simple interface that you can implement in order to customize Product equality comparisons
     *
     * @see #setEqualityComparator(EqualityComparator)
     * @see Cart#remove(Product)
     */
    public interface EqualityComparator {
        boolean equals(Product product1, Product product2);
    }

    /**
     * Optionally customize the EqualityComparator
     *
     * @param comparator
     */
    public static void setEqualityComparator(EqualityComparator comparator) {
        mComparator = comparator;
    }

    private Product() {
    }

    private Product(Builder builder) {
        mName = builder.mName;
        mCategory = builder.mCategory;
        mCouponCode = builder.mCouponCode;
        mSku = builder.mSku;
        mPosition = builder.mPosition;
        mPrice = builder.mPrice;
        mQuantity = builder.mQuantity;
        mBrand = builder.mBrand;
        mVariant = builder.mVariant;
        mCustomAttributes = builder.mCustomAttributes;
        updateTimeAdded();

        boolean devMode = MParticle.getInstance().getEnvironment().equals(MParticle.Environment.Development);

        if (MPUtility.isEmpty(mName)) {
            String message = "Product name is required.";
            if (devMode) {
                throw new IllegalArgumentException(message);
            } else {
                mName = "Unknown";
                ConfigManager.log(MParticle.LogLevel.ERROR, message);
            }
        } else if (MPUtility.isEmpty(mSku)) {
            String message = "Product sku is required.";
            if (devMode) {
                throw new IllegalArgumentException(message);
            } else {
                mSku = "Unknown";
                ConfigManager.log(MParticle.LogLevel.ERROR, message);
            }
        }
    }

    void updateTimeAdded() {
        mTimeAdded = System.currentTimeMillis();
    }

    /**
     *
     * @return the name description of the Product
     */
    public String getName() {
        return mName;
    }

    /**
     *
     * @return the category description of the Product
     */
    public String getCategory() {
        return mCategory;
    }

    /**
     *
     *
     * @return the coupon code associated with the Product
     */
    public String getCouponCode() {
        return mCouponCode;
    }

    /**
     *
     * @return the SKU/ID associated with the Product
     */
    public String getSku() {
        return mSku;
    }

    /**
     *
     * @return the position of the product on the page/product impression list
     */
    public Integer getPosition() {
        return mPosition;
    }

    /**
     *
     * @return the unit price of a single Product
     */
    public double getUnitPrice() {
        return mPrice;
    }

    /**
     *
     * @return the quantity of Products
     */
    public double getQuantity() {
        if (mQuantity < 1) {
            return 1;
        }
        return mQuantity;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (!(object instanceof Product)) {
            return false;
        }
        Product product = (Product) object;
        if (mComparator == null) {
            return false;
        } else {
            return mComparator.equals(this, product);
        }
    }

    /**
     *
     * @return the brand of the Product
     */
    public String getBrand() {
        return mBrand;
    }

    /**
     *
     *
     * @return the variant or version of the Product
     */
    public String getVariant() {
        return mVariant;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static Product fromString(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return fromJson(jsonObject);
        } catch (JSONException jse) {

        }
        return null;
    }

    static Product fromJson(JSONObject jsonObject) {
        try {
            Product.Builder builder = new Product.Builder(jsonObject.getString("nm"), jsonObject.optString("id", null), jsonObject.optDouble("pr", 0));
            builder.category(jsonObject.optString("ca", null));
            builder.couponCode(jsonObject.optString("cc", null));
            if (jsonObject.has("ps")) {
                builder.position(jsonObject.optInt("ps", 0));
            }
            if (jsonObject.has("qt")) {
                builder.quantity(jsonObject.optDouble("qt", 1));
            }
            builder.brand(jsonObject.optString("br", null));
            builder.variant(jsonObject.optString("va", null));
            if (jsonObject.has("attrs")) {
                JSONObject attributesJson = jsonObject.getJSONObject("attrs");
                if (attributesJson.length() > 0) {
                    Map<String, String> customAttributes = new HashMap<String, String>();
                    Iterator<String> keys = attributesJson.keys();

                    while (keys.hasNext()) {
                        String key = keys.next();
                        customAttributes.put(key, attributesJson.getString(key));
                    }
                    builder.customAttributes(customAttributes);
                }
            }
            Product product = builder.build();
            product.mTimeAdded = jsonObject.optLong("act", 0);
            return product;
        } catch (JSONException jse) {

        }
        return null;
    }

    JSONObject toJson() {
        try {
            JSONObject productJson = new JSONObject();
            if (mName != null) {
                productJson.put("nm", mName);
            }
            if (mCategory != null) {
                productJson.put("ca", mCategory);
            }
            if (mCouponCode != null) {
                productJson.put("cc", mCouponCode);
            }
            if (mSku != null) {
                productJson.put("id", mSku);
            }
            if (mPosition != null) {
                productJson.put("ps", mPosition);
            }

            productJson.put("pr", mPrice);

            productJson.put("qt", mQuantity);

            productJson.put("act", mTimeAdded);
            productJson.put("tpa", getTotalAmount());

            if (mBrand != null) {
                productJson.put("br", mBrand);
            }
            if (mVariant != null) {
                productJson.put("va", mVariant);
            }
            if (mCustomAttributes != null && mCustomAttributes.size() > 0) {
                JSONObject attributes = new JSONObject();
                for (Map.Entry<String, String> entry : mCustomAttributes.entrySet()) {
                    attributes.put(entry.getKey(), entry.getValue());
                }
                productJson.put("attrs", attributes);
            }
            return productJson;
        } catch (JSONException jse) {

        }
        return new JSONObject();
    }

    void setQuantity(double quantity) {
        mQuantity = quantity;
    }

    /**
     * This class is designed to construct a Product object using the Builder pattern.
     *
     */
    public static class Builder {
        private String mName = null;
        private String mCategory;
        private String mCouponCode;
        private String mSku;

        private Integer mPosition;
        private double mPrice;
        private double mQuantity = 1;
        private String mBrand;
        private String mVariant;
        private Map<String, String> mCustomAttributes = null;

        private Builder() {
        }

        /**
         * Create a Product.Builder. The parameters of this method are all
         * required for a valid product
         *
         * @param name a description/name for the Product
         * @param sku a SKU or ID that unique identifies this Product
         * @param unitPrice the cost of a single Product
         */
        public Builder(String name, String sku, double unitPrice) {
            mName = name;
            mSku = sku;
            mPrice = unitPrice;
        }

        /**
         * Create a Product.Builder from an existing Product. This may
         * be useful if you need to alter or duplicate an existing Product.
         *
         * @param product an existing Product object
         */
        public Builder(Product product) {
            this(product.getName(), product.getSku(), product.getUnitPrice());
            mCategory = product.mCategory;
            mCouponCode = product.mCouponCode;
            mPosition = product.mPosition;
            mPrice = product.mPrice;
            mQuantity = product.mQuantity;
            mBrand = product.mBrand;
            mVariant = product.mVariant;
            if (product.getCustomAttributes() != null) {
                Map<String, String> shallowCopy = new HashMap<String, String>();
                shallowCopy.putAll(product.getCustomAttributes());
                mCustomAttributes = shallowCopy;
            }
        }

        /**
         * Set a custom Map of attributes on an object. Use this only
         * if there is an attribute of a Product that does not correlate to
         * any of the default attributes.
         *
         * @param attributes a Map of custom keys and values
         * @return returns this Builder object for use in method chaining
         */
        public Builder customAttributes(Map<String, String> attributes) {
            mCustomAttributes = attributes;
            return this;
        }

        /**
         * Sets the category associate with the Product
         *
         * @param category the Product's category
         * @return returns this Builder object for use in method chaining
         */
        public Builder category(String category) {
            mCategory = category;
            return this;
        }

        /**
         *
         * Sets the coupon code to associate with this Product
         *
         * @param couponCode the Product's coupon code
         * @return returns this Builder object for use in method chaining
         */
        public Builder couponCode(String couponCode) {
            mCouponCode = couponCode;
            return this;
        }

        /**
         * Change unique SKU or ID to associate with this Product
         *
         * @param sku the Product's SKU or ID
         * @return returns this Builder object for use in method chaining
         */
        public Builder sku(String sku) {
            mSku = sku;
            return this;
        }

        /**
         * Change the name/description to associate with this Product
         *
         * @param name the Product's name
         * @return returns this Builder object for use in method chaining
         */
        public Builder name(String name) {
            mName = name;
            return this;
        }

        /**
         * Sets the position of the product on the page or product impression list
         *
         * @param position the Products position
         * @return returns this Builder object for use in method chaining
         */
        public Builder position(Integer position) {
            mPosition = position;
            return this;
        }

        /**
         * Sets the unit price to associate with this Product
         *
         * @param price the Product's price
         * @return returns this Builder object for use in method chaining
         */
        public Builder unitPrice(double price) {
            mPrice = price;
            return this;
        }

        /**
         * Sets the quantity to associate with this Product.
         *
         * This field with default to 1
         *
         * @param quantity the Product's quantity
         * @return returns this Builder object for use in method chaining
         */
        public Builder quantity(double quantity) {
            mQuantity = quantity;
            return this;
        }

        /**
         * Sets the brand to associate with this Product
         *
         * @param brand the Product's brand
         * @return returns this Builder object for use in method chaining
         */
        public Builder brand(String brand) {
            mBrand = brand;
            return this;
        }

        /**
         * Sets the variant to associate with this Product
         *
         * @param variant the Product's variant
         * @return returns this Builder object for use in method chaining
         */
        public Builder variant(String variant) {
            mVariant = variant;
            return this;
        }

        /**
         * Build the Product object
         *
         * @return a Product object to be added to a {@link CommerceEvent} or to the {@link Cart}
         */
        public Product build() {
            return new Product(this);
        }
    }
}
