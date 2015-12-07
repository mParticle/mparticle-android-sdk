package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.kahuna.sdk.EmptyCredentialsException;
import com.kahuna.sdk.EventBuilder;
import com.kahuna.sdk.Kahuna;
import com.kahuna.sdk.KahunaPushReceiver;
import com.kahuna.sdk.KahunaPushService;
import com.kahuna.sdk.KahunaUserAttributesKeys;
import com.kahuna.sdk.IKahunaUserCredentials;
import com.kahuna.sdk.KahunaUserCredentials;
import com.mparticle.BuildConfig;
import com.mparticle.Constants;
import com.mparticle.MPEvent;
import com.mparticle.MPReceiver;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.CommerceEventUtil;

import org.json.JSONObject;

import java.math.BigDecimal;
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
    private static final String MPARTICLE_WRAPPER_KEY = "mparticle";
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
            Kahuna.getInstance().setDebugMode(MParticle.getInstance().getEnvironment() == MParticle.Environment.Development);
            if (mEkManager.getConfigurationManager().isPushEnabled()) {
                // registerForPush(context);
                Kahuna.getInstance().onAppCreate(context, properties.get(KEY_SECRET_KEY), mEkManager.getConfigurationManager().getPushSenderId());
                Kahuna.getInstance().disableKahunaGenerateNotifications();
            } else {
                Kahuna.getInstance().onAppCreate(context, properties.get(KEY_SECRET_KEY), null);
            }
            Kahuna.getInstance().setHybridSDKVersion(MPARTICLE_WRAPPER_KEY, BuildConfig.VERSION_NAME);

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
            String eventName = event.getEventName();
            Integer count = null;
            Integer value = null;
            if (sendTransactionData && eventAttributes != null && eventAttributes.containsKey(Constants.Commerce.RESERVED_KEY_LTV)) {
                Double amount = Double.parseDouble(eventAttributes.get(Constants.Commerce.RESERVED_KEY_LTV)) * 100;
                eventName = "purchase";
                count = 1;
                value = amount.intValue();
                messages.add(ReportingMessage.fromEvent(this,
                        new MPEvent.Builder(event).eventName("purchase").build())
                );
            } else {
                messages.add(ReportingMessage.fromEvent(this, event)
                );
            }

            trackKahunaEvent(eventName, count, value, eventAttributes);
            return messages;
        }
        return null;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
        if (sendTransactionData) {
            Double revenue = valueIncreased.doubleValue() * 100;
            trackKahunaEvent(eventName, 1, revenue.intValue(), null);
            List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
            messages.add(
                    new ReportingMessage(this, ReportingMessage.MessageType.EVENT, System.currentTimeMillis(), contextInfo)
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
            Kahuna.getInstance().setUserAttributes(kahunaAttributes);
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
                kahunaKey = KahunaUserCredentials.FACEBOOK_KEY;
                break;
            case Email:
                kahunaKey = KahunaUserCredentials.EMAIL_KEY;
                break;
            case CustomerId:
                kahunaKey = KahunaUserCredentials.USERNAME_KEY;
                break;
            case Twitter:
                kahunaKey = KahunaUserCredentials.TWITTER_KEY;
                break;
            case Google:
                kahunaKey = KahunaUserCredentials.GOOGLE_PLUS_ID;
                break;
            case FacebookCustomAudienceId:
                kahunaKey = "fb_app_user_id";
                break;
            case Microsoft:
                kahunaKey = "msft_id";
                break;
            case Yahoo:
                kahunaKey = "yahoo_id";
                break;
            case Other:
                kahunaKey = "mp_other_id";
                break;
            default:
                return;

        }
        IKahunaUserCredentials newCreds = Kahuna.getInstance().getUserCredentials();
        newCreds.add(kahunaKey, id);
        try {
            Kahuna.getInstance().login(newCreds);
        } catch (EmptyCredentialsException e) {
            Log.e("mParticle SDK", "Kahuna is unable to login the user: " + e.getMessage());
        }
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
        Kahuna.getInstance().logout();
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
        Kahuna.getInstance().stop();
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int activityCount) {
        update();
        Kahuna.getInstance().start();
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

        Map<String, String> eventAttributes = event.getCustomAttributes();
        int eventType = CommerceEventUtil.getEventType(event);
        Integer count = null;
        Integer value = null;
        if (eventType == Constants.Commerce.EVENT_TYPE_PURCHASE) {
            int quantity = 0;
            int revenue = 0;
            List<Product> products = event.getProducts();
            if (products != null) {
                for (Product product : products) {
                    quantity += product.getQuantity();
                }
            }
            TransactionAttributes transactionAttributes = event.getTransactionAttributes();
            if (transactionAttributes != null) {
                Double transRevenue = transactionAttributes.getRevenue();
                if (transRevenue != null) {
                    revenue = (int) (transRevenue.doubleValue() * 100);
                }
            }
            count = quantity;
            value = revenue;
        }
        trackKahunaEvent(eventName, count, value, event.getCustomAttributes());
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event).setEventName(eventName).setAttributes(eventAttributes));
        return messages;
    }

    private void trackKahunaEvent(String name, Integer count, Integer value, Map<String, String> eventAttributes) {
        EventBuilder eb = new EventBuilder(name);
        if(eventAttributes != null && eventAttributes.size() > 0) {
            for(Map.Entry<String, String> entry : eventAttributes.entrySet()) {
                eb.addProperty(entry.getKey(), entry.getValue());
            }
        }
        if(count != null && value != null) {
            eb.setPurchaseData(count.intValue(), value.intValue());
        }
        Kahuna.getInstance().track(eb.build());
    }
}