package com.mparticle.internal

interface ReportingManager {
    fun log(message: JsonReportingMessage)

    fun logAll(messageList: List<JsonReportingMessage>)
}
