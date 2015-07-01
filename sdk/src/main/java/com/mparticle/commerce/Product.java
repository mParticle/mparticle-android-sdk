package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Object;import java.lang.String;import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public final class Product extends HashMap<String, Object> {

    private static EqualityComparator mComparator = null;
    private Map<String, String> mCustomAttributes;
    private String mName = null;
    private String mCategory;
    private String mCouponCode;
    private String mSku;

    private Integer mPosition;
    private Double mPrice;
    private double mQuantity;
    private String mBrand;
    private String mVariant;

    public interface EqualityComparator {
        boolean equals(Product product1, Product product2);
    }

    public static void setEqualityComparator(EqualityComparator comparator){
        mComparator = comparator;
    }

    private Product(){}

    public Product(Builder builder) {
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

        boolean devMode = MParticle.getInstance().getEnvironment().equals(MParticle.Environment.Development);

        if (MPUtility.isEmpty(mName)){
            String message = "Product name is required.";
            if (devMode){
                throw new IllegalArgumentException(message);
            }else {
                mName = "Unknown";
                ConfigManager.log(MParticle.LogLevel.ERROR, message);
            }
        }else if (MPUtility.isEmpty(mSku)){
            String message = "Product sku is required.";
            if (devMode){
                throw new IllegalArgumentException(message);
            }else{
                mSku = "Unknown";
                ConfigManager.log(MParticle.LogLevel.ERROR, message);
            }
        }
    }

    public String getName() {
        return mName;
    }

    public String getCategory() {
        return mCategory;
    }

    public String getCouponCode() {
        return mCouponCode;
    }

    public String getSku() {
        return mSku;
    }

    public Integer getPosition() {
        return mPosition;
    }

    public Double getPrice() {
        return mPrice;
    }

    public double getQuantity() {
        return mQuantity;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null){
            return false;
        }
        if (object == this) {
            return true;
        }
        if (!(object instanceof Product)) {
            return false;
        }
        Product product = (Product)object;
        if (mComparator == null){
            return false;
        }else{
            return mComparator.equals(this, product);
        }
    }

    public String getBrand() {
        return mBrand;
    }

    public String getVariant() {
        return mVariant;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static Product fromString(String json) {
        try{
            JSONObject jsonObject = new JSONObject(json);
            return fromJson(jsonObject);
        }catch (JSONException jse){

        }
        return null;
    }

    static Product fromJson(JSONObject jsonObject) {
        try{
            Product.Builder builder =  new Product.Builder(jsonObject.getString("nm"), jsonObject.optString("id", null));
            builder.category(jsonObject.optString("ca", null));
            builder.couponCode(jsonObject.optString("cc", null));
            if (jsonObject.has("ps")) {
                builder.position(jsonObject.optInt("ps", 0));
            }
            if (jsonObject.has("pr")) {
                builder.unitPrice(jsonObject.optDouble("pr", 0));
            }
            if (jsonObject.has("qt")) {
                builder.quantity(jsonObject.optInt("qt", 1));
            }
            builder.brand(jsonObject.optString("br", null));
            builder.variant(jsonObject.optString("va", null));
            if (jsonObject.has("attrs")){
                JSONObject attributesJson = jsonObject.getJSONObject("attrs");
                if (attributesJson.length() > 0) {
                    Map<String, String> customAttributes = new HashMap<String, String>();
                    Iterator<String> keys = attributesJson.keys();

                    while( keys.hasNext() ) {
                        String key = keys.next();
                        customAttributes.put(key, attributesJson.getString(key));
                    }
                    builder.customAttributes(customAttributes);
                }
            }
            return builder.build();
        }catch (JSONException jse){

        }
        return null;
    }

    JSONObject toJson() {
        try{
            JSONObject productJson = new JSONObject();
            if (mName != null){
                productJson.put("nm", mName);
            }
            if (mCategory != null){
                productJson.put("ca", mCategory);
            }
            if (mCouponCode != null){
                productJson.put("cc", mCouponCode);
            }
            if (mSku != null){
                productJson.put("id", mSku);
            }
            if (mPosition != null){
                productJson.put("ps", mPosition);
            }
            if (mPrice != null){
                productJson.put("pr", mPrice);
            }
            productJson.put("qt", mQuantity);

            if (mBrand != null){
                productJson.put("br", mBrand);
            }
            if (mVariant != null){
                productJson.put("va", mVariant);
            }
            if (mCustomAttributes != null && mCustomAttributes.size() > 0){
                JSONObject attributes = new JSONObject();
                for (Map.Entry<String, String> entry : mCustomAttributes.entrySet())
                {
                    attributes.put(entry.getKey(), entry.getValue());
                }
                productJson.put("attrs", attributes);
            }
            return productJson;
        }catch(JSONException jse){

        }
        return new JSONObject();
    }

    void setQuantity(double quantity) {
        mQuantity = quantity;
    }

    public static class Builder {
        private String mName = null;
        private String mCategory;
        private String mCouponCode;
        private String mSku;

        private Integer mPosition;
        private Double mPrice;
        private double mQuantity = 1;
        private String mBrand;
        private String mVariant;
        private Map<String, String> mCustomAttributes = null;

        private Builder(){}

        public Builder(String name, String sku){
            mName = name;
            mSku = sku;
        }

        public Builder(Product product){

        }

        public Builder customAttributes(Map<String, String> attributes) {
            mCustomAttributes = attributes;
            return this;
        }

        public Builder category(String category) {
            mCategory = category;
            return this;
        }

        public Builder couponCode(String couponCode) {
            mCouponCode = couponCode;
            return this;
        }

        public Builder sku(String sku) {
            mSku = sku;
            return this;
        }

        public Builder name(String name) {
            mName = name;
            return this;
        }

        public Builder position(Integer position) {
            mPosition = position;
            return this;
        }

        public Builder unitPrice(Double price) {
            mPrice = price;
            return this;
        }

        public Builder quantity(double quantity) {
            mQuantity = quantity;
            return this;
        }

        public Builder brand(String brand) {
            mBrand = brand;
            return this;
        }

        public Builder variant(String variant){
            mVariant = variant;
            return this;
        }

        public Product build() {
            return new Product(this);
        }
    }
}
