package com.mparticle.internal;

import android.content.SharedPreferences;

import com.mparticle.MParticle;
import com.mparticle.licensing.LicenseCheckerCallback;
import com.mparticle.licensing.Policy;
import com.mparticle.licensing.ServerManagedPolicy;

import java.lang.ref.WeakReference;

/**
 * Created by sdozor on 3/9/15.
 */
public class MPLicenseCheckerCallback implements LicenseCheckerCallback {

    private final WeakReference<LicenseCheckerCallback> mClientCallback;
    private final SharedPreferences mPreference;

    public MPLicenseCheckerCallback(SharedPreferences preferences, LicenseCheckerCallback clientCallback) {
        super();
        if (clientCallback != null) {
            mClientCallback = new WeakReference<LicenseCheckerCallback>(clientCallback);
        }else{
            mClientCallback = null;
        }
        mPreference = preferences;
    }

    public void allow(int policyReason) {
        if (policyReason == Policy.LICENSED) {
            mPreference.edit().putBoolean(Constants.PrefKeys.PIRATED, false).apply();
        }
        if (mClientCallback != null) {
            LicenseCheckerCallback clientCallback = mClientCallback.get();
            if (clientCallback != null) {
                clientCallback.allow(policyReason);
            }
        }
    }

    public void dontAllow(int policyReason) {
        if (policyReason == ServerManagedPolicy.NOT_LICENSED) {
            mPreference.edit().putBoolean(Constants.PrefKeys.PIRATED, true).apply();
        }
        if (mClientCallback != null) {
            LicenseCheckerCallback clientCallback = mClientCallback.get();
            if (clientCallback != null) {
                clientCallback.dontAllow(policyReason);
            }
        }
    }

    public void applicationError(int errorCode) {
        if (errorCode == LicenseCheckerCallback.ERROR_MISSING_PERMISSION) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "License checking enabled but app is missing permission: \"com.android.vending.CHECK_LICENSE\"");
        }
        if (mClientCallback != null) {
            LicenseCheckerCallback clientCallback = mClientCallback.get();
            if (clientCallback != null) {
                clientCallback.applicationError(errorCode);
            }
        }
    }
}