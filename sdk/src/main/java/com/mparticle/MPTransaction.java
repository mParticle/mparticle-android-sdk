package com.mparticle;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sdozor on 1/22/14.
 */
public class MPTransaction {
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
            MParticle.setCheckedAttribute(transactionInfo, "ProductName", builder.productName);
            MParticle.setCheckedAttribute(transactionInfo, "ProductSKU", builder.productSku);

            if (builder.affiliation != null && builder.affiliation.length() > 0)
                MParticle.setCheckedAttribute(transactionInfo, "TransactionAffiliation", builder.affiliation);

            if (builder.productUnitPrice != null)
                MParticle.setCheckedAttribute(transactionInfo, "ProductUnitPrice", Double.toString(builder.productUnitPrice));

            if (builder.quantity != null)
                MParticle.setCheckedAttribute(transactionInfo, "ProductQuantity", Double.toString(builder.quantity));

            if (builder.revenueAmount != null)
                MParticle.setCheckedAttribute(transactionInfo, "RevenueAmount", Double.toString(builder.revenueAmount));

            if (builder.taxAmount != null)
                MParticle.setCheckedAttribute(transactionInfo, "TaxAmount", Double.toString(builder.taxAmount));

            if (builder.shippingAmount != null)
                MParticle.setCheckedAttribute(transactionInfo, "ShippingAmount", Double.toString(builder.shippingAmount));

            if (builder.productCategory != null)
                MParticle.setCheckedAttribute(transactionInfo, "ProductCategory", builder.productCategory);

            if (builder.currencyCode != null)
                MParticle.setCheckedAttribute(transactionInfo, "CurrencyCode", builder.currencyCode);

            if (builder.transactionId != null && builder.transactionId.length() > 0)
                MParticle.setCheckedAttribute(transactionInfo, "TransactionID", builder.transactionId);


        } catch (JSONException jse) {
            Log.w(Constants.LOG_TAG, "Failed to create transaction: " + jse.getMessage());
        }

    }

    public JSONObject getData() {
        return transactionInfo;
    }

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


        public Builder(String productName, String productSku) {
            this.productName = productName;
            this.productSku = productSku;
        }

        public Builder unitPrice(double val) {
            productUnitPrice = val;
            return this;
        }

        public Builder quantity(int val) {
            quantity = val;
            return this;
        }

        public Builder productCategory(String val) {
            productCategory = val;
            return this;
        }

        public Builder totalRevenue(double val) {
            revenueAmount = val;
            return this;
        }

        public Builder taxAmount(double val) {
            taxAmount = val;
            return this;
        }

        public Builder shippingAmount(double val) {
            shippingAmount = val;
            return this;
        }

        public Builder currencyCode(String val) {
            currencyCode = val;
            return this;
        }

        public Builder affiliation(String val) {
            affiliation = val;
            return this;
        }

        public Builder transactionId(String val) {
            transactionId = val;
            return this;
        }

        public MPTransaction build() {
            return new MPTransaction(this);
        }
    }
}
