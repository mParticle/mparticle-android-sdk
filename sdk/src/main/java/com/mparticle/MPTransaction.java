package com.mparticle;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * Class used to log an e-commerce transaction.
 *
 *  @see com.mparticle.MParticle#logTransaction(MPTransaction)
 *
 */
public class MPTransaction {
    static final String NAME = "ProductName";
    static final String SKU = "ProductSKU";
    static final String AFFILIATION = "TransactionAffiliation";
    static final String UNITPRICE = "ProductUnitPrice";
    static final String QUANTITY = "ProductQuantity";
    static final String REVENUE = "RevenueAmount";
    static final String TAX = "TaxAmount";
    static final String SHIPPING = "ShippingAmount";
    static final String CATEGORY = "ProductCategory";
    static final String CURRENCY = "CurrencyCode";
    static final String TRANSACTION_ID = "TransactionID";
    private JSONObject transactionInfo;

    private MPTransaction(Builder builder) {
        if (builder.productName == null) {
            throw new IllegalStateException("productName is required for a transaction");
        }
        if (builder.productSku == null) {
            throw new IllegalStateException("productSku is required for a transaction");
        }

        try {
            transactionInfo = new JSONObject();
            transactionInfo.put("$MethodName", "LogEcommerceTransaction");
            MParticle.setCheckedAttribute(transactionInfo, NAME, builder.productName);
            MParticle.setCheckedAttribute(transactionInfo, SKU, builder.productSku);

            if (builder.affiliation != null && builder.affiliation.length() > 0)
                MParticle.setCheckedAttribute(transactionInfo, AFFILIATION, builder.affiliation);

            if (builder.productUnitPrice != null)
                MParticle.setCheckedAttribute(transactionInfo, UNITPRICE, Double.toString(builder.productUnitPrice));

            if (builder.quantity != null)
                MParticle.setCheckedAttribute(transactionInfo, QUANTITY, Double.toString(builder.quantity));

            if (builder.revenueAmount != null)
                MParticle.setCheckedAttribute(transactionInfo, REVENUE, Double.toString(builder.revenueAmount));

            if (builder.taxAmount != null)
                MParticle.setCheckedAttribute(transactionInfo, TAX, Double.toString(builder.taxAmount));

            if (builder.shippingAmount != null)
                MParticle.setCheckedAttribute(transactionInfo, SHIPPING, Double.toString(builder.shippingAmount));

            if (builder.productCategory != null)
                MParticle.setCheckedAttribute(transactionInfo, CATEGORY, builder.productCategory);

            if (builder.currencyCode != null)
                MParticle.setCheckedAttribute(transactionInfo, CURRENCY, builder.currencyCode);

            if (builder.transactionId != null && builder.transactionId.length() > 0)
                MParticle.setCheckedAttribute(transactionInfo, TRANSACTION_ID, builder.transactionId);


        } catch (JSONException jse) {
            Log.w(Constants.LOG_TAG, "Failed to create transaction: " + jse.getMessage());
        }

    }

    JSONObject getData() {
        return transactionInfo;
    }

    /**
     * Class used to build an {@code MPTransaction} object.
     *
     * @see com.mparticle.MParticle#logTransaction(MPTransaction)
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
         * The final step in creating an {@code MPTransaction} object, to be passed into {@link com.mparticle.MParticle#logTransaction(MPTransaction)}.
         * This method will perform validation on the fields and will throw an {@code IllegalStateException} if {@code productName} or {@code productCode} are null.
         *
         * @return The {@code MPTransaction}
         */
        public MPTransaction build() {
            return new MPTransaction(this);
        }
    }
}
