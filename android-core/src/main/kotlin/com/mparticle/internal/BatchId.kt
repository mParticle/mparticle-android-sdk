package com.mparticle.internal

import com.mparticle.internal.database.services.MessageService.ReadyMessage

class BatchId {
    var mpid: Long
        get() = field
    var sessionId: String?
        get() = field
    var dataplanId: String?
        get() = field
    var dataplanVersion: Int?
        get() = field

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

    override fun equals(obj: Any?): Boolean {
        if (obj !is BatchId) {
            return false
        }
        for (i in 0 until fields().size) {
            if (!MPUtility.isEqual(fields()[i], obj.fields()[i])) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        return fields().contentHashCode()
    }

    private fun fields(): Array<Any?> {
        return arrayOf(mpid, sessionId, dataplanId, dataplanVersion)
    }
}