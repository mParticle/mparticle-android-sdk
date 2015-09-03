package com.mparticle.internal.embedded;

import java.util.List;

/**
 * Created by sdozor on 8/31/15.
 */
public interface ReportingManager {
    void log(ReportingMessage message);
    void logAll(List<ReportingMessage> messageList);
}
