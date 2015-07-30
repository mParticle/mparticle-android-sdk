package com.mparticle.internal.embedded;

import com.mparticle.MPEvent;

import java.util.Map;

interface ClientSideForwarder {
    void logEvent(MPEvent event) throws Exception;
    void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception;
}
