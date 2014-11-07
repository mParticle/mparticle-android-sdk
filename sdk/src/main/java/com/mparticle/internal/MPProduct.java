package com.mparticle.internal;

import java.util.HashMap;

/**
 *
 * Use this class to represent a product that a user could purchase or otherwise interact with.
 *
 *  @see com.mparticle.MParticle#logProductEvent(MPProduct.Event, MPProduct)
 *  @see com.mparticle.MParticle#logTransaction(MPProduct)
 *
 */
    
public class MPProduct extends HashMap<String, String> {

    /**
     *  Use this enumeration to conveniently log common product interactions.
     *
     *  @see com.mparticle.MParticle#logProductEvent(MPProduct.Event, MPProduct)
     */
    public static enum Event {
        VIEW("Product Viewed"),
        ADD_TO_WISHLIST("Added to Wishlist"),
        REMOVE_FROM_WISHLIST("Removed from Wishlist"),
        ADD_TO_CART("Added to Cart"),
        REMOVE_FROM_CART("Removed from Cart"),
        PURCHASE("Product Purchased");

        private final String description;
        Event(String description){
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public static final String NAME = "ProductName";
    public static final String SKU = "ProductSKU";
    public static final String AFFILIATION = "TransactionAffiliation";
    public static final String UNITPRICE = "ProductUnitPrice";
    public static final String QUANTITY = "ProductQuantity";
    public static final String REVENUE = "RevenueAmount";
    public static final String TAX = "TaxAmount";
    public static final String SHIPPING = "ShippingAmount";
    public static final String CATEGORY = "ProductCategory";
    public static final String CURRENCY = "CurrencyCode";
    public static final String TRANSACTION_ID = "TransactionID";


    private MPProduct(){
        //prevent instantiation
    }

    private MPProduct(Builder builder) {
        if (builder.productName == null) {
            throw new IllegalStateException("productName is required for a transaction");
        }
        if (builder.productSku == null) {
            throw new IllegalStateException("productSku is required for a transaction");
        }


        put(NAME, builder.productName);
        put(SKU, builder.productSku);

        if (builder.affiliation != null && builder.affiliation.length() > 0)
            put(AFFILIATION, builder.affiliation);

        if (builder.productUnitPrice != null)
            put(UNITPRICE, Double.toString(builder.productUnitPrice));

        if (builder.quantity != null)
            put(QUANTITY, Double.toString(builder.quantity));

        if (builder.revenueAmount != null)
            put(REVENUE, Double.toString(builder.revenueAmount));

        if (builder.taxAmount != null)
            put(TAX, Double.toString(builder.taxAmount));

        if (builder.shippingAmount != null)
            put(SHIPPING, Double.toString(builder.shippingAmount));

        if (builder.productCategory != null)
            put(CATEGORY, builder.productCategory);

        if (builder.currencyCode != null)
            put(CURRENCY, builder.currencyCode);

        if (builder.transactionId != null && builder.transactionId.length() > 0)
            put(TRANSACTION_ID, builder.transactionId);

    }

    public double getUnitPrice(){
        return Double.parseDouble(get(UNITPRICE, "0"));
    }

    public double getQuantity(){
        return Double.parseDouble(get(QUANTITY, "0"));
    }

    public String getProductCategory(){
        return get(CATEGORY, null);
    }

    public double getTotalRevenue(){
        return Double.parseDouble(get(REVENUE, "0"));
    }

    public double getTaxAmount(){
        return Double.parseDouble(get(TAX, "0"));
    }

    public double getShippingAmount(){
        return Double.parseDouble(get(SHIPPING, "0"));
    }

    public String getCurrencyCode(){
        return get(CURRENCY, null);
    }

    public String getAffiliation(){
        return get(AFFILIATION, null);
    }

    public String getTransactionId(){
        return get(TRANSACTION_ID, null);
    }

    public String getProductName(){
        return get(NAME, null);
    }

    public String get(Object key, String defaultValue) {
        return containsKey(key) ? super.get(key) : defaultValue;
    }

    /**
     * Class used to build an {@link MPProduct} object.
     *
     * @see com.mparticle.MParticle#logTransaction(MPProduct)
     */
    public static class Builder {
        //required parameters
        private final String productName;
        private final String productSku;
        private Double productUnitPrice;
        private Integer quantity;
        private String productCategory = null;
        private Double revenueAmount;
        private Double taxAmount;
        private Double shippingAmount;
        private String currencyCode = null;
        private String affiliation = null;
        private String transactionId = null;


        /**
         * Starting point of the builder with two required parameters. The rest of the fields
         * of this class are optional. Once the desired fields have been set, use {@link #build()} to
         * create the {@link MPProduct} object.
         *
         * @param productName The name of the product that was purchased
         * @param productSku The Sku or internal label for the product that was purchased
         */
        public Builder(String productName, String productSku) {
            this.productName = productName;
            this.productSku = productSku;
        }

        /**
         * The price per unit of the product purchased
         *
         * @param val unit price
         * @return The {@code Builder} with the unit price set.
         */
        public Builder unitPrice(double val) {
            productUnitPrice = val;
            return this;
        }

        /**
         * The number of items/units that were purchased in this transaction
         *
         * @param val quantity
         * @return The {@code Builder} with the quantity set.
         */
        public Builder quantity(int val) {
            quantity = val;
            return this;
        }

        /**
         * A category to assign to the product(s) purchased in this transaction.
         *
         * @param val product category
         * @return The {@code Builder} with the category set.
         */
        public Builder productCategory(String val) {
            productCategory = val;
            return this;
        }

        /**
         * The total revenue received from this transaction
         *
         * @param val total revenue
         * @return The {@code Builder} with the total revenue set.
         */
        public Builder totalRevenue(double val) {
            revenueAmount = val;
            return this;
        }

        /**
         * The tax amount charged for this transaction
         *
         * @param val tax amount
         * @return The {@code Builder} with the tax amount set.
         */
        public Builder taxAmount(double val) {
            taxAmount = val;
            return this;
        }

        /**
         * The shipping amount charged for this transaction
         *
         * @param val shipping amount
         * @return The {@code Builder} with the shipping amount set.
         */
        public Builder shippingAmount(double val) {
            shippingAmount = val;
            return this;
        }

        /**
         * The currency code to be associate with this transaction
         *
         * @param val currency code
         * @return The {@code Builder} with the currency code set.
         */
        public Builder currencyCode(String val) {
            currencyCode = val;
            return this;
        }

        /**
         * The company affiliation to associate with this transaction
         *
         * @param val affiliation
         * @return The {@code Builder} with the company affiliation set.
         */
        public Builder affiliation(String val) {
            affiliation = val;
            return this;
        }

        /**
         * An additional ID to associate with this transaction
         *
         * @param val id
         * @return The {@code Builder} with an additional ID set.
         */
        public Builder transactionId(String val) {
            transactionId = val;
            return this;
        }

        /**
         * The final step in creating an {@link MPProduct} object, to be passed into {@link com.mparticle.MParticle#logTransaction(MPProduct)}.
         * This method will perform validation on the fields and will throw an {@code IllegalStateException} if {@code productName} or {@code productCode} are null.
         *
         * @return The {@link MPProduct}
         */
        public MPProduct build() {
            return new MPProduct(this);
        }
    }
}
