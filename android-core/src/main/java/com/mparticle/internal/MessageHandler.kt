package com.mparticle.internal

import android.content.Context
import android.os.Looper
import android.os.Message
import com.mparticle.MParticle
import com.mparticle.internal.Constants.MessageKey
import com.mparticle.internal.MPUtility.addNumbers
import com.mparticle.internal.MPUtility.isEmpty
import com.mparticle.internal.MParticleApiClientImpl.MPNoConfigException
import com.mparticle.internal.MessageManager.IncrementUserAttributeMessage
import com.mparticle.internal.MessageManager.ReportingMpidMessage
import com.mparticle.internal.database.services.MParticleDBManager
import com.mparticle.internal.database.services.MParticleDBManager.AttributionChange
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeRemoval
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeResponse
import com.mparticle.internal.database.tables.SessionTable
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.internal.messages.MPAliasMessage
import org.json.JSONException
import org.json.JSONObject
import java.util.AbstractMap
import java.util.UUID

/* package-private */
internal open class MessageHandler : BaseHandler {
    private val mContext: Context

    var mMParticleDBManager: MParticleDBManager

    private val mMessageManagerCallbacks: MessageManagerCallbacks
    var mDataplanId: String? = null
    var mDataplanVersion: Int? = null

    /**
     * for unit testing only
     */
    constructor(
        messageManager: MessageManagerCallbacks,
        context: Context,
        dbManager: MParticleDBManager,
        dataplanId: String?,
        dataplanVersion: Int?
    ) {
        mMessageManagerCallbacks = messageManager
        mContext = context
        mMParticleDBManager = dbManager
        mDataplanId = dataplanId
        mDataplanVersion = dataplanVersion
    }

    constructor(
        looper: Looper,
        messageManager: MessageManagerCallbacks,
        context: Context,
        dbManager: MParticleDBManager,
        dataplanId: String?,
        dataplanVersion: Int?
    ) : super(looper) {
        mMessageManagerCallbacks = messageManager
        mContext = context
        mMParticleDBManager = dbManager
        mDataplanId = dataplanId
        mDataplanVersion = dataplanVersion
    }

    open fun databaseAvailable(): Boolean {
        try {
            return mMParticleDBManager.database != null
        } catch (ex: Exception) {
            Logger.error("Database unavailable.")
            return false
        }
    }

    override fun handleMessageImpl(msg: Message) {
        try {
            if (!databaseAvailable()) {
                return
            }
            mMessageManagerCallbacks.delayedStart()
        } catch (e: Exception) {
            Logger.verbose(e.toString())
        }
        when (msg.what) {
            STORE_MESSAGE -> try {
                val message = msg.obj as BaseMPMessage
                message.put(MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo())
                val messageType = message.getString(MessageKey.TYPE)
                // Handle the special case of session-start by creating the
                // session record first.
                if (Constants.MessageType.SESSION_START == messageType) {
                    dbInsertSession(message)
                } else {
                    mMParticleDBManager.updateSessionEndTime(message.sessionId, message.getLong(MessageKey.TIMESTAMP), 0)
                    message.put(MessageKey.ID, UUID.randomUUID().toString())
                }
                if (Constants.MessageType.ERROR == messageType) {
                    mMParticleDBManager.appendBreadcrumbs(message)
                }
                try {
                    mMParticleDBManager.insertMessage(mMessageManagerCallbacks.apiKey, message, mDataplanId, mDataplanVersion)
                } catch (e: MPNoConfigException) {
                    Logger.error("Unable to process uploads, API key and/or API Secret are missing.")
                    return
                }
                mMessageManagerCallbacks.checkForTrigger(message)
            } catch (e: Exception) {
                Logger.error(e, "Error saving message to mParticle DB.")
            }

            INSTALL_REFERRER_UPDATED -> try {
                mMParticleDBManager.updateSessionInstallReferrer(
                    msg.obj as String,
                    mMessageManagerCallbacks.deviceAttributes.getAppInfo(mContext, true)
                )
            } catch (e: Exception) {
                Logger.error(e, "Error updating session attributes in mParticle DB.")
            }

            UPDATE_SESSION_ATTRIBUTES -> try {
                val sessionAttributes = msg.obj as JSONObject
                val sessionId = sessionAttributes.getString(MessageKey.SESSION_ID)
                val attributes = sessionAttributes.optString(MessageKey.ATTRIBUTES)
                if (!isEmpty(attributes)) {
                    mMParticleDBManager.updateSessionAttributes(sessionId, attributes)
                }
            } catch (e: Exception) {
                Logger.error(e, "Error updating session attributes in mParticle DB.")
            }

            UPDATE_SESSION_END -> try {
                val session = msg.obj as InternalSession
                mMParticleDBManager.updateSessionEndTime(session.mSessionID, session.mLastEventTime, session.foregroundTime)
            } catch (e: Exception) {
                Logger.error(e, "Error updating session end time in mParticle DB.")
            }

            CREATE_SESSION_END_MESSAGE -> try {
                val entry = msg.obj as Map.Entry<String, Set<Long>>
                var endMessage: BaseMPMessage? = null
                val sessionId = entry.key
                try {
                    endMessage = mMParticleDBManager.getSessionForSessionEndMessage(
                        sessionId,
                        (mMessageManagerCallbacks as MessageManager).location,
                        entry.value
                    )
                } catch (jse: JSONException) {
                    Logger.warning("Failed to create mParticle session end message.")
                }
                if (endMessage != null) {
                    try {
                        Logger.verbose("Creating session end message for session ID: $sessionId")
                        mMParticleDBManager.insertMessage(mMessageManagerCallbacks.apiKey, endMessage, mDataplanId, mDataplanVersion)
                        mMParticleDBManager.updateSessionStatus(sessionId, SessionTable.SessionStatus.CLOSED)
                    } catch (e: MPNoConfigException) {
                        Logger.error("Unable to process uploads, API key and/or API Secret are missing.")
                        return
                    }
                } else {
                    Logger.error("Error creating session end, no entry for sessionId in mParticle DB.")
                }
                // 1 means this came from ending the session
                if (msg.arg1 == 1) {
                    mMessageManagerCallbacks.endUploadLoop()
                }
            } catch (e: Exception) {
                Logger.error(e, "Error creating session end message in mParticle DB.")
            } finally {
            }

            END_ORPHAN_SESSIONS -> try {
                Logger.verbose("Ending orphaned sessions.")
                // Find left-over sessions that exist during startup and end them.
                val mpid = msg.obj as Long
                val sessionIds = mMParticleDBManager.getOrphanSessionIds(mMessageManagerCallbacks.apiKey)
                for (sessionId in sessionIds) {
                    val entry: Map.Entry<String, Set<Long>> = AbstractMap.SimpleEntry(sessionId, setOf(mpid))
                    sendMessage(obtainMessage(CREATE_SESSION_END_MESSAGE, 0, 0, entry))
                }
            } catch (ex: MPNoConfigException) {
                Logger.error("Unable to process initialization, API key and or API Secret is missing.")
            } catch (e: Exception) {
                Logger.error(e, "Error processing initialization in mParticle DB.")
            }

            STORE_BREADCRUMB -> try {
                val message = msg.obj as BaseMPMessage
                message.put(MessageKey.ID, UUID.randomUUID().toString())
                try {
                    mMParticleDBManager.insertBreadcrumb(message, mMessageManagerCallbacks.apiKey)
                } catch (ex: MPNoConfigException) {
                    Logger.error("Unable to process uploads, API key and/or API Secret are missing.")
                }
            } catch (e: Exception) {
                Logger.error(e, "Error saving breadcrumb to mParticle DB.")
            }

            STORE_REPORTING_MESSAGE_LIST -> try {
                val reportingMessages = msg.obj as ReportingMpidMessage
                mMParticleDBManager.insertReportingMessages(
                    reportingMessages.reportingMessages as List<JsonReportingMessage?>,
                    reportingMessages.mpid
                )
            } catch (e: Exception) {
                Logger.verbose(e, "Error while inserting reporting messages: ", e.toString())
            }

            REMOVE_USER_ATTRIBUTE -> try {
                mMParticleDBManager.removeUserAttribute(msg.obj as UserAttributeRemoval, mMessageManagerCallbacks)
            } catch (e: Exception) {
                Logger.error(e, "Error while removing user attribute: ", e.toString())
            }

            SET_USER_ATTRIBUTE -> try {
                setUserAttributes(msg.obj as UserAttributeResponse)
            } catch (e: Exception) {
                Logger.error(e, "Error while setting user attribute: ", e.toString())
            }

            INCREMENT_USER_ATTRIBUTE -> try {
                val obj = msg.obj as IncrementUserAttributeMessage
                incrementUserAttribute(obj)
            } catch (e: Exception) {
                Logger.error(e, "Error while incrementing user attribute: ", e.toString())
            }

            CLEAR_MESSAGES_FOR_UPLOAD -> mMessageManagerCallbacks.messagesClearedForUpload()
            STORE_ALIAS_MESSAGE -> try {
                val aliasMessage = msg.obj as MPAliasMessage
                mMParticleDBManager.insertAliasRequest(mMessageManagerCallbacks.apiKey, aliasMessage)

                val instance = MParticle.getInstance()
                instance?.upload()
            } catch (ex: MPNoConfigException) {
                Logger.error("Unable to Alias Request, API key and or API Secret is missing")
            } catch (ex: Exception) {
                Logger.error("Error sending Alias Request")
            }
        }
    }

    fun setUserAttributes(response: UserAttributeResponse?) {
        val attributionChanges = mMParticleDBManager.setUserAttribute(response)
        for (attributionChange in attributionChanges) {
            logUserAttributeChanged(attributionChange)
        }
    }

    private fun incrementUserAttribute(message: IncrementUserAttributeMessage) {
        val userAttributes = mMParticleDBManager.getUserAttributeSingles(message.mpid)

        if (!userAttributes.containsKey(message.key)) {
            val userAttributeList = mMParticleDBManager.getUserAttributeLists(message.mpid)
            if (userAttributeList.containsKey(message.key)) {
                Logger.error("Error while attempting to increment user attribute - existing attribute is a list, which can't be incremented.")
                return
            }
        }
        var newValue: String? = null
        val currentValue = userAttributes[message.key]
        if (currentValue == null) {
            newValue = message.incrementBy.toString()
        } else if (currentValue is Number) {
            newValue = addNumbers(currentValue, message.incrementBy).toString()
            Logger.info("incrementing attribute: \"" + message.key + "\" from: " + currentValue + " by: " + message.incrementBy + " to: " + newValue)
        }
        val wrapper = UserAttributeResponse()
        wrapper.attributeSingles = HashMap(1)
        wrapper.attributeSingles[message.key] = newValue
        wrapper.mpId = message.mpid
        val attributionChanges = mMParticleDBManager.setUserAttribute(wrapper)
        for (attributeChange in attributionChanges) {
            logUserAttributeChanged(attributeChange)
        }
        val instance = MParticle.getInstance()
        if (instance != null && instance.Internal().kitManager != null) {
            instance.Internal().kitManager.incrementUserAttribute(message.key, message.incrementBy, newValue, message.mpid)
        }
    }

    @Throws(JSONException::class)
    private fun dbInsertSession(message: BaseMPMessage) {
        try {
            val deviceAttributes = mMessageManagerCallbacks.deviceAttributes
            mMParticleDBManager.insertSession(
                message,
                mMessageManagerCallbacks.apiKey,
                deviceAttributes.getAppInfo(mContext),
                deviceAttributes.getDeviceInfo(mContext)
            )
        } catch (ex: MPNoConfigException) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing.")
        }
    }

    private fun logUserAttributeChanged(attributionChange: AttributionChange) {
        mMessageManagerCallbacks.logUserAttributeChangeMessage(
            attributionChange.key,
            attributionChange.newValue,
            attributionChange.oldValue,
            attributionChange.isDeleted,
            attributionChange.isNewAttribute,
            attributionChange.time,
            attributionChange.mpId
        )
    }

    companion object {
        const val STORE_MESSAGE: Int = 0
        const val UPDATE_SESSION_ATTRIBUTES: Int = 1
        const val UPDATE_SESSION_END: Int = 2
        const val CREATE_SESSION_END_MESSAGE: Int = 3
        const val END_ORPHAN_SESSIONS: Int = 4
        const val STORE_BREADCRUMB: Int = 5
        const val STORE_REPORTING_MESSAGE_LIST: Int = 9
        const val REMOVE_USER_ATTRIBUTE: Int = 10
        const val SET_USER_ATTRIBUTE: Int = 11
        const val INCREMENT_USER_ATTRIBUTE: Int = 12
        const val INSTALL_REFERRER_UPDATED: Int = 13
        const val CLEAR_MESSAGES_FOR_UPLOAD: Int = 14
        const val STORE_ALIAS_MESSAGE: Int = 15
    }
}
