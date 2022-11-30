package com.mparticle.kits

import android.content.Intent
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent
import com.mparticle.internal.JsonReportingMessage
import com.mparticle.internal.MPUtility
import com.mparticle.kits.ReportingMessage.ProjectionReport
import com.mparticle.kits.mappings.CustomMapping.ProjectionResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class ReportingMessage(
    provider: KitIntegration,
    messageType: String,
    timestamp: Long,
    attributes: Map<String, *>?
) : JsonReportingMessage {
    private val moduleId: Int
    private var messageType: String
    private val timestamp: Long
    private var attributes: Map<String, *>?
    var eventName: String? = null
        private set
    var eventTypeString: String? = null
        private set
    private var projectionReports: LinkedList<ProjectionReport>? = null
    private var screenName: String? = null
    private var devMode = false
    private var optOut = false
    private var exceptionClassName: String? = null
    private var mSessionId: String? = null

    init {
        moduleId = provider.configuration?.kitId!!
        this.messageType = messageType
        this.timestamp = timestamp
        this.attributes = attributes
    }

    fun setEventName(eventName: String?): ReportingMessage {
        this.eventName = eventName
        return this
    }

    fun setAttributes(eventAttributes: Map<String, *>?): ReportingMessage {
        attributes = eventAttributes
        return this
    }

    fun addProjectionReport(report: ProjectionReport) {
        if (projectionReports == null) {
            projectionReports = LinkedList()
        }
        projectionReports!!.add(report)
    }

    fun setMessageType(messageType: String) {
        this.messageType = messageType
    }

    fun setScreenName(screenName: String?): ReportingMessage {
        this.screenName = screenName
        return this
    }

    override fun setDevMode(devMode: Boolean) {
        this.devMode = devMode
    }

    override fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("mid", moduleId)
            jsonObject.put("dt", messageType)
            jsonObject.put("ct", timestamp)
            if (projectionReports != null) {
                val reports = JSONArray()
                for (i in projectionReports!!.indices) {
                    val report = JSONObject()
                    report.put("pid", projectionReports!![i].projectionId)
                    report.put("dt", projectionReports!![i].messageType)
                    report.put("name", projectionReports!![i].eventName)
                    report.put("et", projectionReports!![i].eventType)
                    reports.put(report)
                }
                if (reports.length() > 0) {
                    jsonObject.put("proj", reports)
                }
            }
            if (devMode && attributes != null && attributes!!.size > 0) {
                val attributeJson = JSONObject()
                for ((key, value) in attributes!!) {
                    attributeJson.put(key, value)
                }
                jsonObject.put("attrs", attributeJson)
            }
            if (messageType == MessageType.EVENT) {
                if (!MPUtility.isEmpty(eventName)) {
                    jsonObject.put("n", eventName)
                }
                if (!MPUtility.isEmpty(eventTypeString)) {
                    jsonObject.put("et", eventTypeString)
                }
            } else if (messageType == MessageType.SCREEN_VIEW) {
                if (!MPUtility.isEmpty(screenName)) {
                    jsonObject.put("n", screenName)
                }
            } else if (messageType == MessageType.PUSH_REGISTRATION) {
                jsonObject.put("r", true)
            } else if (messageType == MessageType.OPT_OUT) {
                jsonObject.put("s", optOut)
            } else if (messageType == MessageType.ERROR) {
                jsonObject.put("c", exceptionClassName)
            } else if (messageType == MessageType.COMMERCE_EVENT) {
                if (!MPUtility.isEmpty(eventTypeString)) {
                    jsonObject.put("et", eventTypeString)
                }
            }
        } catch (ignored: JSONException) {
        }
        return jsonObject
    }

    override fun getSessionId(): String {
        return mSessionId!!
    }

    override fun setSessionId(sessionId: String) {
        mSessionId = sessionId
    }

    override fun getTimestamp(): Long {
        return timestamp
    }

    override fun getModuleId(): Int {
        return moduleId
    }

    fun setOptOut(optOut: Boolean): ReportingMessage {
        this.optOut = optOut
        return this
    }

    fun setExceptionClassName(exceptionClassName: String?) {
        this.exceptionClassName = exceptionClassName
    }

    interface MessageType {
        companion object {
            const val SESSION_START = "ss"
            const val SESSION_END = "se"
            const val EVENT = "e"
            const val SCREEN_VIEW = "v"
            const val COMMERCE_EVENT = "cm"
            const val OPT_OUT = "o"
            const val ERROR = "x"
            const val PUSH_REGISTRATION = "pr"
            const val REQUEST_HEADER = "h"
            const val FIRST_RUN = "fr"
            const val APP_STATE_TRANSITION = "ast"
            const val PUSH_RECEIVED = "pm"
            const val BREADCRUMB = "bc"
            const val NETWORK_PERFORMNACE = "npe"
            const val PROFILE = "pro"
        }
    }

    class ProjectionReport(
        val projectionId: Int,
        val messageType: String,
        val eventName: String?,
        val eventType: String
    ) {
        companion object {
            fun fromEvent(projectionId: Int, event: MPEvent): ProjectionReport {
                return ProjectionReport(
                    projectionId,
                    MessageType.EVENT,
                    event.eventName,
                    event.eventType.name
                )
            }

            fun fromEvent(projectionId: Int, event: CommerceEvent?): ProjectionReport {
                return ProjectionReport(
                    projectionId,
                    MessageType.EVENT,
                    event?.eventName,
                    CommerceEventUtils.getEventTypeString(event)
                )
            }

            fun fromProjectionResult(result: ProjectionResult): ProjectionReport {
                return if (result.mPEvent != null) {
                    fromEvent(result.projectionId, result.mPEvent)
                } else {
                    fromEvent(result.projectionId, result.commerceEvent)
                }
            }
        }
    }

    companion object {
        fun fromPushMessage(provider: KitIntegration, intent: Intent?): ReportingMessage {
            return ReportingMessage(
                provider,
                MessageType.PUSH_RECEIVED,
                System.currentTimeMillis(),
                null
            )
        }

        fun fromPushRegistrationMessage(provider: KitIntegration): ReportingMessage {
            return ReportingMessage(
                provider,
                MessageType.PUSH_REGISTRATION,
                System.currentTimeMillis(),
                null
            )
        }

        fun logoutMessage(provider: KitIntegration): ReportingMessage {
            return ReportingMessage(
                provider,
                MessageType.PROFILE,
                System.currentTimeMillis(),
                null
            )
        }

        fun fromEvent(provider: KitIntegration, event: BaseEvent): ReportingMessage {
            val message = ReportingMessage(
                provider,
                event.type.messageType,
                System.currentTimeMillis(),
                event.customAttributeStrings
            )
            if (event is MPEvent) {
                val mpEvent = event
                message.eventTypeString = mpEvent.eventType.name
                message.eventName = mpEvent.eventName
            } else if (event is CommerceEvent) {
                message.eventTypeString = CommerceEventUtils.getEventTypeString(event)
            }
            return message
        }
    }
}
