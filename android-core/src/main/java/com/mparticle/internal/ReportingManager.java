package com.mparticle.internal;

import java.util.List;


public interface ReportingManager {
    void log(JsonReportingMessage message);

    void logAll(List<? extends JsonReportingMessage> messageList);
}
