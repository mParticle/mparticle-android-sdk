package com.mparticle.kits;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.MPUtility;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class CommerceEventUtils {

    static final String PLUSONE_NAME = "eCommerce - %s - Total";
    private static final String ITEM_NAME = "eCommerce - %s - Item";
    private static final String IMPRESSION_NAME = "eCommerce - Impression - Item";

    private CommerceEventUtils() {
    }

    public static List<MPEvent> expand(CommerceEvent event) {
        List<MPEvent> eventList = new LinkedList<MPEvent>();
        if (event == null) {
            return eventList;
        }
        eventList.addAll(expandProductAction(event));
        eventList.addAll(expandPromotionAction(event));
        eventList.addAll(expandProductImpression(event));
        return eventList;
    }

    public static List<MPEvent> expandProductAction(CommerceEvent event) {
        List<MPEvent> events = new LinkedList<MPEvent>();
        String productAction = event.getProductAction();
        if (productAction == null) {
            return events;
        }
        if (productAction.equalsIgnoreCase(Product.PURCHASE) || productAction.equalsIgnoreCase(Product.REFUND)) {
            MPEvent.Builder plusOne = new MPEvent.Builder(String.format(PLUSONE_NAME, event.getProductAction()), MParticle.EventType.Transaction);
            //Set all product action fields to attributes.
            Map<String, String> attributes = new HashMap<String, String>();
            //Start with the custom attributes then overwrite with action fields.
            if (event.getCustomAttributes() != null) {
                attributes.putAll(event.getCustomAttributes());
            }
            extractActionAttributes(event, attributes);
            events.add(plusOne.customAttributes(attributes).build());
        }
        List<Product> products = event.getProducts();
        if (products != null) {
            for (int i = 0; i < products.size(); i++) {
                MPEvent.Builder itemEvent = new MPEvent.Builder(String.format(ITEM_NAME, productAction), MParticle.EventType.Transaction);
                Map<String, String> attributes = new HashMap<String, String>();
                extractProductFields(products.get(i), attributes);
                extractProductAttributes(products.get(i), attributes);
                extractTransactionId(event, attributes);
                events.add(itemEvent.customAttributes(attributes).build());
            }
        }
        return events;
    }

    public static void extractProductFields(Product product, Map<String, String> attributes) {
        if (product != null) {
            if (!MPUtility.isEmpty(product.getCouponCode())) {
                attributes.put(Constants.ATT_PRODUCT_COUPON_CODE, product.getCouponCode());
            }
            if (!MPUtility.isEmpty(product.getBrand())) {
                attributes.put(Constants.ATT_PRODUCT_BRAND, product.getBrand());
            }
            if (!MPUtility.isEmpty(product.getCategory())) {
                attributes.put(Constants.ATT_PRODUCT_CATEGORY, product.getCategory());
            }
            if (!MPUtility.isEmpty(product.getName())) {
                attributes.put(Constants.ATT_PRODUCT_NAME, product.getName());
            }
            if (!MPUtility.isEmpty(product.getSku())) {
                attributes.put(Constants.ATT_PRODUCT_ID, product.getSku());
            }
            if (!MPUtility.isEmpty(product.getVariant())) {
                attributes.put(Constants.ATT_PRODUCT_VARIANT, product.getVariant());
            }
            if (product.getPosition() != null) {
                attributes.put(Constants.ATT_PRODUCT_POSITION, Integer.toString(product.getPosition()));
            }
            attributes.put(Constants.ATT_PRODUCT_PRICE, Double.toString(product.getUnitPrice()));
            attributes.put(Constants.ATT_PRODUCT_QUANTITY, Double.toString(product.getQuantity()));
            attributes.put(Constants.ATT_PRODUCT_TOTAL_AMOUNT, Double.toString(product.getTotalAmount()));
        }
    }

    public static void extractProductAttributes(Product product, Map<String, String> attributes) {
        if (product != null) {
            if (product.getCustomAttributes() != null) {
                attributes.putAll(product.getCustomAttributes());
            }
        }
    }

    private static void extractTransactionId(CommerceEvent event, Map<String, String> attributes) {
        if (event != null && event.getTransactionAttributes() != null && !MPUtility.isEmpty(event.getTransactionAttributes().getId())) {
            attributes.put(Constants.ATT_TRANSACTION_ID, event.getTransactionAttributes().getId());
        }
    }

    public static void extractActionAttributes(CommerceEvent event, Map<String, String> attributes) {
        extractTransactionAttributes(event, attributes);
        extractTransactionId(event, attributes);
        String currency = event.getCurrency();
        if (MPUtility.isEmpty(currency)) {
            currency = Constants.DEFAULT_CURRENCY_CODE;
        }
        attributes.put(Constants.ATT_ACTION_CURRENCY_CODE, currency);
        String checkoutOptions = event.getCheckoutOptions();
        if (!MPUtility.isEmpty(checkoutOptions)) {
            attributes.put(Constants.ATT_ACTION_CHECKOUT_OPTIONS, checkoutOptions);
        }
        if (event.getCheckoutStep() != null) {
            attributes.put(Constants.ATT_ACTION_CHECKOUT_STEP, Integer.toString(event.getCheckoutStep()));
        }
        if (!MPUtility.isEmpty(event.getProductListSource())) {
            attributes.put(Constants.ATT_ACTION_PRODUCT_LIST_SOURCE, event.getProductListSource());
        }
        if (!MPUtility.isEmpty(event.getProductListName())) {
            attributes.put(Constants.ATT_ACTION_PRODUCT_ACTION_LIST, event.getProductListName());
        }
    }

    public static Map<String, String> extractTransactionAttributes(CommerceEvent event, Map<String, String> attributes) {
        if (event == null || event.getTransactionAttributes() == null) {
            return attributes;
        }
        TransactionAttributes transactionAttributes = event.getTransactionAttributes();
        extractTransactionId(event, attributes);
        if (!MPUtility.isEmpty(transactionAttributes.getAffiliation())) {
            attributes.put(Constants.ATT_AFFILIATION, transactionAttributes.getAffiliation());
        }
        if (!MPUtility.isEmpty(transactionAttributes.getCouponCode())) {
            attributes.put(Constants.ATT_TRANSACTION_COUPON_CODE, transactionAttributes.getCouponCode());
        }
        if (transactionAttributes.getRevenue() != null) {
            attributes.put(Constants.ATT_TOTAL, Double.toString(transactionAttributes.getRevenue()));
        }
        if (transactionAttributes.getShipping() != null) {
            attributes.put(Constants.ATT_SHIPPING, Double.toString(transactionAttributes.getShipping()));
        }
        if (transactionAttributes.getTax() != null) {
            attributes.put(Constants.ATT_TAX, Double.toString(transactionAttributes.getTax()));
        }

        return attributes;
    }

    public static List<MPEvent> expandPromotionAction(CommerceEvent event) {
        List<MPEvent> events = new LinkedList<MPEvent>();
        String promotionAction = event.getPromotionAction();
        if (promotionAction == null) {
            return events;
        }
        List<Promotion> promotions = event.getPromotions();
        if (promotions != null) {
            for (int i = 0; i < promotions.size(); i++) {
                MPEvent.Builder itemEvent = new MPEvent.Builder(String.format(ITEM_NAME, promotionAction), MParticle.EventType.Transaction);
                Map<String, String> attributes = new HashMap<String, String>();
                if (event.getCustomAttributes() != null) {
                    attributes.putAll(event.getCustomAttributes());
                }
                extractPromotionAttributes(promotions.get(i), attributes);
                events.add(itemEvent.customAttributes(attributes).build());
            }
        }
        return events;
    }

    public static void extractPromotionAttributes(Promotion promotion, Map<String, String> attributes) {
        if (promotion != null) {
            if (!MPUtility.isEmpty(promotion.getId())) {
                attributes.put(Constants.ATT_PROMOTION_ID, promotion.getId());
            }
            if (!MPUtility.isEmpty(promotion.getPosition())) {
                attributes.put(Constants.ATT_PROMOTION_POSITION, promotion.getPosition());
            }
            if (!MPUtility.isEmpty(promotion.getName())) {
                attributes.put(Constants.ATT_PROMOTION_NAME, promotion.getName());
            }
            if (!MPUtility.isEmpty(promotion.getCreative())) {
                attributes.put(Constants.ATT_PROMOTION_CREATIVE, promotion.getCreative());
            }
        }
    }

    public static List<MPEvent> expandProductImpression(CommerceEvent event) {
        List<Impression> impressions = event.getImpressions();
        List<MPEvent> events = new LinkedList<MPEvent>();
        if (impressions == null) {
            return events;
        }
        for (int i = 0; i < impressions.size(); i++) {
            List<Product> products = impressions.get(i).getProducts();
            if (products != null) {
                for (int j = 0; j < products.size(); j++) {
                    MPEvent.Builder itemEvent = new MPEvent.Builder(IMPRESSION_NAME, MParticle.EventType.Transaction);
                    Map<String, String> attributes = new HashMap<String, String>();
                    if (event.getCustomAttributes() != null) {
                        attributes.putAll(event.getCustomAttributes());
                    }
                    extractProductAttributes(products.get(i), attributes);
                    extractProductFields(products.get(i), attributes);
                    extractImpressionAttributes(impressions.get(i), attributes);
                    events.add(itemEvent.customAttributes(attributes).build());
                }
            }
        }
        return events;
    }

    private static void extractImpressionAttributes(Impression impression, Map<String, String> attributes) {
        if (impression != null) {

            if (!MPUtility.isEmpty(impression.getListName())) {
                attributes.put("Product Impression List", impression.getListName());
            }
        }
    }

    public static String getEventTypeString(CommerceEvent filteredEvent) {
        int eventType = getEventType(filteredEvent);
        switch (eventType) {
            case Constants.EVENT_TYPE_ADD_TO_CART:
                return Constants.EVENT_TYPE_STRING_ADD_TO_CART;
            case Constants.EVENT_TYPE_REMOVE_FROM_CART:
                return Constants.EVENT_TYPE_STRING_REMOVE_FROM_CART;
            case Constants.EVENT_TYPE_CHECKOUT:
                return Constants.EVENT_TYPE_STRING_CHECKOUT;
            case Constants.EVENT_TYPE_CHECKOUT_OPTION:
                return Constants.EVENT_TYPE_STRING_CHECKOUT_OPTION;
            case Constants.EVENT_TYPE_CLICK:
                return Constants.EVENT_TYPE_STRING_CLICK;
            case Constants.EVENT_TYPE_VIEW_DETAIL:
                return Constants.EVENT_TYPE_STRING_VIEW_DETAIL;
            case Constants.EVENT_TYPE_PURCHASE:
                return Constants.EVENT_TYPE_STRING_PURCHASE;
            case Constants.EVENT_TYPE_REFUND:
                return Constants.EVENT_TYPE_STRING_REFUND;
            case Constants.EVENT_TYPE_ADD_TO_WISHLIST:
                return Constants.EVENT_TYPE_STRING_ADD_TO_WISHLIST;
            case Constants.EVENT_TYPE_REMOVE_FROM_WISHLIST:
                return Constants.EVENT_TYPE_STRING_REMOVE_FROM_WISHLIST;
            case Constants.EVENT_TYPE_PROMOTION_VIEW:
                return Constants.EVENT_TYPE_STRING_PROMOTION_VIEW;
            case Constants.EVENT_TYPE_PROMOTION_CLICK:
                return Constants.EVENT_TYPE_STRING_PROMOTION_CLICK;
            case Constants.EVENT_TYPE_IMPRESSION:
                return Constants.EVENT_TYPE_STRING_IMPRESSION;

        }
        return Constants.EVENT_TYPE_STRING_UNKNOWN;
    }

    public static int getEventType(CommerceEvent filteredEvent) {
        if (!MPUtility.isEmpty(filteredEvent.getProductAction())) {
            String action = filteredEvent.getProductAction();
            if (action.equalsIgnoreCase(Product.ADD_TO_CART)) {
                return Constants.EVENT_TYPE_ADD_TO_CART;
            } else if (action.equalsIgnoreCase(Product.REMOVE_FROM_CART)) {
                return Constants.EVENT_TYPE_REMOVE_FROM_CART;
            } else if (action.equalsIgnoreCase(Product.CHECKOUT)) {
                return Constants.EVENT_TYPE_CHECKOUT;
            } else if (action.equalsIgnoreCase(Product.CHECKOUT_OPTION)) {
                return Constants.EVENT_TYPE_CHECKOUT_OPTION;
            } else if (action.equalsIgnoreCase(Product.CLICK)) {
                return Constants.EVENT_TYPE_CLICK;
            } else if (action.equalsIgnoreCase(Product.DETAIL)) {
                return Constants.EVENT_TYPE_VIEW_DETAIL;
            } else if (action.equalsIgnoreCase(Product.PURCHASE)) {
                return Constants.EVENT_TYPE_PURCHASE;
            } else if (action.equalsIgnoreCase(Product.REFUND)) {
                return Constants.EVENT_TYPE_REFUND;
            } else if (action.equalsIgnoreCase(Product.ADD_TO_WISHLIST)) {
                return Constants.EVENT_TYPE_ADD_TO_WISHLIST;
            } else if (action.equalsIgnoreCase(Product.REMOVE_FROM_WISHLIST)) {
                return Constants.EVENT_TYPE_REMOVE_FROM_WISHLIST;
            }
        }
        if (!MPUtility.isEmpty(filteredEvent.getPromotionAction())) {
            String action = filteredEvent.getPromotionAction();
            if (action.equalsIgnoreCase(Promotion.VIEW)) {
                return Constants.EVENT_TYPE_PROMOTION_VIEW;
            } else if (action.equalsIgnoreCase(Promotion.CLICK)) {
                return Constants.EVENT_TYPE_PROMOTION_CLICK;
            }
        }
        return Constants.EVENT_TYPE_IMPRESSION;
    }

    public interface Constants {

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