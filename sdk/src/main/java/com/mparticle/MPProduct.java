package com.mparticle;

import java.util.HashMap;

/**
 *
 * Class used to log an e-commerce transaction.
 *
 *  @see com.mparticle.MParticle#logTransaction(MPProduct)
 *
 */
public class MPProduct extends HashMap<String, String> {


    private MPProduct(Builder builder) {
        if (builder.productName == null) {
            throw new IllegalStateException("productName is required for a transaction");
        }
        if (builder.productSku == null) {
            throw new IllegalStateException("productSku is required for a transaction");
        }

        put("$MethodName", "LogEcommerceTransaction");
        put("ProductName", builder.productName);
        put("ProductSKU", builder.productSku);

        if (builder.affiliation != null && builder.affiliation.length() > 0)
            put("TransactionAffiliation", builder.affiliation);

        if (builder.productUnitPrice != null)
            put("ProductUnitPrice", Double.toString(builder.productUnitPrice));

        if (builder.quantity != null)
            put("ProductQuantity", Double.toString(builder.quantity));

        if (builder.revenueAmount != null)
            put("RevenueAmount", Double.toString(builder.revenueAmount));

        if (builder.taxAmount != null)
            put("TaxAmount", Double.toString(builder.taxAmount));

        if (builder.shippingAmount != null)
            put("ShippingAmount", Double.toString(builder.shippingAmount));

        if (builder.productCategory != null)
            put("ProductCategory", builder.productCategory);

        if (builder.currencyCode != null)
            put("CurrencyCode", builder.currencyCode);

        if (builder.transactionId != null && builder.transactionId.length() > 0)
            put("TransactionID", builder.transactionId);

    }

    /**
     * Class used to build an {@code MPTransaction} object.
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
         * create the {@code MPTransaction} object.
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
         * The final step in creating an {@code MPTransaction} object, to be passed into {@link com.mparticle.MParticle#logTransaction(MPProduct)}.
         * This method will perform validation on the fields and will throw an {@code IllegalStateException} if {@code productName} or {@code productCode} are null.
         *
         * @return The {@code MPTransaction}
         */
        public MPProduct build() {
            return new MPProduct(this);
        }
    }
}
