package com.mparticle.internal.embedded;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;

import com.appboy.Appboy;
import com.appboy.AppboyUser;
import com.appboy.IAppboyNotificationFactory;
import com.appboy.enums.Gender;
import com.appboy.models.outgoing.AppboyProperties;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.MParticle.UserAttributes;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPActivityCallbacks;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.internal.embedded.appboy.AppboyGcmReceiver;
import com.mparticle.internal.embedded.appboy.push.AppboyNotificationUtils;
import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.MessagingConfigCallbacks;
import com.mparticle.messaging.ProviderCloudMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;


/**
 * Embedded version of the AppBoy SDK v 1.7.2
 */
public class EmbeddedAppboy extends EmbeddedProvider implements MPActivityCallbacks, PushProvider, MessagingConfigCallbacks {
    static final String APPBOY_KEY = "apiKey";
    public static final String PUSH_ENABLED = "push_enabled";
    public static final String REGISTER_INAPP = "register_inapp";
    boolean started = false;
    boolean running = false;
    public String senderId = null;
    private boolean pushEnabled;

    public EmbeddedAppboy(EmbeddedKitManager ekManager) {
        super(ekManager);
    }

    @Override
    public String getName() {
        return "Appboy";
    }

    @Override
    public boolean isOriginator(String uri) {
        return false;
    }

    @Override
    protected EmbeddedProvider update() {
        String key =  properties.get(APPBOY_KEY);
        if (!running && !MPUtility.isEmpty(key)) {
            Appboy.configure(context, key);
            running = true;
        }
        if (running) {
            pushEnabled = Boolean.parseBoolean(properties.get(PUSH_ENABLED));
            if (pushEnabled) {
                String regId = PushRegistrationHelper.getRegistrationId(context);
                if (MPUtility.isEmpty(regId)) {
                    PushRegistrationHelper.enablePushNotifications(context, mEkManager.getConfigurationManager().getPushSenderId(), this);
                }else{
                    setPushRegistrationId(regId);
                }
            }
            mEkManager.onProviderActive(Appboy.getInstance(context));
        }
        return this;
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {

    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void logEvent(MPEvent event, Map<String, String> attributes) throws Exception {
        if (attributes == null) {
            Appboy.getInstance(context).logCustomEvent(event.getEventName());
        }else{
            AppboyProperties properties = new AppboyProperties();
            for (Map.Entry<String, String> entry : attributes.entrySet()){
                properties.addProperty(entry.getKey(), entry.getValue());
            }
            Appboy.getInstance(context).logCustomEvent(event.getEventName(), properties);
        }
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
        if (MParticle.IdentityType.CustomerId.equals(identityType)){
            if (user == null || (user.getUserId() != null && !user.getUserId().equals(id))) {
                Appboy.getInstance(context).changeUser(id);
            }
        }else if (MParticle.IdentityType.Email.equals(identityType)){
            user.setEmail(id);
        }
    }

    @Override
    void setUserAttributes(JSONObject attributes) {
        AppboyUser user = Appboy.getInstance(context).getCurrentUser();

        if (attributes != null){
            Iterator<String> keys = attributes.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = attributes.optString(key, "");
                if (UserAttributes.CITY.equals(key)){
                    user.setHomeCity(value);
                } else if (UserAttributes.COUNTRY.equals(key)){
                    user.setCountry(value);
                } else if (UserAttributes.FIRSTNAME.equals(key)){
                    user.setFirstName(value);
                } else if (UserAttributes.GENDER.equals(key)){
                    if (value.contains("fe")){
                        user.setGender(Gender.FEMALE);
                    }else{
                        user.setGender(Gender.MALE);
                    }
                } else if (UserAttributes.LASTNAME.equals(key)){
                    user.setLastName(value);
                } else if (UserAttributes.MOBILE_NUMBER.equals(key)) {
                    user.setPhoneNumber(value);
                } else{
                    if (key.startsWith("$")){
                        key = key.substring(1);
                    }
                    user.setCustomUserAttribute(key, value);
                }
            }
        }
    }

    @Override
    public void logTransaction(MPProduct product) throws Exception {
        AppboyProperties purchaseProperties = new AppboyProperties();
        for (Map.Entry<String, String> entry : product.entrySet()){
            String key = entry.getKey();
            if (!key.equals(MPProduct.SKU) &&
                    !key.equals(MPProduct.CURRENCY) &&
                    !key.equals(MPProduct.TOTALAMOUNT) &&
                    !key.equals(MPProduct.QUANTITY) &&
                    !key.equals(Constants.MethodName.METHOD_NAME)){
                if (!MPUtility.isEmpty(key)) {
                    if (key.startsWith("$")){
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
                (int)product.getQuantity(),
                purchaseProperties
        );
    }

    @Override
    public void onActivityResumed(Activity activity, int activityCount) {
        if (!started){
            onActivityStarted(activity, activityCount);
        }
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityStopped(Activity activity, int activityCount) {
        if (started) {
            Appboy.getInstance(activity).closeSession(activity);
            started = false;
        }
    }

    @Override
    public void onActivityStarted(Activity activity, int activityCount) {
        started = true;
        Appboy.getInstance(activity).openSession(activity);
        Appboy.getInstance(context).requestImmediateDataFlush();
    }

    @Override
    public void onRegistered(String registrationId) {
        Appboy.getInstance(context).registerAppboyGcmMessages(registrationId);
    }

    @Override
    public boolean handleGcmMessage(Intent intent) {
        if (AppboyNotificationUtils.isAppboyPushMessage(intent)) {
            new AppboyGcmReceiver().onReceive(context, intent);
            IntentFilter intentFilter = new IntentFilter(context.getPackageName() + ".intent.APPBOY_NOTIFICATION_OPENED");
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        String campaignId = intent.getStringExtra(AppboyGcmReceiver.CAMPAIGN_ID_KEY);
                        if (!MPUtility.isEmpty(campaignId)) {
                            Appboy.getInstance(context).logPushNotificationOpened(campaignId);
                        }
                    }catch (Exception e){

                    }
                }
            }, intentFilter);
            return true;
        }
        return false;
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
        Appboy.getInstance(context).requestImmediateDataFlush();
    }

    public Appboy getAppboy() {
        return Appboy.getInstance(context);
    }
}
