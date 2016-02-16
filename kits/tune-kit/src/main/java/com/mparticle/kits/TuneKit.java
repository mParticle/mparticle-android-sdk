package com.mparticle.kits;

import android.app.Activity;
import android.text.TextUtils;

import com.mparticle.kits.mobileapptracker.MATDeeplinkListener;
import com.mparticle.kits.mobileapptracker.MATDeferredDplinkr;
import com.mparticle.kits.mobileapptracker.MATUrlRequester;
import com.mparticle.kits.mobileapptracker.MATUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tune Kit implementing Tune's post-install deep-link feature. Different from other Kits, the Tune Kit
 * does not actually wrap the full Tune SDK - only a small subset of classes required to query the Tune server
 * for deep links that match the given user.
 */
public class TuneKit extends AbstractKit implements MATDeeplinkListener {

    private static final String SETTING_ADVERTISER_ID = "advertiserId";
    private static final String SETTING_CONVERSION_KEY = "conversionKey";
    private static final String SETTING_PACKAGE_NAME_OVERRIDE = "overridePackageName";
    public String settingAdvertiserId = null;
    public String settingConversionKey = null;
    public String packageName = null;
    private MATDeferredDplinkr deepLinker;
    private AtomicBoolean listenerWaiting = new AtomicBoolean(false);

    @Override
    public Object getInstance(Activity activity) {
        return null;
    }

    @Override
    public String getName() {
        return "Tune";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri.contains("mobileapptracking.com");
    }

    @Override
    protected AbstractKit update() {
        if (MATUtils.firstInstall(context)) {
            settingAdvertiserId = properties.get(SETTING_ADVERTISER_ID);
            settingConversionKey = properties.get(SETTING_CONVERSION_KEY);
            packageName = properties.get(SETTING_PACKAGE_NAME_OVERRIDE);
            if (TextUtils.isEmpty(packageName)) {
                packageName = context.getPackageName();
            }
            deepLinker = MATDeferredDplinkr.initialize(settingAdvertiserId, settingConversionKey, packageName);
            deepLinker.setListener(this);
        }
        return this;
    }

    public void setUserAgent(String userAgent) {
        if (deepLinker != null) {
            deepLinker.setUserAgent(userAgent);
            if (listenerWaiting.get()) {
                checkForDeepLink();
            }
        }
    }

    @Override
    public void checkForDeepLink() {
        if (deepLinker != null) {
            listenerWaiting.set(true);
            if (deepLinker.getUserAgent() == null) {
                MATUtils.calculateUserAgent(context, this);
            } else {
                deepLinker.checkForDeferredDeeplink(context, new MATUrlRequester());
            }
        }
    }

    @Override
    public void didReceiveDeeplink(String deeplink) {
        listenerWaiting.set(false);
        DeepLinkResult result = new DeepLinkResult()
                .setLink(deeplink)
                .setServiceProviderId(this.getKitId());
        mEkManager.onResult(result);
    }

    @Override
    public void didFailDeeplink(String error) {
        listenerWaiting.set(false);
        DeepLinkError deepLinkError = new DeepLinkError()
                .setMessage(error)
                .setServiceProviderId(this.getKitId());
        mEkManager.onError(deepLinkError);
    }
}
