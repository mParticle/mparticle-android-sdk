package com.mparticle.internal

import com.mparticle.internal.database.services.MessageService.ReadyMessage

class BatchId {
    val mpid: Long
    var sessionId: String?
    var dataplanId: String?
    var dataplanVersion: Int?

    constructor(mpid: Long, sessionId: String?, dataplanId: String?, dataplanVersion: Int?) {
        this.mpid = mpid
        this.sessionId = sessionId
        this.dataplanId = dataplanId
        this.dataplanVersion = dataplanVersion
    }

    constructor(readyMessage: ReadyMessage) {
        mpid = readyMessage.mpid
        sessionId = readyMessage.sessionId
        dataplanId = readyMessage.dataplanId
        dataplanVersion = readyMessage.dataplanVersion
    }
}