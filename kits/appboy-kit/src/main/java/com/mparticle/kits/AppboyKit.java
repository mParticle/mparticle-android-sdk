package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.appboy.Appboy;
import com.appboy.AppboyGcmReceiver;
import com.appboy.AppboyUser;
import com.appboy.enums.Gender;
import com.appboy.models.outgoing.AppboyProperties;
import com.appboy.push.AppboyNotificationUtils;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticle.UserAttributes;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.messaging.MessagingConfigCallbacks;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Embedded version of the AppBoy SDK v 1.12.0
 */
public class AppboyKit extends KitIntegration implements KitIntegration.PushListener, KitIntegration.EventListener, KitIntegration.CommerceListener, KitIntegration.AttributeListener, KitIntegration.ActivityListener {
    static final String APPBOY_KEY = "apiKey";

    @Override
    public Object getInstance() {
        return Appboy.getInstance(getContext());
    }

    @Override
    public String getName() {
        return "Appboy";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        String key = getSettings().get(APPBOY_KEY);
        Appboy.configure(getContext(), key);
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) {
        return null;
    }

    @Override
    public List<ReportingMessage> logError(String message, Map<String, String> errorAttributes) {
        return null;
    }

    @Override
    public List<ReportingMessage> logException(Exception exception, Map<String, String> exceptionAttributes, String message) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        if (event.getInfo() == null) {
            Appboy.getInstance(getContext()).logCustomEvent(event.getEventName());
        } else {
            AppboyProperties properties = new AppboyProperties();
            for (Map.Entry<String, String> entry : event.getInfo().entrySet()) {
                properties.addProperty(entry.getKey(), entry.getValue());
            }
            Appboy.getInstance(getContext()).logCustomEvent(event.getEventName(), properties);
        }
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) {
        return null;
    }


    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
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
    public boolean isCommerceSupported() {
        return true;
    }

    @Override
    public void removeUserAttribute(String key) {
        AppboyUser user = Appboy.getInstance(getContext()).getCurrentUser();

        if (UserAttributes.CITY.equals(key)) {
            user.setHomeCity(null);
        } else if (UserAttributes.COUNTRY.equals(key)) {
            user.setCountry(null);
        } else if (UserAttributes.FIRSTNAME.equals(key)) {
            user.setFirstName(null);
        } else if (UserAttributes.GENDER.equals(key)) {
            user.setGender(null);
        } else if (UserAttributes.LASTNAME.equals(key)) {
            user.setLastName(null);
        } else if (UserAttributes.MOBILE_NUMBER.equals(key)) {
            user.setPhoneNumber(null);
        } else {
            user.unsetCustomUserAttribute(KitUtils.sanitizeAttributeKey(key));
        }
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String id) {
        AppboyUser user = Appboy.getInstance(getContext()).getCurrentUser();
        if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            if (user == null || (user.getUserId() != null && !user.getUserId().equals(id))) {
                Appboy.getInstance(getContext()).changeUser(id);
            }
        } else if (MParticle.IdentityType.Email.equals(identityType)) {
            user.setEmail(id);
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        //appboy recommends never un-setting identities, until there's a new identity to set.
    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    @Override
    public void setUserAttribute(String key, String value) {
        AppboyUser user = Appboy.getInstance(getContext()).getCurrentUser();
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
            user.setCustomUserAttribute(KitUtils.sanitizeAttributeKey(key), value);
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
        Appboy.getInstance(getContext()).logPurchase(
                product.getSku(),
                currency,
                new BigDecimal(product.getUnitPrice()),
                (int) product.getQuantity(),
                purchaseProperties
        );
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity) {
       return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity) {
        Appboy.getInstance(activity).closeSession(activity);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    @Override
    public List<ReportingMessage> onActivitySaveInstanceState(Activity activity, Bundle outState) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityDestroyed(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle savedInstanceState) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity) {
        Appboy.getInstance(activity).openSession(activity);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null)
        );
        return messageList;
    }

    public Appboy getAppboy() {
        return Appboy.getInstance(getContext());
    }

    @Override
    public boolean willHandleMessage(Set<String> keyset) {
        return keyset.contains(com.appboy.Constants.APPBOY_PUSH_APPBOY_KEY);
    }

    @Override
    public void onMessageReceived(Context context, Intent pushIntent) {
        new AppboyGcmReceiver().onReceive(getContext(), pushIntent);
    }

    @Override
    public boolean onPushRegistration(String token, String senderId) {
        Appboy.getInstance(getContext()).registerAppboyGcmMessages(token);
        return true;
    }
}
