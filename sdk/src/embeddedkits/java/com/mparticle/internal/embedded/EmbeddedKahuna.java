package com.mparticle.internal.embedded;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;

import com.kahuna.sdk.KahunaAnalytics;
import com.kahuna.sdk.KahunaPushReceiver;
import com.kahuna.sdk.KahunaPushService;
import com.kahuna.sdk.KahunaUserAttributesKeys;
import com.kahuna.sdk.KahunaUserCredentialKeys;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MPReceiver;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPActivityCallbacks;
import com.mparticle.internal.MPUtility;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p/>
 * Embedded implementation of the Kahuna SDK
 * <p/>
 */
class EmbeddedKahuna extends EmbeddedProvider implements MPActivityCallbacks, ECommerceForwarder {

    static KahunaPushReceiver sReceiver;
    boolean initialized = false;
    private static final String KEY_TRANSACTION_DATA = "sendTransactionData";
    private static final String KEY_SECRET_KEY = "secretKey";
    private boolean sendTransactionData = false;
    private static final String KEY_EVENTNAME_ADD_TO_CART = "defaultAddToCartEventName";
    private static final String KEY_EVENTNAME_REMOVE_FROM_CART = "defaultRemoveFromCartEventName";
    private static final String KEY_EVENTNAME_CHECKOUT = "defaultCheckoutEventName";
    private static final String KEY_EVENTNAME_CHECKOUT_OPTION = "defaultCheckoutOptionEventName";
    private static final String KEY_EVENTNAME_PRODUCT_CLICK = "defaultProductClickName";
    private static final String KEY_EVENTNAME_VIEW_DETAIL = "defaultViewDetailEventName";
    private static final String KEY_EVENTNAME_PURCHASE = "defaultPurchaseEventName";
    private static final String KEY_EVENTNAME_REFUND = "defaultRefundEventName";
    private static final String KEY_EVENTNAME_PROMOTION_VIEW = "defaultPromotionViewEventName";
    private static final String KEY_EVENTNAME_PROMOTION_CLICK = "defaultPromotionClickEventName";
    private static final String KEY_EVENTNAME_ADD_TO_WISHLIST = "defaultAddToWishlistEventName";
    private static final String KEY_EVENTNAME_REMOVE_FROM_WISHLIST = "defaultRemoveFromWishlistEventName";
    private static final String KEY_EVENTNAME_IMPRESSION = "defaultImpressionEventName";

    public EmbeddedKahuna(EmbeddedKitManager ekManager) {
        super(ekManager);
    }

    @Override
    protected EmbeddedProvider update() {
        if (!initialized) {
            KahunaAnalytics.setDebugMode(mEkManager.getConfigurationManager().getEnvironment() == MParticle.Environment.Development);
            if (mEkManager.getConfigurationManager().isPushEnabled()) {
                // registerForPush(context);
                KahunaAnalytics.onAppCreate(context, properties.get(KEY_SECRET_KEY), mEkManager.getConfigurationManager().getPushSenderId());
                KahunaAnalytics.disableKahunaGenerateNotifications();
            } else {
                KahunaAnalytics.onAppCreate(context, properties.get(KEY_SECRET_KEY), null);
            }
            KahunaAnalytics.start();

            initialized = true;
            if (!MPUtility.isServiceAvailable(context, KahunaPushService.class)) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "The Kahuna SDK is enabled for this application, but you have not added <service android:name=\"com.kahuna.sdk.KahunaPushService\" /> to the <application> section in your application's AndroidManifest.xml");
            }
        }
        sendTransactionData = properties.containsKey(KEY_TRANSACTION_DATA) && Boolean.parseBoolean(properties.get(KEY_TRANSACTION_DATA));
        return this;
    }

    public void registerForPush() {
        sReceiver = new KahunaPushReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.google.android.c2dm.intent.REGISTRATION");
        filter.addAction("com.google.android.c2dm.intent.RECEIVE");
        filter.addCategory(context.getPackageName());
        context.registerReceiver(sReceiver, filter, "com.google.android.c2dm.permission.SEND", null);
    }

    private String getEventName(CommerceEvent event) {
        int eventType = CommerceEventUtil.getEventType(event);
        switch (eventType) {
            case Constants.Commerce.EVENT_TYPE_ADD_TO_CART:
                return properties.get(KEY_EVENTNAME_ADD_TO_CART);
            case Constants.Commerce.EVENT_TYPE_REMOVE_FROM_CART:
                return properties.get(KEY_EVENTNAME_REMOVE_FROM_CART);
            case Constants.Commerce.EVENT_TYPE_CHECKOUT:
                return properties.get(KEY_EVENTNAME_CHECKOUT);
            case Constants.Commerce.EVENT_TYPE_CHECKOUT_OPTION:
                return properties.get(KEY_EVENTNAME_CHECKOUT_OPTION);
            case Constants.Commerce.EVENT_TYPE_CLICK:
                return properties.get(KEY_EVENTNAME_PRODUCT_CLICK);
            case Constants.Commerce.EVENT_TYPE_VIEW_DETAIL:
                return properties.get(KEY_EVENTNAME_VIEW_DETAIL);
            case Constants.Commerce.EVENT_TYPE_PURCHASE:
                return properties.get(KEY_EVENTNAME_PURCHASE);
            case Constants.Commerce.EVENT_TYPE_REFUND:
                return properties.get(KEY_EVENTNAME_REFUND);
            case Constants.Commerce.EVENT_TYPE_PROMOTION_VIEW:
                return properties.get(KEY_EVENTNAME_PROMOTION_VIEW);
            case Constants.Commerce.EVENT_TYPE_PROMOTION_CLICK:
                return properties.get(KEY_EVENTNAME_PROMOTION_CLICK);
            case Constants.Commerce.EVENT_TYPE_ADD_TO_WISHLIST:
                return properties.get(KEY_EVENTNAME_ADD_TO_WISHLIST);
            case Constants.Commerce.EVENT_TYPE_REMOVE_FROM_WISHLIST:
                return properties.get(KEY_EVENTNAME_REMOVE_FROM_WISHLIST);
            case Constants.Commerce.EVENT_TYPE_IMPRESSION:
                return properties.get(KEY_EVENTNAME_IMPRESSION);
            default:
                return "Unknown";
        }
    }

    @Override
    public String getName() {
        return "Kahuna";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.contains("tap-nexus.appspot.com");
    }

    @Override
    public void logEvent(MPEvent event) throws Exception {
        if (!MPUtility.isEmpty(event.getEventName())) {
            Map<String, String> eventAttributes = event.getInfo();
            if (sendTransactionData && eventAttributes != null && eventAttributes.containsKey(Constants.MessageKey.RESERVED_KEY_LTV)) {
                Double amount = Double.parseDouble(eventAttributes.get(Constants.MessageKey.RESERVED_KEY_LTV)) * 100;
                KahunaAnalytics.trackEvent("purchase", 1, amount.intValue());
                if (eventAttributes != null)
                    this.setUserAttributes(eventAttributes);
            } else {
                KahunaAnalytics.trackEvent(event.getEventName());
                if (eventAttributes != null) {
                    KahunaAnalytics.setUserAttributes(eventAttributes);
                }
            }
        }
    }

    @Override
    public void logTransaction(MPProduct transaction) throws Exception {
        if (sendTransactionData && transaction != null) {
            Double revenue = transaction.getTotalAmount() * 100;
            KahunaAnalytics.trackEvent("purchase", (int) transaction.getQuantity(), revenue.intValue());
        }
    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {

    }

    @Override
    public void setLocation(Location location) {

    }

    @Override
    public void setUserAttributes(JSONObject attributes) {
        Map<String, String> kahunaAttributes = null;
        if (attributes != null) {
            Iterator<String> keys = attributes.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = attributes.optString(key, "");
                if (kahunaAttributes == null) {
                    kahunaAttributes = new HashMap<String, String>(attributes.length());
                }
                kahunaAttributes.put(convertMpKeyToKahuna(key), value);
            }
        }
        if (kahunaAttributes != null) {
            KahunaAnalytics.setUserAttributes(kahunaAttributes);
        }
    }

    private void setUserAttributes(Map<String, String> attributes) {
        if (attributes != null) {
            HashMap<String, String> kahunaAttributes = new HashMap<String, String>(attributes.size());
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                kahunaAttributes.put(convertMpKeyToKahuna(entry.getKey()), entry.getValue());
            }
        }
    }

    private static String convertMpKeyToKahuna(String key) {
        if (key.equals(MParticle.UserAttributes.FIRSTNAME)) {
            return KahunaUserAttributesKeys.FIRST_NAME_KEY;
        } else if (key.equals(MParticle.UserAttributes.LASTNAME)) {
            return KahunaUserAttributesKeys.LAST_NAME_KEY;
        } else if (key.equals(MParticle.UserAttributes.GENDER)) {
            return KahunaUserAttributesKeys.GENDER_KEY;
        } else {
            return key;
        }
    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        String kahunaKey;
        switch (identityType) {
            case Facebook:
                kahunaKey = KahunaUserCredentialKeys.FACEBOOK_KEY;
                break;
            case Email:
                kahunaKey = KahunaUserCredentialKeys.EMAIL_KEY;
                break;
            case Other:
                kahunaKey = "user_id";
                break;
            case CustomerId:
                kahunaKey = KahunaUserCredentialKeys.USERNAME_KEY;
                break;
            case Twitter:
                kahunaKey = KahunaUserCredentialKeys.TWITTER_KEY;
                break;
            default:
                return;

        }
        KahunaAnalytics.setUserCredential(kahunaKey, id);
    }

    @Override
    public void removeUserIdentity(String id) {

    }

    @Override
    public void handleIntent(Intent intent) {
        String action = intent.getAction();

        if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
            if (EmbeddedKahuna.sReceiver == null) {
                registerForPush();
                intent.putExtra(MPReceiver.MPARTICLE_IGNORE, true);
                context.sendBroadcast(intent);
            }
        } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
            if (EmbeddedKahuna.sReceiver == null) {
                registerForPush();
                intent.putExtra(MPReceiver.MPARTICLE_IGNORE, true);
                context.sendBroadcast(intent);
            }
        }
    }

    @Override
    public void startSession() {

    }

    @Override
    public void endSession() {

    }

    @Override
    public void logout() {
        KahunaAnalytics.logout();
    }


    @Override
    public void onActivityCreated(Activity activity, int activityCount) {
        update();
    }

    @Override
    public void onActivityResumed(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityStopped(Activity activity, int activityCount) {
        update();
        KahunaAnalytics.stop();
    }

    @Override
    public void onActivityStarted(Activity activity, int activityCount) {
        update();
        KahunaAnalytics.start();
    }

    @Override
    public void logEvent(CommerceEvent event) throws Exception {
        String eventName = event.getEventName();
        if (MPUtility.isEmpty(eventName)) {
            eventName = getEventName(event);
        }
        int quantity = 0;
        int eventType = CommerceEventUtil.getEventType(event);
        if (eventType == Constants.Commerce.EVENT_TYPE_IMPRESSION) {
            List<Impression> impressions = event.getImpressions();
            if (impressions != null) {
                for (Impression impression : impressions) {
                    List<Product> products = impression.getProducts();
                    if (products != null) {
                        for (Product product : products) {
                            quantity += product.getQuantity();
                        }
                    }
                }
            }
        } else if (eventType == Constants.Commerce.EVENT_TYPE_PROMOTION_VIEW
                || eventType == Constants.Commerce.EVENT_TYPE_PROMOTION_CLICK) {
            List<Promotion> promotions = event.getPromotions();
            if (promotions != null) {
                quantity = promotions.size();
            }
        } else {
            List<Product> products = event.getProducts();
            if (products != null) {
                for (Product product : products) {
                    quantity += product.getQuantity();
                }
            }
        }
        int revenue = 0;
        TransactionAttributes transactionAttributes = event.getTransactionAttributes();
        if (transactionAttributes != null) {
            Double transRevenue = transactionAttributes.getRevenue();
            if (transRevenue != null) {
                revenue = (int) (transRevenue.doubleValue() * 100);
            }
        }
        KahunaAnalytics.trackEvent(eventName, quantity, revenue);
        Map<String, String> eventAttributes = event.getCustomAttributes();
        if (eventAttributes != null && eventAttributes.size() > 0) {
            KahunaAnalytics.setUserAttributes(eventAttributes);
        }
    }
}