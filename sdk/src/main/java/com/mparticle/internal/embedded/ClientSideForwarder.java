package com.mparticle.internal.embedded;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;

import java.util.List;
import java.util.Map;

interface ClientSideForwarder {
    List<ReportingMessage> logEvent(MPEvent event) throws Exception;
    List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception;
    @Deprecated
    List<ReportingMessage> logTransaction(MPProduct transaction) throws Exception;

}
