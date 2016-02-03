package com.mparticle.kits;

import android.app.Activity;
import android.app.Application;
import android.text.TextUtils;

import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerProperties;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Embedded version of the AppsFlyer SDK v 4.3.0
 */
public class AppsFlyerKit extends AbstractKit implements ECommerceForwarder, AppsFlyerConversionListener {

    private static final String DEV_KEY = "devKey";

    @Override
    public Object getInstance(Activity activity) {
        return null;
    }

    @Override
    public String getName() {
        return "AppsFlyer";
    }

    @Override
    public boolean isOriginator(String uri) {
        return false;
    }

    @Override
    protected AbstractKit update() {
        AppsFlyerLib.getInstance().startTracking((Application) context.getApplicationContext(), properties.get(DEV_KEY));
        AppsFlyerLib.getInstance().setDebugLog(MParticle.getInstance().getEnvironment() == MParticle.Environment.Development);
        return this;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) throws Exception {
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();

        if (event.getProductAction().equals(Product.ADD_TO_CART)
                || event.getProductAction().equals(Product.ADD_TO_WISHLIST)
                || event.getProductAction().equals(Product.CHECKOUT)
                || event.getProductAction().equals(Product.PURCHASE)) {
            Map<String, Object> eventValues = new HashMap<String, Object>();

            if (!TextUtils.isEmpty(event.getCurrency())) {
                eventValues.put(AFInAppEventParameterName.CURRENCY, event.getCurrency());
            }
            if (event.getProductAction().equals(Product.ADD_TO_CART)
                    || event.getProductAction().equals(Product.ADD_TO_WISHLIST)) {
                String eventName = event.getProductAction().equals(Product.ADD_TO_CART) ? AFInAppEventType.ADD_TO_CART : AFInAppEventType.ADD_TO_WISH_LIST;
                if (event.getProducts().size() > 0) {
                    List<Product> productList = event.getProducts();
                    for (Product product : productList) {
                        Map<String, Object> productEventValues = new HashMap<String, Object>();
                        productEventValues.putAll(eventValues);
                        productEventValues.put(AFInAppEventParameterName.PRICE, product.getUnitPrice());
                        productEventValues.put(AFInAppEventParameterName.QUANTITY, product.getQuantity());
                        if (!TextUtils.isEmpty(product.getSku())) {
                            productEventValues.put(AFInAppEventParameterName.CONTENT_ID, product.getSku());
                        }
                        if (!TextUtils.isEmpty(product.getCategory())) {
                            productEventValues.put(AFInAppEventParameterName.CONTENT_TYPE, product.getCategory());
                        }
                        AppsFlyerLib.getInstance().trackEvent(context, eventName, productEventValues);
                        messages.add(ReportingMessage.fromEvent(this, event));
                    }
                }
            } else {
                String eventName = event.getProductAction().equals(Product.CHECKOUT) ? AFInAppEventType.INITIATED_CHECKOUT : AFInAppEventType.PURCHASE;
                if (event.getProducts() != null && event.getProducts().size() > 0) {
                    double totalQuantity = 0;
                    for (Product product : event.getProducts()) {
                        totalQuantity += product.getQuantity();
                    }
                    eventValues.put(AFInAppEventParameterName.QUANTITY, totalQuantity);
                }
                if (event.getTransactionAttributes() != null && event.getTransactionAttributes().getRevenue() != 0) {
                    String paramName = event.getProductAction().equals(Product.PURCHASE) ? AFInAppEventParameterName.REVENUE : AFInAppEventParameterName.PRICE;
                    eventValues.put(paramName, event.getTransactionAttributes().getRevenue());
                }
                AppsFlyerLib.getInstance().trackEvent(context, eventName, eventValues);
                messages.add(ReportingMessage.fromEvent(this, event));
            }
        } else {
            List<MPEvent> eventList = CommerceEventUtil.expand(event);
            if (eventList != null) {
                for (int i = 0; i < eventList.size(); i++) {
                    try {
                        logEvent(eventList.get(i));
                        messages.add(ReportingMessage.fromEvent(this, event));
                    } catch (Exception e) {
                        ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logCustomEvent to AppsFlyer kit: " + e.toString());
                    }
                }
            }
        }

        return messages;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) throws Exception {
        AppsFlyerLib.getInstance().trackEvent(context, event.getEventName(), new HashMap<String, Object>(event.getInfo()));
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        return null;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
        return null;
    }

    @Override
    public void checkForDeepLink() {
        AppsFlyerLib.getInstance().registerConversionListener(context, this);
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        AppsFlyerLib.getInstance().setDeviceTrackingDisabled(optOutStatus);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null)
                        .setOptOut(optOutStatus)
        );
        return messageList;
    }


    @Override
    void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            AppsFlyerLib.getInstance().setCustomerUserId(id);
        } else if (MParticle.IdentityType.Email.equals(identityType)) {
            AppsFlyerLib.getInstance().setUserEmails(AppsFlyerProperties.EmailsCryptType.NONE, id);
        }
    }

    @Override
    public void onInstallConversionDataLoaded(Map<String, String> map) {
        if (map != null) {
            JSONObject jsonResult = new JSONObject();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                try {
                    jsonResult.put(entry.getKey(), entry.getValue());
                } catch (JSONException e) {
                }
            }
            DeepLinkResult result = new DeepLinkResult()
                    .setParameters(jsonResult)
                    .setServiceProviderId(this.getKitId());
            mEkManager.onResult(result);
        }
    }

    @Override
    public void onInstallConversionFailure(String s) {
        if (!TextUtils.isEmpty(s)) {
            DeepLinkError error = new DeepLinkError()
                    .setMessage(s)
                    .setServiceProviderId(this.getKitId());
            mEkManager.onError(error);
        }
    }

    @Override
    public void onAppOpenAttribution(Map<String, String> map) {
        onInstallConversionDataLoaded(map);
    }

    @Override
    public void onAttributionFailure(String s) {
        onInstallConversionFailure(s);
    }
}
