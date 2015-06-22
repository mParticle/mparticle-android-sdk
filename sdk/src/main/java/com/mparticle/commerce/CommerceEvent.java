package com.mparticle.commerce;


import java.util.Map;

public class CommerceEvent {
    public static final String ADD = "add";
    public static final String CHECKOUT = "checkout";
    public static final String CLICK = "click";
    public static final String DETAIL = "detail";
    public static final String PURCHASE = "purchase";
    public static final String REFUND = "refund";
    public static final String REMOVE = "remove";
    private String screen;

    private CommerceEvent(Builder builder) {
        super();
    }

    private CommerceEvent() {

    }

    @Override
    public String toString() {
        return super.toString();
    }

    public CommerceEvent addImpression(Product product, String where) {
        return this;
    }

    public static class Builder {
        private Builder() {
        }

        public Builder(String event) {

        }

        public Builder(String event, String eventName) {

        }

        public Builder screen(String screenName) {
            return this;
        }

        public Builder addProduct(Product... product) {
            return this;
        }

        public Builder transactionAttributes(TransactionAttributes attributes) {
            return this;
        }

        public Builder currency(String currency) {
            return this;
        }

        public Builder nonInteraction(boolean userTriggered) {
            return this;
        }

        public Builder customAttributes(Map<String, String> attributes) {
            return this;
        }

        public Builder addPromotion(Promotion promotion) {
            return this;
        }

        public Builder checkoutStep(int step) {
            return this;
        }

        public Builder checkoutOptions(String options) {
            return this;
        }

        public CommerceEvent build() {
            return new CommerceEvent(this);
        }

        public Builder productListName(String listName) {
            return this;
        }

        public Builder productListSource(String listSource) {
            return this;
        }
    }
}
