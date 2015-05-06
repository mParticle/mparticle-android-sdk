package com.mparticle.internal.embedded;

import android.app.Activity;

import com.appboy.Appboy;
import com.appboy.AppboyUser;
import com.appboy.enums.Gender;
import com.appboy.models.outgoing.AppboyProperties;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.MParticle.UserAttributes;
import com.mparticle.internal.MPActivityCallbacks;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

/**
 * Embedded version of the AppBoy SDK v 1.7.1
 */
public class EmbeddedAppboy extends EmbeddedProvider implements MPActivityCallbacks {
    private final String APPBOY_KEY = "appboy_key";
    boolean started = false;

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
        Appboy.configure(context, properties.get(APPBOY_KEY));
        return this;
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {

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
        }else {
            user.setCustomUserAttribute(identityType.toString(), id);
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
                    !key.equals(MPProduct.QUANTITY)){
                purchaseProperties.addProperty(key, entry.getValue());
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
    }
}
