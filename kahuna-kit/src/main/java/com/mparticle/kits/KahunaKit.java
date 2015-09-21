package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.kahuna.sdk.KahunaAnalytics;
import com.kahuna.sdk.KahunaPushReceiver;
import com.kahuna.sdk.KahunaPushService;
import com.kahuna.sdk.KahunaUserAttributesKeys;
import com.kahuna.sdk.KahunaUserCredentialKeys;
import com.mparticle.Constants;
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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p/>
 * Embedded implementation of the Kahuna SDK
 * <p/>
 */
public class KahunaKit extends AbstractKit implements ActivityLifecycleForwarder, ECommerceForwarder, ClientSideForwarder {

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

    @Override
    protected AbstractKit update() {
        if (!initialized) {
            KahunaAnalytics.setDebugMode(MParticle.getInstance().getEnvironment() == MParticle.Environment.Development);
            if (mEkManager.getConfigurationManager().isPushEnabled()) {
                // registerForPush(context);
                KahunaAnalytics.onAppCreate(context, properties.get(KEY_SECRET_KEY), mEkManager.getConfigurationManager().getPushSenderId());
                KahunaAnalytics.disableKahunaGenerateNotifications();
            } else {
                KahunaAnalytics.onAppCreate(context, properties.get(KEY_SECRET_KEY), null);
            }
            KahunaAnalytics.start();

            initialized = true;
            if (!isServiceAvailable(context, KahunaPushService.class)) {
                Log.e("mParticle SDK", "The Kahuna SDK is enabled for this application, but you have not added <service android:name=\"com.kahuna.sdk.KahunaPushService\" /> to the <application> section in your application's AndroidManifest.xml");
            }
        }
        sendTransactionData = properties.containsKey(KEY_TRANSACTION_DATA) && Boolean.parseBoolean(properties.get(KEY_TRANSACTION_DATA));
        return this;
    }


    public static boolean isServiceAvailable(Context context, Class<?> service) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(context, service);
        List resolveInfo =
                packageManager.queryIntentServices(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() > 0) {
            return true;
        }
        return false;
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
    public List<ReportingMessage> logEvent(MPEvent event) throws Exception {
        if (!TextUtils.isEmpty(event.getEventName())) {
            List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
            Map<String, String> eventAttributes = event.getInfo();
            if (sendTransactionData && eventAttributes != null && eventAttributes.containsKey(Constants.Commerce.RESERVED_KEY_LTV)) {
                Double amount = Double.parseDouble(eventAttributes.get(Constants.Commerce.RESERVED_KEY_LTV)) * 100;
                KahunaAnalytics.trackEvent("purchase", 1, amount.intValue());
                if (eventAttributes != null) {
                    this.setUserAttributes(eventAttributes);
                }
                messages.add(ReportingMessage.fromEvent(this,
                        new MPEvent.Builder(event).eventName("purchase").build())
                );
                return messages;
            } else {
                KahunaAnalytics.trackEvent(event.getEventName());
                if (eventAttributes != null) {
                    KahunaAnalytics.setUserAttributes(eventAttributes);
                }
                messages.add(ReportingMessage.fromEvent(this, event)
                );
                return messages;
            }
        }
        return null;
    }

    @Override
    public List<ReportingMessage> logTransaction(MPProduct transaction) throws Exception {
        if (sendTransactionData && transaction != null) {
            Double revenue = transaction.getTotalAmount() * 100;
            KahunaAnalytics.trackEvent("purchase", (int) transaction.getQuantity(), revenue.intValue());
            List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
            messages.add(ReportingMessage.fromTransaction(this, transaction)
                    .setEventName("purchase")
            );
            return messages;
        }
        return null;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        return null;
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
    public List<ReportingMessage> handleIntent(Intent intent) {
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        String action = intent.getAction();
        if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
            if (KahunaKit.sReceiver == null) {
                registerForPush();
                intent.putExtra(MPReceiver.MPARTICLE_IGNORE, true);
                context.sendBroadcast(intent);
                messageList.add(ReportingMessage.fromPushRegistrationMessage(this, intent));
            }
        } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
            if (KahunaKit.sReceiver == null) {
                registerForPush();
                intent.putExtra(MPReceiver.MPARTICLE_IGNORE, true);
                context.sendBroadcast(intent);
                messageList.add(ReportingMessage.fromPushMessage(this, intent));
            }
        }
        return messageList;
    }

    @Override
    public void startSession() {

    }

    @Override
    public void endSession() {

    }

    @Override
    List<ReportingMessage> logout() {
        KahunaAnalytics.logout();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(ReportingMessage.logoutMessage(this));
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, int activityCount) {
        update();
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity, int activityCount) {
        update();
        KahunaAnalytics.stop();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int activityCount) {
        update();
        KahunaAnalytics.start();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) throws Exception {
        String eventName = event.getEventName();
        if (TextUtils.isEmpty(eventName)) {
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
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event).setEventName(eventName).setAttributes(eventAttributes));
        return messages;
    }
}