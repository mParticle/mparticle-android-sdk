package com.mparticle.internal.database.services

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.annotation.CheckResult
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.identity.UserAttributeListenerWrapper
import com.mparticle.internal.BatchId
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.DatabaseHelper
import com.mparticle.internal.DeviceAttributes
import com.mparticle.internal.InternalSession
import com.mparticle.internal.JsonReportingMessage
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.internal.MessageBatch
import com.mparticle.internal.MessageManager
import com.mparticle.internal.MessageManagerCallbacks
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.MPDatabaseImpl
import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.listeners.InternalListenerManager
import com.mparticle.internal.messages.BaseMPMessage
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.TreeMap
import java.util.UUID

open class MParticleDBManager {
    private val mPreferences: SharedPreferences?
    private val mContext: Context?
    private val mDatabaseHelper: DatabaseHelper?
    private val options: MParticleOptions?

    internal constructor() {
        // for unit testing
        mPreferences = null
        mContext = null
        mDatabaseHelper = null
        options = null
    }

    constructor(context: Context?, options: MParticleOptions?) {
        mContext = context
        this.options = options
        mPreferences = context!!.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        mDatabaseHelper = DatabaseHelper(context)
    }

    constructor(context: Context?) : this(context, null)

    /**
     * Creates a new SQLiteDatabase instance, if the Database has not been opened yet, it returns
     * an instance. Each instance is a singleton, per thread.
     *
     * @return
     */
    open fun getDatabase(): MPDatabase? = MPDatabaseImpl(mDatabaseHelper!!.writableDatabase)

    open fun updateMpId(oldMpId: Long, newMpId: Long) {
        val db = getDatabase()
        try {
            db!!.beginTransaction()
            BreadcrumbService().updateMpId(db, oldMpId, newMpId)
            MessageService().updateMpId(db, oldMpId, newMpId)
            ReportingService().updateMpId(db, oldMpId, newMpId)
            SessionService().updateMpId(db, oldMpId, newMpId)
            UserAttributesService().updateMpId(db, oldMpId, newMpId)
            db.setTransactionSuccessful()
        } finally {
            db!!.endTransaction()
        }
    }

    // Breadcumb Service Methods.

    @Throws(JSONException::class)
    open fun insertBreadcrumb(message: BaseMPMessage?, apiKey: String?) {
        BreadcrumbService.insertBreadcrumb(getDatabase(), mContext, message, apiKey, message!!.mpId)
    }

    @Throws(JSONException::class)
    open fun appendBreadcrumbs(message: BaseMPMessage?) {
        val breadcrumbs = BreadcrumbService.getBreadcrumbs(getDatabase(), mContext, message!!.mpId)
        if (!MPUtility.isEmpty(breadcrumbs)) {
            message.put(Constants.MessageType.BREADCRUMB, breadcrumbs)
        }
    }

    // Message Service Methods.

    open fun cleanupMessages() {
        MessageService.cleanupMessages(getDatabase())
    }

    @Throws(JSONException::class)
    open fun insertMessage(apiKey: String?, message: BaseMPMessage?, dataplanId: String?, dataplanVersion: Int?) {
        MessageService.insertMessage(getDatabase(), apiKey, message, message!!.mpId, dataplanId, dataplanVersion)
        sMessageListener?.onMessageStored(message)
    }

    open fun updateSessionInstallReferrer(sessionId: String?, appInfo: JSONObject?) {
        SessionService.updateSessionInstallReferrer(getDatabase(), appInfo, sessionId)
    }

    fun interface MessageListener {
        fun onMessageStored(message: BaseMPMessage)
    }

    // Prepare Messages for Upload.

    open fun hasMessagesForUpload(): Boolean {
        val db = getDatabase()
        return MessageService.hasMessagesForUpload(db)
    }

    @Throws(JSONException::class)
    open fun createMessagesForUploadMessage(
        configManager: ConfigManager?,
        deviceAttributes: DeviceAttributes?,
        currentSessionId: String?,
        uploadSettings: UploadSettings?,
    ) {
        val db = getDatabase()
        db!!.beginTransaction()
        try {
            val readyMessages = MessageService.getMessagesForUpload(db)
            if (readyMessages.size <= 0) {
                db.setTransactionSuccessful()
                return
            }
            val uploadMessagesByBatchId = getUploadMessageByBatchIdMap(readyMessages, db, configManager, deviceAttributes, false)

            val reportingMessages = ReportingService.getReportingMessagesForUpload(db)
            for (reportingMessage in reportingMessages) {
                var match: MessageBatch? = null
                var sessionIdMatch: MessageBatch? = null
                var notAMatch: MessageBatch? = null
                for ((batchId, messageBatch) in uploadMessagesByBatchId) {
                    if (MPUtility.isEqual(batchId.sessionId, reportingMessage.sessionId) && MPUtility.isEqual(batchId.mpid, reportingMessage.mpid)) {
                        match = messageBatch
                    } else if (MPUtility.isEqual(batchId.sessionId, reportingMessage.sessionId)) {
                        sessionIdMatch = messageBatch
                    } else {
                        notAMatch = messageBatch
                    }
                }
                if (match == null) {
                    // if there's not matching by session id & mpid, use the first matching session id
                    match = sessionIdMatch
                }
                if (match == null) {
                    // if there's no matching session id then just use the first batch object
                    match = notAMatch
                }
                if (match != null) {
                    match.addReportingMessage(reportingMessage.msgObject)
                    InternalListenerManager.listener.onCompositeObjects(reportingMessage, match)
                    ReportingService.deleteReportingMessage(db, reportingMessage.reportingMessageId)
                }
            }
            val deviceInfos = SessionService.processSessions(db, uploadMessagesByBatchId)
            for (deviceInfo in deviceInfos) {
                deviceAttributes!!.updateDeviceInfo(mContext, deviceInfo)
            }
            createUploads(uploadMessagesByBatchId, db, deviceAttributes, configManager, currentSessionId, uploadSettings)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    open fun deleteMessagesAndSessions(currentSessionId: String?) {
        val db = getDatabase()
        try {
            db!!.beginTransaction()
            MessageService.deleteOldMessages(db, currentSessionId)
            SessionService.deleteSessions(db, currentSessionId)
            db.setTransactionSuccessful()
        } finally {
            db!!.endTransaction()
        }
    }

    /**
     * Age-based retention sweep across the three persistence tables.
     *
     * Deletes any messages and uploads whose `CREATED_AT` is strictly less than
     * `cutoffMillis`, and any sessions whose `END_TIME` is strictly less than
     * `cutoffMillis`.
     * `MPPersistenceController.deleteRecordsOlderThan:`, but reports success so
     * callers can decide whether to arm retry/throttle state.
     *
     * @param cutoffMillis the unix-epoch millisecond cutoff; rows older than this are removed
     * @return `true` if the transaction committed successfully, `false` if any
     *         exception was caught (and logged) during the sweep
     */
    @CheckResult
    open fun deleteRecordsOlderThan(cutoffMillis: Long): Boolean {
        val db = getDatabase()
        try {
            db!!.beginTransaction()
            MessageService.deleteMessagesOlderThan(db, cutoffMillis)
            UploadService.deleteUploadsOlderThan(db, cutoffMillis)
            SessionService.deleteSessionsOlderThan(db, cutoffMillis)
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            Logger.warning(e, "Error pruning persisted records older than $cutoffMillis ms.")
            return false
        } finally {
            db!!.endTransaction()
        }
    }

    private fun getUploadMessageByBatchIdMap(
        readyMessages: List<MessageService.ReadyMessage>,
        db: MPDatabase?,
        configManager: ConfigManager?,
        deviceAttributes: DeviceAttributes?,
    ): HashMap<BatchId, MessageBatch> = getUploadMessageByBatchIdMap(readyMessages, db, configManager, deviceAttributes, false)

    private fun getUploadMessageByBatchIdMap(
        readyMessages: List<MessageService.ReadyMessage>,
        db: MPDatabase?,
        configManager: ConfigManager?,
        deviceAttributes: DeviceAttributes?,
        markAsUpload: Boolean,
    ): HashMap<BatchId, MessageBatch> {
        val uploadMessagesByBatchId = HashMap<BatchId, MessageBatch>()
        var highestUploadedMessageId = -1
        for (readyMessage in readyMessages) {
            val batchId = BatchId(readyMessage)
            var uploadMessage = uploadMessagesByBatchId[batchId]
            if (uploadMessage == null) {
                uploadMessage = createUploadMessage(configManager, true, batchId)
                // Count the batch header toward LIMIT_MAX_UPLOAD_SIZE, not only the messages.
                uploadMessage.setAppInfo(deviceAttributes!!.getAppInfo(mContext))
                uploadMessage.setDeviceInfo(deviceAttributes.getDeviceInfo(mContext))
                uploadMessage.setIdentities(configManager!!.getUserIdentityJson(batchId.mpid))
                uploadMessage.setUserAttributes(getAllUserAttributesJson(batchId.mpid))
                uploadMessage.put(Constants.MessageKey.MESSAGES, JSONArray())
                val batchHeader = uploadMessage.toString()
                if (batchHeader != null) {
                    uploadMessage.incrementMessageLengthBytes(batchHeader.length.toLong())
                }
                uploadMessagesByBatchId[batchId] = uploadMessage
            }
            val messageLength = readyMessage.message!!.length
            val msgObject = JSONObject(readyMessage.message)
            // Always take a batch's first message, so an oversized header cannot stall the queue.
            val batchMessages = uploadMessage.messages
            val batchHasMessages = batchMessages != null && batchMessages.length() > 0
            if (batchHasMessages && messageLength + uploadMessage.messageLengthBytes > Constants.LIMIT_MAX_UPLOAD_SIZE) {
                break
            }
            uploadMessage.addMessage(msgObject)
            InternalListenerManager.listener.onCompositeObjects(readyMessage, uploadMessage)
            // +1 for the separator between messages.
            uploadMessage.incrementMessageLengthBytes((messageLength + 1).toLong())
            highestUploadedMessageId = readyMessage.messageId
        }
        if (markAsUpload) {
            // Else mark the messages as uploaded, so next time around it'll be included in session history.
            MessageService.markMessagesAsUploaded(db, highestUploadedMessageId)
        } else {
            // If this is a session-less message, or if session history is disabled, just delete it.
            MessageService.deleteMessages(db, highestUploadedMessageId)
        }
        return uploadMessagesByBatchId
    }

    private fun createUploads(
        uploadMessagesByBatchId: Map<BatchId, MessageBatch?>,
        db: MPDatabase?,
        deviceAttributes: DeviceAttributes?,
        configManager: ConfigManager?,
        currentSessionId: String?,
        uploadSettings: UploadSettings?,
    ) {
        for ((batchId, uploadMessage) in uploadMessagesByBatchId) {
            if (uploadMessage != null) {
                val sessionId = batchId.sessionId

                // For upgrade scenarios, there may be no device or app customAttributes associated with the session, so create it now.
                if (uploadMessage.appInfo == null) {
                    uploadMessage.setAppInfo(deviceAttributes!!.getAppInfo(mContext))
                }
                if (uploadMessage.deviceInfo == null || sessionId!! == currentSessionId) {
                    uploadMessage.setDeviceInfo(deviceAttributes!!.getDeviceInfo(mContext))
                }
                val messages = uploadMessage.messages
                val identities = findIdentityState(configManager, messages, batchId.mpid)
                uploadMessage.setIdentities(identities)
                val userAttributes = findUserAttributeState(messages, batchId.mpid)
                uploadMessage.setUserAttributes(userAttributes)

                var batch: JSONObject? = uploadMessage
                val batchCreationListener = options?.batchCreationListener
                if (batchCreationListener != null) {
                    try {
                        batch = batchCreationListener.onBatchCreated(uploadMessage)
                        if (batch == null || batch.length() == 0) {
                            Logger.error("Not uploading batch due to 'onCreateBatch' handler being empty")
                            return
                        } else {
                            batch.put(Constants.MessageKey.MODIFIED_BATCH, true)
                        }
                    } catch (e: Exception) {
                        Logger.error(e, "batch creation listener error, original batch will be uploaded")
                    }
                }

                // The messages are already consumed, so log an oversized batch instead of storing it for
                // cleanupUploadMessages to delete silently.
                val batchString = batch!!.toString()
                if (batchString != null && batchString.length > Constants.LIMIT_MAX_UPLOAD_SIZE) {
                    Logger.error(
                        "Not uploading batch of size ${batchString.length} that exceeds maximum safe size of " +
                            "${Constants.LIMIT_MAX_UPLOAD_SIZE} bytes.",
                    )
                    continue
                }
                UploadService.insertUpload(db, batch, uploadSettings)
                cleanSessions(currentSessionId)
            }
        }
    }

    /**
     * remove Session entries that do not have Message entries referencing them, and
     * are not the current Session
     *
     * @param currentSessionId
     */
    internal fun cleanSessions(currentSessionId: String?) {
        val database = getDatabase()
        val sessionIds = MessageService.getSessionIds(database)
        sessionIds.add(currentSessionId)
        SessionService.deleteSessions(database, sessionIds)
    }

    /**
     * Look for the last UAC message to find the end-state of user attributes.
     */
    private fun findUserAttributeState(messages: JSONArray?, mpId: Long): JSONObject {
        var userAttributes: JSONObject? = null
        if (messages != null) {
            for (i in 0 until messages.length()) {
                try {
                    if (messages.getJSONObject(i).get(Constants.MessageKey.TYPE) == Constants.MessageType.USER_ATTRIBUTE_CHANGE) {
                        userAttributes = messages.getJSONObject(i).getJSONObject(Constants.MessageKey.USER_ATTRIBUTES)
                        messages.getJSONObject(i).remove(Constants.MessageKey.USER_ATTRIBUTES)
                    }
                } catch (ignored: JSONException) {
                } catch (ignored: NullPointerException) {
                }
            }
        }
        return if (userAttributes == null) {
            getAllUserAttributesJson(mpId)
        } else {
            userAttributes
        }
    }

    /**
     * Look for the last UIC message to find the end-state of user identities.
     */
    private fun findIdentityState(configManager: ConfigManager?, messages: JSONArray?, mpId: Long): JSONArray? {
        var identities: JSONArray? = null
        if (messages != null) {
            for (i in 0 until messages.length()) {
                try {
                    if (messages.getJSONObject(i).get(Constants.MessageKey.TYPE) == Constants.MessageType.USER_IDENTITY_CHANGE) {
                        identities = messages.getJSONObject(i).getJSONArray(Constants.MessageKey.USER_IDENTITIES)
                        messages.getJSONObject(i).remove(Constants.MessageKey.USER_IDENTITIES)
                    }
                } catch (ignored: JSONException) {
                } catch (ignored: NullPointerException) {
                }
            }
        }
        return if (identities == null) {
            configManager!!.getUserIdentityJson(mpId)
        } else {
            identities
        }
    }

    /**
     * Method that is responsible for building an upload message to be sent over the wire.
     */
    private fun createUploadMessage(configManager: ConfigManager?, history: Boolean, batchId: BatchId): MessageBatch {
        val batchMessage = MessageBatch.create(history, configManager, configManager!!.getCookies(batchId.mpid), batchId)
        return batchMessage
    }

    open fun updateSessionEndTime(sessionId: String?, endTime: Long, sessionLength: Long) {
        SessionService.updateSessionEndTime(getDatabase(), sessionId, endTime, sessionLength)
    }

    open fun updateSessionAttributes(sessionId: String?, attributes: String?) {
        SessionService.updateSessionAttributes(getDatabase(), sessionId, attributes)
    }

    open fun updateSessionStatus(sessionId: String?, status: String?) {
        SessionService.updateSessionStatus(getDatabase(), sessionId, status)
    }

    @Throws(JSONException::class)
    open fun getSessionForSessionEndMessage(sessionId: String?, location: Location?, mpIds: Set<Long>?): BaseMPMessage? {
        var selectCursor: Cursor? = null
        try {
            val cursor = SessionService.getSessionForSessionEndMessage(getDatabase(), sessionId)
            selectCursor = cursor
            var endMessage: BaseMPMessage? = null
            if (cursor.moveToFirst()) {
                val start = cursor.getLong(0)
                val end = cursor.getLong(1)
                val foregroundLength = cursor.getLong(2)
                val attributes = cursor.getString(3)
                var sessionAttributes: JSONObject? = null
                if (attributes != null) {
                    sessionAttributes = JSONObject(attributes)
                }

                // Create a session-end message.
                endMessage = createMessageSessionEnd(sessionId, start, end, foregroundLength, sessionAttributes, location, mpIds)
                endMessage.put(Constants.MessageKey.ID, UUID.randomUUID().toString())
            }
            return endMessage
        } finally {
            if (selectCursor != null && !selectCursor.isClosed) {
                selectCursor.close()
            }
        }
    }

    @Throws(JSONException::class)
    internal fun createMessageSessionEnd(
        sessionId: String?,
        start: Long,
        end: Long,
        foregroundLength: Long,
        sessionAttributes: JSONObject?,
        location: Location?,
        mpIds: Set<Long>?,
    ): BaseMPMessage {
        val eventCounter = mPreferences!!.getInt(Constants.PrefKeys.EVENT_COUNTER, 0)
        resetEventCounter()
        val session = InternalSession()
        session.mSessionID = sessionId
        session.mSessionStartTime = start
        val spanningMpids = JSONArray()
        var storageMpid = Constants.TEMPORARY_MPID
        for (mpid in mpIds!!) {
            // We do not need to associate a SessionEnd message with any particular MPID, as long as it is not == Constants.TEMPORARY_MPID.
            if (mpid != Constants.TEMPORARY_MPID) {
                spanningMpids.put(mpid)
                storageMpid = mpid
            }
        }
        val message =
            BaseMPMessage
                .Builder(Constants.MessageType.SESSION_END)
                .timestamp(end)
                .attributes(sessionAttributes)
                .build(session, location, storageMpid)
        message.put(Constants.MessageKey.EVENT_COUNTER, eventCounter)
        message.put(Constants.MessageKey.SESSION_LENGTH, foregroundLength)
        message.put(Constants.MessageKey.SESSION_LENGTH_TOTAL, end - start)
        message.put(Constants.MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo())
        message.put(Constants.MessageKey.SESSION_SPANNING_MPIDS, spanningMpids)
        return message
    }

    private fun resetEventCounter() {
        mPreferences!!.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, 0).apply()
    }

    open fun getOrphanSessionIds(apiKey: String?): List<String?> = SessionService.getOrphanSessionIds(getDatabase(), apiKey)

    @Throws(JSONException::class)
    open fun insertSession(message: BaseMPMessage?, apiKey: String?, appInfo: JSONObject?, deviceInfo: JSONObject?) {
        val appInfoString = appInfo!!.toString()
        val deviceInfoString = deviceInfo!!.toString()
        SessionService.insertSession(getDatabase(), message, apiKey, appInfoString, deviceInfoString, message!!.mpId)
    }

    // Reporting Service Methods

    open fun insertReportingMessages(reportingMessages: List<JsonReportingMessage?>?, mpId: Long) {
        val db = getDatabase()
        try {
            db!!.beginTransaction()
            for (i in reportingMessages!!.indices) {
                ReportingService.insertReportingMessage(db, reportingMessages[i], mpId)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Logger.verbose("Error inserting reporting message: $e")
        } finally {
            db!!.endTransaction()
        }
    }

    // Upload Service Methods

    open fun cleanupUploadMessages() {
        UploadService.cleanupUploadMessages(getDatabase())
    }

    open fun getReadyUploads(): List<ReadyUpload> = UploadService.getReadyUploads(getDatabase())

    open fun deleteUpload(id: Int): Int = UploadService.deleteUpload(getDatabase(), id)

    open fun insertAliasRequest(request: JSONObject, uploadSettings: UploadSettings) {
        UploadService.insertAliasRequest(getDatabase(), request, uploadSettings)
    }

    // UserAttribute Service Methods

    open fun getUserAttributeSingles(mpId: Long): Map<String, Any?>? {
        if (getDatabase() != null) {
            val stringifiedAttributes = UserAttributesService.getUserAttributesSingles(getDatabase(), mpId)
            val typedAttributes: MutableMap<String, Any?> = HashMap()
            for ((key, value) in stringifiedAttributes) {
                typedAttributes[key] = MPUtility.toNumberOrString(value)
            }
            return typedAttributes
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    open fun getUserAttributeLists(mpId: Long): TreeMap<String, List<String>>? {
        if (getDatabase() != null) {
            return UserAttributesService.getUserAttributesLists(getDatabase(), mpId) as TreeMap<String, List<String>>
        }
        return null
    }

    open fun getAllUserAttributesJson(mpId: Long): JSONObject {
        val attributes = getUserAttributes(null, mpId)
        val jsonAttributes = JSONObject()
        for ((key, value) in attributes!!) {
            if (value is List<*>) {
                @Suppress("UNCHECKED_CAST")
                val attributeList = value as List<String>
                val jsonArray = JSONArray()
                for (attribute in attributeList) {
                    jsonArray.put(attribute)
                }
                try {
                    jsonAttributes.put(key, jsonArray)
                } catch (ignored: JSONException) {
                }
            } else {
                try {
                    var entryValue = value
                    if (entryValue == null) {
                        entryValue = JSONObject.NULL
                    }
                    jsonAttributes.put(key, entryValue.toString())
                } catch (e: JSONException) {
                }
            }
        }
        return jsonAttributes
    }

    open fun getUserAttributes(mpId: Long): Map<String, Any?> = getUserAttributes(null, mpId)!!

    open fun getUserAttributes(listener: UserAttributeListenerWrapper?, mpId: Long): Map<String, Any?>? {
        val allUserAttributes: MutableMap<String, Any?> = HashMap()
        if (listener == null || Looper.getMainLooper() !== Looper.myLooper()) {
            val userAttributes = getUserAttributeSingles(mpId)
            val userAttributeLists: Map<String, List<String>>? = getUserAttributeLists(mpId)
            listener?.onUserAttributesReceived(userAttributes, userAttributeLists, mpId)
            if (userAttributes != null) {
                allUserAttributes.putAll(userAttributes)
            }
            if (userAttributeLists != null) {
                allUserAttributes.putAll(userAttributeLists)
            }
            return allUserAttributes
        } else {
            val instance = MParticle.getInstance()
            if (instance != null) {
                instance
                    .Internal()
                    .messageManager
                    .messageHandler!!
                    .post(
                        object : Runnable {
                            override fun run() {
                                val attributeSingles = getUserAttributeSingles(mpId)
                                val attributeLists: Map<String, List<String>>? = getUserAttributeLists(mpId)
                                Handler(Looper.getMainLooper()).post(
                                    object : Runnable {
                                        override fun run() {
                                            listener.onUserAttributesReceived(attributeSingles, attributeLists, mpId)
                                        }
                                    },
                                )
                            }
                        },
                    )
            }
            return null
        }
    }

    open fun setUserAttribute(userAttribute: UserAttributeResponse?): List<AttributionChange> {
        val attributionChanges: MutableList<AttributionChange> = ArrayList()
        if (getDatabase() == null) {
            return attributionChanges
        }
        val currentValues = getUserAttributes(null, userAttribute!!.mpId)
        val db = getDatabase()
        try {
            db!!.beginTransaction()
            val time = System.currentTimeMillis()
            if (userAttribute.attributeLists != null) {
                for ((key, attributeValues) in userAttribute.attributeLists!!) {
                    val oldValue = currentValues!![key]
                    if (oldValue != null && oldValue is List<*> && oldValue == attributeValues) {
                        continue
                    }
                    val deleted = UserAttributesService.deleteAttributes(db, key, userAttribute.mpId)
                    val isNewAttribute = deleted == 0
                    for (attributeValue in attributeValues) {
                        UserAttributesService.insertAttribute(db, key, attributeValue, time, true, userAttribute.mpId)
                    }
                    attributionChanges.add(
                        AttributionChange(key, attributeValues, oldValue, false, isNewAttribute, userAttribute.time, userAttribute.mpId),
                    )
                }
            }
            if (userAttribute.attributeSingles != null) {
                for ((key, value) in userAttribute.attributeSingles!!) {
                    var attributeValue: String? = null
                    if (value != null) {
                        attributeValue = value.toString()
                    }
                    val oldValue = currentValues!![key]
                    if (oldValue != null && oldValue is String && oldValue.equals(attributeValue, ignoreCase = true)) {
                        continue
                    }
                    val deleted = UserAttributesService.deleteAttributes(db, key, userAttribute.mpId)
                    val isNewAttribute = deleted == 0
                    UserAttributesService.insertAttribute(db, key, attributeValue, time, false, userAttribute.mpId)
                    attributionChanges.add(
                        AttributionChange(key, attributeValue, oldValue, false, isNewAttribute, userAttribute.time, userAttribute.mpId),
                    )
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Logger.error(e, "Error while adding user attributes: ", e.toString())
        } finally {
            db!!.endTransaction()
        }
        return attributionChanges
    }

    open fun removeUserAttribute(container: UserAttributeRemoval?, callbacks: MessageManagerCallbacks?) {
        val currentValues = getUserAttributes(null, container!!.mpId)
        val db = getDatabase()
        try {
            db!!.beginTransaction()
            val deleted = UserAttributesService.deleteAttributes(db, container.key, container.mpId)
            if (callbacks != null && deleted > 0) {
                callbacks.attributeRemoved(container.key, container.mpId)
                callbacks.logUserAttributeChangeMessage(
                    container.key,
                    null,
                    currentValues!![container.key],
                    true,
                    false,
                    container.time,
                    container.mpId,
                )
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
        } finally {
            db!!.endTransaction()
        }
    }

    open fun resetDatabaseForWorkspaceSwitching() {
        val db = getDatabase()
        try {
            db!!.beginTransaction()
            BreadcrumbService.deleteAll(db)
            MessageService.deleteAll(db)
            ReportingService.deleteAll(db)
            SessionService.deleteAll(db)
            UserAttributesService.deleteAll(db)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
        } finally {
            db!!.endTransaction()
        }
    }

    open class AttributionChange(
        open val key: String?,
        open val newValue: Any?,
        open val oldValue: Any?,
        open val isDeleted: Boolean,
        open val isNewAttribute: Boolean,
        open val time: Long,
        open val mpId: Long,
    )

    open class ReadyUpload(open val id: Int, open val isAliasRequest: Boolean, open val message: String?, open val uploadSettings: UploadSettings?)

    open class UserAttributeRemoval {
        @JvmField
        var key: String? = null

        @JvmField
        var time: Long = 0

        @JvmField
        var mpId: Long = 0
    }

    open class UserAttributeResponse {
        @JvmField
        var attributeSingles: MutableMap<String, Any?>? = null

        @JvmField
        var attributeLists: MutableMap<String, MutableList<String>>? = null

        @JvmField
        var time: Long = 0

        @JvmField
        var mpId: Long = 0
    }

    internal companion object {
        private var sMessageListener: MessageListener? = null

        fun setMessageListener(messageListener: MessageListener?) {
            sMessageListener = messageListener
        }
    }
}
