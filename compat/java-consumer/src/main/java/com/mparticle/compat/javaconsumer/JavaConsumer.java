/*
 * Compile-only fixture. Exercises the mParticle public API from Java, the way an integrating app
 * does, against the published android-core and android-kit-base artifacts rather than the modules
 * in this repository. Nothing here runs; the build fails when the shipped API stops compiling for
 * a Java consumer.
 */
package com.mparticle.compat.javaconsumer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mparticle.AttributionError;
import com.mparticle.AttributionListener;
import com.mparticle.AttributionResult;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.MParticleTask;
import com.mparticle.SdkListener;
import com.mparticle.WrapperSdk;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.consent.CCPAConsent;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.MParticleUser;
import com.mparticle.rokt.RoktSession;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JavaConsumer {

    private JavaConsumer() {
    }

    public static void start(@NonNull Context context) {
        IdentityApiRequest identifyRequest = IdentityApiRequest.withEmptyUser()
                .email("consumer@example.com")
                .customerId(null)
                .userIdentity(MParticle.IdentityType.Other, null)
                .build();
        MParticleOptions options = MParticleOptions.builder(context)
                .credentials("api-key", "api-secret")
                .environment(MParticle.Environment.Development)
                .installType(MParticle.InstallType.AutoDetect)
                .logLevel(MParticle.LogLevel.DEBUG)
                .identify(identifyRequest)
                .attributionListener(new FixtureAttributionListener())
                .dataplan("plan", null)
                .build();
        MParticle.addListener(context, new FixtureSdkListener());
        MParticle.start(options);
    }

    public static void logEvents() {
        MParticle instance = MParticle.getInstance();
        if (instance == null) {
            return;
        }
        instance.setWrapperSdk(WrapperSdk.WrapperFlutter, "1.0.0");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("plan", "premium");
        MPEvent event = new MPEvent.Builder("Signup", MParticle.EventType.UserPreference)
                .customAttributes(attributes)
                .category(null)
                .addCustomFlag("flag", null)
                .duration(120d)
                .build();
        instance.logEvent(event);
        instance.logScreen("Home", null);
        instance.leaveBreadcrumb(event.getEventName());

        Product product = new Product.Builder("Widget", "SKU-1", 9.99)
                .quantity(2)
                .brand(null)
                .category("Gadgets")
                .build();
        TransactionAttributes transaction = new TransactionAttributes("order-1")
                .setRevenue(product.getTotalAmount())
                .setTax(null)
                .setShipping(null);
        CommerceEvent purchase = new CommerceEvent.Builder(Product.PURCHASE, product)
                .transactionAttributes(transaction)
                .currency("USD")
                .screen(null)
                .build();
        instance.logEvent(purchase);

        ConsentState consent = ConsentState.builder()
                .addGDPRConsentState("marketing", GDPRConsent.builder(true).document("v1").timestamp(null).build())
                .setCCPAConsentState(CCPAConsent.builder(false).location(null).build())
                .build();
        instance.setDeviceConsentState(consent);
        instance.setDeviceConsentState(null);
    }

    public static void identify() {
        MParticle instance = MParticle.getInstance();
        if (instance == null) {
            return;
        }
        MParticleUser currentUser = instance.Identity().getCurrentUser();
        IdentityApiRequest request = IdentityApiRequest.withUser(currentUser)
                .email("consumer@example.com")
                .build();
        MParticleTask<IdentityApiResult> task = instance.Identity().login(request);
        task.addSuccessListener(result -> result.getUser().getId())
                .addFailureListener(response -> {
                    if (response != null) {
                        response.getHttpCode();
                    }
                });
        instance.Identity().login(null);
        if (currentUser != null) {
            currentUser.setConsentState(null);
        }

        RoktSession session = new RoktSession("session-id", "session-token");
        session.getSessionId();
        session.getExpiresAt();
    }

    /** {@code IdentityHttpResponse(int, JSONObject)} is the one public constructor that declares a checked exception. */
    @Nullable
    public static IdentityHttpResponse parseIdentityResponse(@NonNull JSONObject body) {
        try {
            return new IdentityHttpResponse(200, body);
        } catch (JSONException e) {
            return null;
        }
    }

    private static final class FixtureAttributionListener implements AttributionListener {
        @Override
        public void onResult(@NonNull AttributionResult result) {
            result.getServiceProviderId();
        }

        @Override
        public void onError(@NonNull AttributionError error) {
            error.getMessage();
        }
    }

    private static final class FixtureSdkListener extends SdkListener {
        @Override
        public void onApiCalled(@NonNull String apiName, @NonNull List<Object> objects, boolean isExternal) {
        }

        @Override
        public void onKitStarted(int kitId) {
        }
    }
}
