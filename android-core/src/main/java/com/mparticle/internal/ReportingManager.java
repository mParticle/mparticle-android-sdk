package com.mparticle.internal;

import java.util.List;


public interface ReportingManager {
    void log(ReportingMessage message);
    void logAll(List<? extends ReportingMessage> messageList);
}
