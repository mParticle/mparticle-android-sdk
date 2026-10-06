package com.mparticle.integrationtests;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class EventsTest extends BaselineTest {

    @Test
    public void simpleEvent() throws Exception {
        MParticle mParticle = start();
        mParticle.logEvent(new MPEvent.Builder("Simple Event", MParticle.EventType.Other).build());
        uploadAndVerify();
    }

    @Test
    public void eventWithAttributesAndFlags() throws Exception {
        MParticle mParticle = start();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("string", "value");
        attributes.put("int", 42);
        attributes.put("long", 1700000000000L);
        attributes.put("double", 3.25);
        attributes.put("boolean", true);
        attributes.put("list", Arrays.asList("a", "b"));
        attributes.put("null", null);
        Map<String, List<String>> flags = new HashMap<>();
        flags.put("flag", Arrays.asList("one", "two"));
        mParticle.logEvent(new MPEvent.Builder("Attributed Event", MParticle.EventType.Transaction)
                .category("integration")
                .customAttributes(attributes)
                .customFlags(flags)
                .build());
        uploadAndVerify();
    }

    @Test
    public void everyEventType() throws Exception {
        MParticle mParticle = start();
        for (MParticle.EventType type : MParticle.EventType.values()) {
            if (type == MParticle.EventType.Media) {
                continue; // reserved for the media SDK
            }
            mParticle.logEvent(new MPEvent.Builder("Type " + type.name(), type).build());
        }
        uploadAndVerify();
    }

    @Test
    public void timedEvent() throws Exception {
        MParticle mParticle = start();
        mParticle.logEvent(new MPEvent.Builder("Timed Event", MParticle.EventType.Navigation)
                .duration(2000)
                .build());
        uploadAndVerify();
    }

    @Test
    public void screenViews() throws Exception {
        MParticle mParticle = start();
        mParticle.logScreen("Plain Screen");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("section", "checkout");
        mParticle.logScreen("Attributed Screen", attributes);
        mParticle.logScreen(new MPEvent.Builder("Event Screen").addCustomFlag("flag", "value").build());
        uploadAndVerify();
    }

    @Test
    public void errorsExceptionsAndBreadcrumbs() throws Exception {
        MParticle mParticle = start();
        mParticle.leaveBreadcrumb("before error");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("code", "E42");
        mParticle.logError("Plain Error");
        mParticle.logError("Attributed Error", attributes);
        mParticle.logException(new IllegalStateException("Handled Exception"), attributes, "context message");
        uploadAndVerify();
    }

    @Test
    public void sessionAttributes() throws Exception {
        MParticle mParticle = start();
        mParticle.setSessionAttribute("plan", "premium");
        mParticle.setSessionAttribute("visits", 1);
        mParticle.incrementSessionAttribute("visits", 2);
        mParticle.logEvent(new MPEvent.Builder("Session Event").build());
        uploadAndVerify();
    }

    @Test
    public void ltvIncrease() throws Exception {
        MParticle mParticle = start();
        Map<String, String> info = new HashMap<>();
        info.put("source", "integration");
        mParticle.logLtvIncrease(new java.math.BigDecimal("12.50"), "LTV Event", info);
        uploadAndVerify();
    }

    @Test
    public void pushRegistrationAndInstallReferrer() throws Exception {
        MParticle mParticle = start();
        mParticle.logPushRegistration("integration-push-token", "123456789");
        mParticle.setInstallReferrer("utm_source=integration&utm_medium=test");
        mParticle.logEvent(new MPEvent.Builder("After Referrer").build());
        uploadAndVerify();
    }

    @Test
    public void optOut() throws Exception {
        MParticle mParticle = start();
        mParticle.logEvent(new MPEvent.Builder("Before Opt Out").build());
        mParticle.setOptOut(true);
        mParticle.logEvent(new MPEvent.Builder("After Opt Out").build());
        uploadAndVerify();
    }
}
