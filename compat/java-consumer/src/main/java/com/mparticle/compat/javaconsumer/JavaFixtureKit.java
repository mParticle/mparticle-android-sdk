/*
 * Compile-only fixture. A kit written the way third-party integrations are, in Java, against the
 * published android-kit-base. It implements every listener interface a kit can opt into.
 */
package com.mparticle.compat.javaconsumer;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mparticle.MPEvent;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.kits.CommerceEventUtils;
import com.mparticle.kits.FilteredIdentityApiRequest;
import com.mparticle.kits.KitConfiguration;
import com.mparticle.kits.KitIntegration;
import com.mparticle.kits.KitUtils;
import com.mparticle.kits.ReportingMessage;
import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class JavaFixtureKit extends KitIntegration implements
        KitIntegration.EventListener,
        KitIntegration.CommerceListener,
        KitIntegration.IdentityListener,
        KitIntegration.UserAttributeListener,
        KitIntegration.PushListener,
        KitIntegration.ApplicationStateListener {

    /** {@code createKitConfiguration} is the kit-base entry point that declares a checked exception. */
    @Nullable
    static KitConfiguration parseConfiguration(@NonNull JSONObject json) {
        try {
            return KitConfiguration.createKitConfiguration(json);
        } catch (JSONException e) {
            Logger.error(e, "invalid kit configuration");
            return null;
        }
    }

    @Override
    public String getName() {
        return "JavaFixtureKit";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        if (MPUtility.isEmpty(settings) || KitUtils.isEmpty(settings.get("apiKey"))) {
            throw new IllegalArgumentException("apiKey is required");
        }
        Logger.debug("JavaFixtureKit " + getConfiguration().getKitId() + " created on " + getKitManager().getClass().getSimpleName());
        return Collections.emptyList();
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return Collections.emptyList();
    }

    // EventListener

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
        return Collections.singletonList(ReportingMessage.fromEvent(this, event));
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> screenAttributes) {
        return null;
    }

    // CommerceListener

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
        List<ReportingMessage> messages = new ArrayList<>();
        for (MPEvent expanded : CommerceEventUtils.expand(event)) {
            messages.add(ReportingMessage.fromEvent(this, expanded));
        }
        return messages;
    }

    // IdentityListener

    @Override
    public void onIdentifyCompleted(MParticleUser user, FilteredIdentityApiRequest request) {
    }

    @Override
    public void onLoginCompleted(MParticleUser user, FilteredIdentityApiRequest request) {
    }

    @Override
    public void onLogoutCompleted(MParticleUser user, FilteredIdentityApiRequest request) {
    }

    @Override
    public void onModifyCompleted(MParticleUser user, FilteredIdentityApiRequest request) {
    }

    @Override
    public void onUserIdentified(MParticleUser user) {
    }

    // UserAttributeListener

    @Override
    public boolean supportsAttributeLists() {
        return true;
    }

    @Override
    public void onRemoveUserAttribute(String key) {
    }

    @Override
    public void onSetUserAttribute(String key, Object value) {
    }

    @Override
    public void onSetUserAttributeList(@Nullable String attributeKey, @Nullable List<String> attributeValueList) {
    }

    @Override
    public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists) {
    }

    @Override
    public void onIncrementUserAttribute(String key, Number incrementedBy, String value) {
    }

    @Override
    public void onSetUserTag(String key) {
    }

    @Override
    public void onConsentStateUpdated(ConsentState oldState, ConsentState newState) {
    }

    // PushListener

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        return false;
    }

    @Override
    public void onPushMessageReceived(Context context, Intent pushIntent) {
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        return false;
    }

    // ApplicationStateListener

    @Override
    public void onApplicationForeground() {
    }

    @Override
    public void onApplicationBackground() {
    }
}
