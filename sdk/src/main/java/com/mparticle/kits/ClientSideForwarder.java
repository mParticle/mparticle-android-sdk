package com.mparticle.kits;

import com.mparticle.MPEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ClientSideForwarder {
    List<ReportingMessage> logEvent(MPEvent event) throws Exception;
    List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) throws Exception;
    List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo);
}
