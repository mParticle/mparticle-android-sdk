package com.mparticle;

public class Constants {
    public interface Commerce {

        String ATT_AFFILIATION = "Affiliation";

        String ATT_TRANSACTION_COUPON_CODE = "Coupon Code";
        String ATT_TOTAL = "Total Amount";
        String ATT_SHIPPING = "Shipping Amount";
        String ATT_TAX = "Tax Amount";
        String ATT_TRANSACTION_ID = "Transaction Id";
        String ATT_PRODUCT_QUANTITY = "Quantity";
        String ATT_PRODUCT_POSITION = "Position";
        String ATT_PRODUCT_VARIANT = "Variant";
        String ATT_PRODUCT_ID = "Id";
        String ATT_PRODUCT_NAME = "Name";
        String ATT_PRODUCT_CATEGORY = "Category";
        String ATT_PRODUCT_BRAND = "Brand";
        String ATT_PRODUCT_COUPON_CODE = "Coupon Code";
        String ATT_PRODUCT_PRICE = "Item Price";
        String ATT_ACTION_PRODUCT_ACTION_LIST = "Product Action List";
        String ATT_ACTION_PRODUCT_LIST_SOURCE = "Product List Source";
        String ATT_ACTION_CHECKOUT_OPTIONS = "Checkout Options";
        String ATT_ACTION_CHECKOUT_STEP = "Checkout Step";
        String ATT_ACTION_CURRENCY_CODE = "Currency Code";
        String ATT_SCREEN_NAME = "Screen Name";
        String ATT_PROMOTION_ID = "Id";
        String ATT_PROMOTION_POSITION = "Position";
        String ATT_PROMOTION_NAME = "Name";
        String ATT_PROMOTION_CREATIVE = "Creative";
        String ATT_PRODUCT_TOTAL_AMOUNT = "Total Product Amount";
        String RESERVED_KEY_LTV = "$Amount";
        /**
         * This is only set when required. Otherwise default to null.
         */
        String DEFAULT_CURRENCY_CODE = "USD";

        int EVENT_TYPE_ADD_TO_CART = 10;
        int EVENT_TYPE_REMOVE_FROM_CART = 11;
        int EVENT_TYPE_CHECKOUT = 12;
        int EVENT_TYPE_CHECKOUT_OPTION = 13;
        int EVENT_TYPE_CLICK = 14;
        int EVENT_TYPE_VIEW_DETAIL = 15;
        int EVENT_TYPE_PURCHASE = 16;
        int EVENT_TYPE_REFUND = 17;
        int EVENT_TYPE_PROMOTION_VIEW = 18;
        int EVENT_TYPE_PROMOTION_CLICK = 19;
        int EVENT_TYPE_ADD_TO_WISHLIST = 20;
        int EVENT_TYPE_REMOVE_FROM_WISHLIST = 21;
        int EVENT_TYPE_IMPRESSION = 22;

        String EVENT_TYPE_STRING_ADD_TO_CART = "ProductAddToCart";
        String EVENT_TYPE_STRING_REMOVE_FROM_CART = "ProductRemoveFromCart";
        String EVENT_TYPE_STRING_CHECKOUT = "ProductCheckout";
        String EVENT_TYPE_STRING_CHECKOUT_OPTION = "ProductCheckoutOption";
        String EVENT_TYPE_STRING_CLICK = "ProductClick";
        String EVENT_TYPE_STRING_VIEW_DETAIL = "ProductViewDetail";
        String EVENT_TYPE_STRING_PURCHASE = "ProductPurchase";
        String EVENT_TYPE_STRING_REFUND = "ProductRefund";
        String EVENT_TYPE_STRING_PROMOTION_VIEW = "PromotionView";
        String EVENT_TYPE_STRING_PROMOTION_CLICK = "PromotionClick";
        String EVENT_TYPE_STRING_ADD_TO_WISHLIST = "ProductAddToWishlist";
        String EVENT_TYPE_STRING_REMOVE_FROM_WISHLIST = "ProductRemoveFromWishlist";
        String EVENT_TYPE_STRING_IMPRESSION = "ProductImpression";
        String EVENT_TYPE_STRING_UNKNOWN = "Unknown";

    }
}
