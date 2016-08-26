package com.mparticle.kits;


import android.content.Intent;

import com.mparticle.MPEvent;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.internal.JsonReportingMessage;
import com.mparticle.internal.MPUtility;
import com.mparticle.kits.mappings.CustomMapping;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Map;

public class ReportingMessage implements JsonReportingMessage {
    private final int moduleId;
    private String messageType;
    private final long timestamp;
    private Map<String, String> attributes;
    private String eventName = null;
    private String eventType = null;
    private LinkedList<ProjectionReport> projectionReports;
    private String screenName;
    private boolean devMode;
    private boolean optOut;
    private String exceptionClassName;


    public ReportingMessage(KitIntegration provider, String messageType, long timestamp, Map<String, String> attributes) {
        this.moduleId = provider.getConfiguration().getKitId();
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.attributes = attributes;
    }

    public static ReportingMessage fromPushMessage(KitIntegration provider, Intent intent) {
        return new ReportingMessage(provider, MessageType.PUSH_RECEIVED, System.currentTimeMillis(), null);
    }

    public static ReportingMessage fromPushRegistrationMessage(KitIntegration provider) {
        return new ReportingMessage(provider, MessageType.PUSH_REGISTRATION, System.currentTimeMillis(), null);
    }

    public ReportingMessage setEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public ReportingMessage setAttributes(Map<String, String> eventAttributes) {
        attributes = eventAttributes;
        return this;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventTypeString() {
        return eventType;
    }

    public void addProjectionReport(ProjectionReport report) {
        if (projectionReports == null) {
            projectionReports = new LinkedList<ProjectionReport>();
        }
        projectionReports.add(report);
    }

    public static ReportingMessage logoutMessage(KitIntegration provider) {
        return new ReportingMessage(provider,
                MessageType.PROFILE,
                System.currentTimeMillis(),
                null);
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public ReportingMessage setScreenName(String screenName) {
        this.screenName = screenName;
        return this;
    }

    public static ReportingMessage fromEvent(KitIntegration provider, MPEvent event) {
        ReportingMessage message = new ReportingMessage(provider, MessageType.EVENT, System.currentTimeMillis(), event.getInfo());
        message.eventType = event.getEventType().name();
        message.eventName = event.getEventName();
        return message;
    }

    public static ReportingMessage fromEvent(KitIntegration provider, CommerceEvent commerceEvent) {
        ReportingMessage message = new ReportingMessage(provider, MessageType.COMMERCE_EVENT, System.currentTimeMillis(), commerceEvent.getCustomAttributes());
        message.eventType = CommerceEventUtils.getEventTypeString(commerceEvent);
        return message;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("mid", moduleId);
            jsonObject.put("dt", messageType);
            jsonObject.put("ct", timestamp);
            if (projectionReports != null) {
                JSONArray reports = new JSONArray();
                for (int i = 0; i < projectionReports.size(); i++) {
                    JSONObject report = new JSONObject();
                    report.put("pid", projectionReports.get(i).projectionId);
                    report.put("dt", projectionReports.get(i).messageType);
                    report.put("name", projectionReports.get(i).eventName);
                    report.put("et", projectionReports.get(i).eventType);
                    reports.put(report);
                }
                if (reports.length() > 0) {
                    jsonObject.put("proj", reports);
                }
            }
            if (devMode && attributes != null && attributes.size() > 0) {
                JSONObject attributeJson = new JSONObject();
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    attributeJson.put(entry.getKey(), entry.getValue());
                }
                jsonObject.put("attrs", attributeJson);
            }

            if (messageType.equals(MessageType.EVENT)) {
                if (!MPUtility.isEmpty(eventName)) {
                    jsonObject.put("n", eventName);
                }
                if (!MPUtility.isEmpty(eventType)) {
                    jsonObject.put("et", eventType);
                }
            } else if (messageType.equals(MessageType.SCREEN_VIEW)) {
                if (!MPUtility.isEmpty(screenName)) {
                    jsonObject.put("n", screenName);
                }
            } else if (messageType.equals(MessageType.PUSH_REGISTRATION)) {
                jsonObject.put("pr", true);
            } else if (messageType.equals(MessageType.OPT_OUT)) {
                jsonObject.put("s", optOut);
            } else if (messageType.equals(MessageType.ERROR)) {
                jsonObject.put("c", exceptionClassName);
            }
        }catch (JSONException jse){

        }
        return jsonObject;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getModuleId() {
        return moduleId;
    }

    public ReportingMessage setOptOut(boolean optOut) {
        this.optOut = optOut;
        return this;
    }

    public void setExceptionClassName(String exceptionClassName) {
        this.exceptionClassName = exceptionClassName;
    }

    public interface MessageType {
        String SESSION_START = "ss";
        String SESSION_END = "se";
        String EVENT = "e";
        String SCREEN_VIEW = "v";
        String COMMERCE_EVENT = "cm";
        String OPT_OUT = "o";
        String ERROR = "x";
        String PUSH_REGISTRATION = "pr";
        String REQUEST_HEADER = "h";
        String FIRST_RUN = "fr";
        String APP_STATE_TRANSITION = "ast";
        String PUSH_RECEIVED = "pm";
        String BREADCRUMB = "bc";
        String NETWORK_PERFORMNACE = "npe";
        String PROFILE = "pro";
    }

    public static class ProjectionReport {


        private final int projectionId;
        private final String messageType;
        private final String eventName;
        private final String eventType;

        public ProjectionReport(int projectionId, String messageType, String eventName, String eventType) {
            this.projectionId = projectionId;
            this.messageType = messageType;
            this.eventName = eventName;
            this.eventType = eventType;
        }

        public static ProjectionReport fromEvent(int projectionId, MPEvent event) {
            return new ProjectionReport(
                    projectionId,
                    MessageType.EVENT,
                    event.getEventName(),
                    event.getEventType().name());
        }

        public static ProjectionReport fromEvent(int projectionId, CommerceEvent event) {
            return new ProjectionReport(
                    projectionId,
                    MessageType.EVENT,
                    event.getEventName(),
                    CommerceEventUtils.getEventTypeString(event));
        }

        public static ProjectionReport fromProjectionResult(CustomMapping.ProjectionResult result) {
            if (result.getMPEvent() != null) {
                return fromEvent(result.getProjectionId(), result.getMPEvent());
            } else {
                return fromEvent(result.getProjectionId(), result.getCommerceEvent());
            }
        }
    }
}
