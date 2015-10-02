package com.mparticle.kits;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.appboy.Appboy;
import com.appboy.AppboyGcmReceiver;
import com.appboy.AppboyUser;
import com.appboy.enums.Gender;
import com.appboy.models.outgoing.AppboyProperties;
import com.appboy.push.AppboyNotificationUtils;
import com.mparticle.Constants;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.MParticle.UserAttributes;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.messaging.MessagingConfigCallbacks;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Embedded version of the AppBoy SDK v 1.7.2
 */
public class AppboyKit extends AbstractKit implements ActivityLifecycleForwarder, PushProvider, MessagingConfigCallbacks, ClientSideForwarder, ECommerceForwarder {
    static final String APPBOY_KEY = "apiKey";
    public static final String PUSH_ENABLED = "push_enabled";
    boolean started = false;
    boolean running = false;
    private boolean pushEnabled;

    @Override
    public String getName() {
        return "Appboy";
    }

    @Override
    public boolean isOriginator(String uri) {
        return false;
    }

    @Override
    protected AbstractKit update() {
        String key = properties.get(APPBOY_KEY);
        if (!running && !TextUtils.isEmpty(key)) {
            Appboy.configure(context, key);
            running = true;
        }
        if (running) {
            pushEnabled = Boolean.parseBoolean(properties.get(PUSH_ENABLED));
            if (pushEnabled) {
                String regId = PushRegistrationHelper.getRegistrationId(context);
                if (TextUtils.isEmpty(regId)) {
                    PushRegistrationHelper.enablePushNotifications(context, mEkManager.getConfigurationManager().getPushSenderId(), this);
                } else {
                    setPushRegistrationId(regId);
                }
            }
        }
        return this;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) throws Exception {
        if (event.getInfo() == null) {
            Appboy.getInstance(context).logCustomEvent(event.getEventName());
        } else {
            AppboyProperties properties = new AppboyProperties();
            for (Map.Entry<String, String> entry : event.getInfo().entrySet()) {
                properties.addProperty(entry.getKey(), entry.getValue());
            }
            Appboy.getInstance(context).logCustomEvent(event.getEventName(), properties);
        }
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        return null;
    }


    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) throws Exception {
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        if (!TextUtils.isEmpty(event.getProductAction()) &&
                event.getProductAction().equalsIgnoreCase(Product.PURCHASE) &&
                event.getProducts().size() > 0) {
            List<Product> productList = event.getProducts();
            for (Product product : productList) {
                logTransaction(event, product);
            }
            messages.add(ReportingMessage.fromEvent(this, event));
            return messages;
        }
        List<MPEvent> eventList = CommerceEventUtil.expand(event);
        if (eventList != null) {
            for (int i = 0; i < eventList.size(); i++) {
                try {
                    logEvent(eventList.get(i));
                    messages.add(ReportingMessage.fromEvent(this, event));
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logCustomEvent to Appboy kit: " + e.toString());
                }
            }
        }
        return messages;
    }

    @Override
    void removeUserAttribute(String key) {
        Appboy.getInstance(context).getCurrentUser().unsetCustomUserAttribute(
                key
        );
    }

    @Override
    void setUserIdentity(String id, MParticle.IdentityType identityType) {
        AppboyUser user = Appboy.getInstance(context).getCurrentUser();
        if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            if (user == null || (user.getUserId() != null && !user.getUserId().equals(id))) {
                Appboy.getInstance(context).changeUser(id);
            }
        } else if (MParticle.IdentityType.Email.equals(identityType)) {
            user.setEmail(id);
        }
    }

    @Override
    void setUserAttributes(JSONObject attributes) {
        AppboyUser user = Appboy.getInstance(context).getCurrentUser();

        if (attributes != null) {
            Iterator<String> keys = attributes.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = attributes.optString(key, "");
                if (UserAttributes.CITY.equals(key)) {
                    user.setHomeCity(value);
                } else if (UserAttributes.COUNTRY.equals(key)) {
                    user.setCountry(value);
                } else if (UserAttributes.FIRSTNAME.equals(key)) {
                    user.setFirstName(value);
                } else if (UserAttributes.GENDER.equals(key)) {
                    if (value.contains("fe")) {
                        user.setGender(Gender.FEMALE);
                    } else {
                        user.setGender(Gender.MALE);
                    }
                } else if (UserAttributes.LASTNAME.equals(key)) {
                    user.setLastName(value);
                } else if (UserAttributes.MOBILE_NUMBER.equals(key)) {
                    user.setPhoneNumber(value);
                } else {
                    if (key.startsWith("$")) {
                        key = key.substring(1);
                    }
                    user.setCustomUserAttribute(key, value);
                }
            }
        }
    }

    private void logTransaction(CommerceEvent event, Product product) {
        AppboyProperties purchaseProperties = new AppboyProperties();
        Map<String, String> eventAttributes = new HashMap<String, String>();
        CommerceEventUtil.extractActionAttributes(event, eventAttributes);

        String currency = eventAttributes.get(Constants.Commerce.ATT_ACTION_CURRENCY_CODE);
        if (TextUtils.isEmpty(currency)) {
            currency = Constants.Commerce.DEFAULT_CURRENCY_CODE;
        }
        eventAttributes.remove(Constants.Commerce.ATT_ACTION_CURRENCY_CODE);
        for (Map.Entry<String, String> entry : eventAttributes.entrySet()) {
            purchaseProperties.addProperty(entry.getKey(), entry.getValue());
        }
        Appboy.getInstance(context).logPurchase(
                product.getSku(),
                currency,
                new BigDecimal(product.getUnitPrice()),
                (int) product.getQuantity(),
                purchaseProperties
        );
    }

    @Override
    public List<ReportingMessage> logTransaction(MPProduct product) throws Exception {
        AppboyProperties purchaseProperties = new AppboyProperties();
        for (Map.Entry<String, String> entry : product.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(MPProduct.SKU) &&
                    !key.equals(MPProduct.CURRENCY) &&
                    !key.equals(MPProduct.TOTALAMOUNT) &&
                    !key.equals(MPProduct.QUANTITY) &&
                    !key.equals("$MethodName")) {
                if (!TextUtils.isEmpty(key)) {
                    if (key.startsWith("$")) {
                        key = key.substring(1);
                    }
                    purchaseProperties.addProperty(key, entry.getValue());
                }
            }
        }
        Appboy.getInstance(context).logPurchase(
                product.get(MPProduct.SKU),
                product.getCurrencyCode(),
                new BigDecimal(product.getTotalAmount()),
                (int) product.getQuantity(),
                purchaseProperties
        );
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromTransaction(this, product));
        return messages;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity, int activityCount) {
        if (!started) {
            onActivityStarted(activity, activityCount);
            List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
            messageList.add(
                    new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
            );
            return messageList;
        }
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity, int activityCount) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity, int activityCount) {
        if (started) {
            Appboy.getInstance(activity).closeSession(activity);
            started = false;
            List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
            messageList.add(
                    new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
            );
            return messageList;
        }
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int activityCount) {
        started = true;
        Appboy.getInstance(activity).openSession(activity);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> handleGcmMessage(Intent intent) {
        if (AppboyNotificationUtils.isAppboyPushMessage(intent)) {
            new AppboyGcmReceiver().onReceive(context, intent);
            List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
            messages.add(ReportingMessage.fromPushMessage(this, intent));
            return messages;
        }
        return null;
    }

    @Override
    public void setPushNotificationIcon(int resId) {

    }

    @Override
    public void setPushNotificationTitle(int resId) {

    }

    @Override
    public void setPushSenderId(String senderId) {

    }

    @Override
    public void setPushSoundEnabled(boolean enabled) {

    }

    @Override
    public void setPushVibrationEnabled(boolean enabled) {

    }

    @Override
    public void setPushRegistrationId(String registrationId) {
        Appboy.getInstance(context).registerAppboyGcmMessages(registrationId);
    }

    public Appboy getAppboy() {
        return Appboy.getInstance(context);
    }
}
