package com.mparticle.internal.database.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.UserAttributeListenerWrapper;
import com.mparticle.internal.BatchId;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.DatabaseHelper;
import com.mparticle.internal.DeviceAttributes;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.JsonReportingMessage;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageBatch;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.MessageManagerCallbacks;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.MPDatabaseImpl;
import com.mparticle.internal.database.UploadSettings;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.internal.messages.BaseMPMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class MParticleDBManager {
    private SharedPreferences mPreferences;
    private Context mContext;
    private DatabaseHelper mDatabaseHelper;
    private MParticleOptions options;

    MParticleDBManager() {
        //for unit testing
    }

    public MParticleDBManager(Context context, @Nullable MParticleOptions options) {
        this.mContext = context;
        this.options = options;
        mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mDatabaseHelper = new DatabaseHelper(context);
    }

    public MParticleDBManager(Context context) {
        this(context, null);
    }

    /**
     * Creates a new SQLiteDatabase instance, if the Database has not been opened yet, it returns
     * an instance. Each instance is a singleton, per thread.
     *
     * @return
     */
    public MPDatabase getDatabase() {
        return new MPDatabaseImpl(mDatabaseHelper.getWritableDatabase());
    }

    public void updateMpId(long oldMpId, long newMpId) {
        MPDatabase db = getDatabase();
        try {
            db.beginTransaction();
            new BreadcrumbService().updateMpId(db, oldMpId, newMpId);
            new MessageService().updateMpId(db, oldMpId, newMpId);
            new ReportingService().updateMpId(db, oldMpId, newMpId);
            new SessionService().updateMpId(db, oldMpId, newMpId);
            new UserAttributesService().updateMpId(db, oldMpId, newMpId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Breadcumb Service Methods.
     */


    public void insertBreadcrumb(BaseMPMessage message, String apiKey) throws JSONException {
        BreadcrumbService.insertBreadcrumb(getDatabase(), mContext, message, apiKey, message.getMpId());
    }

    public void appendBreadcrumbs(BaseMPMessage message) throws JSONException {
        JSONArray breadcrumbs = BreadcrumbService.getBreadcrumbs(getDatabase(), mContext, message.getMpId());
        if (!MPUtility.isEmpty(breadcrumbs)) {
            message.put(Constants.MessageType.BREADCRUMB, breadcrumbs);
        }
    }

    /**
     * Message Service Methods.
     */

    public void cleanupMessages() {
        MessageService.cleanupMessages(getDatabase());
    }

    public void insertMessage(String apiKey, BaseMPMessage message, String dataplanId, Integer dataplanVersion) throws JSONException {
        MessageService.insertMessage(getDatabase(), apiKey, message, message.getMpId(), dataplanId, dataplanVersion);
        if (sMessageListener != null) {
            sMessageListener.onMessageStored(message);
        }
    }

    private static MessageListener sMessageListener;

    static void setMessageListener(MessageListener messageListener) {
        sMessageListener = messageListener;
    }

    public void updateSessionInstallReferrer(String sessionId, JSONObject appInfo) {
        SessionService.updateSessionInstallReferrer(getDatabase(), appInfo, sessionId);
    }

    public interface MessageListener {
        void onMessageStored(BaseMPMessage message);
    }

    /**
     * Prepare Messages for Upload.
     */

    public boolean hasMessagesForUpload() {
        MPDatabase db = getDatabase();
        return MessageService.hasMessagesForUpload(db);
    }

    public void createMessagesForUploadMessage(ConfigManager configManager, DeviceAttributes deviceAttributes, String currentSessionId, UploadSettings uploadSettings) throws JSONException {
        MPDatabase db = getDatabase();
        db.beginTransaction();
        try {
            List<MessageService.ReadyMessage> readyMessages = MessageService.getMessagesForUpload(db);
            if (readyMessages.size() <= 0) {
                db.setTransactionSuccessful();
                return;
            }
            HashMap<BatchId, MessageBatch> uploadMessagesByBatchId = getUploadMessageByBatchIdMap(readyMessages, db, configManager, false);

            List<ReportingService.ReportingMessage> reportingMessages = ReportingService.getReportingMessagesForUpload(db);
            for (ReportingService.ReportingMessage reportingMessage : reportingMessages) {
                MessageBatch match = null;
                MessageBatch sessionIdMatch = null;
                MessageBatch notAMatch = null;
                for (Map.Entry<BatchId, MessageBatch> messageBatchEntry : uploadMessagesByBatchId.entrySet()) {
                    BatchId batchId = messageBatchEntry.getKey();
                    if (MPUtility.isEqual(batchId.getSessionId(), reportingMessage.getSessionId()) &&
                            MPUtility.isEqual(batchId.getMpid(), reportingMessage.getMpid())) {
                        match = messageBatchEntry.getValue();
                    } else if (MPUtility.isEqual(batchId.getSessionId(), reportingMessage.getSessionId())) {
                        sessionIdMatch = messageBatchEntry.getValue();
                    } else {
                        notAMatch = messageBatchEntry.getValue();
                    }
                }
                if (match == null) {
                    //if there's not matching by session id & mpid, use the first matching session id
                    match = sessionIdMatch;
                }
                if (match == null) {
                    //if there's no matching session id then just use the first batch object
                    match = notAMatch;
                }
                if (match != null) {
                    match.addReportingMessage(reportingMessage.getMsgObject());
                    InternalListenerManager.getListener().onCompositeObjects(reportingMessage, match);
                    ReportingService.deleteReportingMessage(db, reportingMessage.getReportingMessageId());
                }
            }
            List<JSONObject> deviceInfos = SessionService.processSessions(db, uploadMessagesByBatchId);
            for (JSONObject deviceInfo : deviceInfos) {
                deviceAttributes.updateDeviceInfo(mContext, deviceInfo);
            }
            createUploads(uploadMessagesByBatchId, db, deviceAttributes, configManager, currentSessionId, uploadSettings);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteMessagesAndSessions(String currentSessionId) {
        MPDatabase db = getDatabase();
        try {
            db.beginTransaction();
            MessageService.deleteOldMessages(db, currentSessionId);
            SessionService.deleteSessions(db, currentSessionId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private HashMap<BatchId, MessageBatch> getUploadMessageByBatchIdMap(List<MessageService.ReadyMessage> readyMessages, MPDatabase db, ConfigManager configManager) throws JSONException {
        return getUploadMessageByBatchIdMap(readyMessages, db, configManager, false);
    }

    private HashMap<BatchId, MessageBatch> getUploadMessageByBatchIdMap(List<MessageService.ReadyMessage> readyMessages, MPDatabase db, ConfigManager configManager, boolean markAsUpload) throws JSONException {
        HashMap<BatchId, MessageBatch> uploadMessagesByBatchId = new HashMap<BatchId, MessageBatch>();
        int highestUploadedMessageId = -1;
        for (MessageService.ReadyMessage readyMessage : readyMessages) {
            BatchId batchId = new BatchId(readyMessage);
            MessageBatch uploadMessage = uploadMessagesByBatchId.get(batchId);
            if (uploadMessage == null) {
                uploadMessage = createUploadMessage(configManager, true, batchId);
                uploadMessagesByBatchId.put(batchId, uploadMessage);
            }
            int messageLength = readyMessage.getMessage().length();
            JSONObject msgObject = new JSONObject(readyMessage.getMessage());
            if (messageLength + uploadMessage.getMessageLengthBytes() > Constants.LIMIT_MAX_UPLOAD_SIZE) {
                break;
            }
            uploadMessage.addMessage(msgObject);
            InternalListenerManager.getListener().onCompositeObjects(readyMessage, uploadMessage);
            uploadMessage.incrementMessageLengthBytes(messageLength);
            highestUploadedMessageId = readyMessage.getMessageId();
        }
        if (markAsUpload) {
            //Else mark the messages as uploaded, so next time around it'll be included in session history.
            MessageService.markMessagesAsUploaded(db, highestUploadedMessageId);
        } else {
            //If this is a session-less message, or if session history is disabled, just delete it.
            MessageService.deleteMessages(db, highestUploadedMessageId);
        }
        return uploadMessagesByBatchId;
    }

    private void createUploads(Map<BatchId, MessageBatch> uploadMessagesByBatchId, MPDatabase db, DeviceAttributes deviceAttributes, ConfigManager configManager, String currentSessionId, UploadSettings uploadSettings) {
        for (Map.Entry<BatchId, MessageBatch> messageBatchEntry : uploadMessagesByBatchId.entrySet()) {
            BatchId batchId = messageBatchEntry.getKey();
            MessageBatch uploadMessage = messageBatchEntry.getValue();
            if (uploadMessage != null) {
                String sessionId = batchId.getSessionId();

                //For upgrade scenarios, there may be no device or app customAttributes associated with the session, so create it now.
                if (uploadMessage.getAppInfo() == null) {
                    uploadMessage.setAppInfo(deviceAttributes.getAppInfo(mContext));
                }
                if (uploadMessage.getDeviceInfo() == null || sessionId.equals(currentSessionId)) {
                    uploadMessage.setDeviceInfo(deviceAttributes.getDeviceInfo(mContext));
                }
                JSONArray messages = uploadMessage.getMessages();
                JSONArray identities = findIdentityState(configManager, messages, batchId.getMpid());
                uploadMessage.setIdentities(identities);
                JSONObject userAttributes = findUserAttributeState(messages, batchId.getMpid());
                uploadMessage.setUserAttributes(userAttributes);

                JSONObject batch = uploadMessage;
                if (options != null && options.getBatchCreationListener() != null) {
                    try {
                        batch = options.getBatchCreationListener().onBatchCreated(batch);
                        if (batch == null || batch.length() == 0) {
                            Logger.error("Not uploading batch due to 'onCreateBatch' handler being empty");
                            return;
                        } else {
                            batch.put(Constants.MessageKey.MODIFIED_BATCH, true);
                        }
                    } catch (Exception e) {
                        Logger.error(e, "batch creation listener error, original batch will be uploaded");
                    }
                }

                UploadService.insertUpload(db, batch, uploadSettings);
                cleanSessions(currentSessionId);
            }
        }
    }

    /**
     * remove Session entries that do not have Message entries referencing them, and
     * are not the current Session
     *
     * @param currentSessionId
     */
    void cleanSessions(String currentSessionId) {
        MPDatabase database = getDatabase();
        Set<String> sessionIds = MessageService.getSessionIds(database);
        sessionIds.add(currentSessionId);
        SessionService.deleteSessions(database, sessionIds);
    }

    /**
     * Look for the last UAC message to find the end-state of user attributes.
     */
    private JSONObject findUserAttributeState(JSONArray messages, long mpId) {
        JSONObject userAttributes = null;
        if (messages != null) {
            for (int i = 0; i < messages.length(); i++) {
                try {
                    if (messages.getJSONObject(i).get(Constants.MessageKey.TYPE).equals(Constants.MessageType.USER_ATTRIBUTE_CHANGE)) {
                        userAttributes = messages.getJSONObject(i).getJSONObject(Constants.MessageKey.USER_ATTRIBUTES);
                        messages.getJSONObject(i).remove(Constants.MessageKey.USER_ATTRIBUTES);
                    }
                } catch (JSONException ignored) {
                } catch (NullPointerException ignored) {
                }
            }
        }
        if (userAttributes == null) {
            return getAllUserAttributesJson(mpId);
        } else {
            return userAttributes;
        }
    }

    /**
     * Look for the last UIC message to find the end-state of user identities.
     */
    private JSONArray findIdentityState(ConfigManager configManager, JSONArray messages, long mpId) {
        JSONArray identities = null;
        if (messages != null) {
            for (int i = 0; i < messages.length(); i++) {
                try {
                    if (messages.getJSONObject(i).get(Constants.MessageKey.TYPE).equals(Constants.MessageType.USER_IDENTITY_CHANGE)) {
                        identities = messages.getJSONObject(i).getJSONArray(Constants.MessageKey.USER_IDENTITIES);
                        messages.getJSONObject(i).remove(Constants.MessageKey.USER_IDENTITIES);
                    }
                } catch (JSONException ignored) {
                } catch (NullPointerException ignored) {
                }
            }
        }
        if (identities == null) {
            return configManager.getUserIdentityJson(mpId);
        } else {
            return identities;
        }
    }

    /**
     * Method that is responsible for building an upload message to be sent over the wire.
     **/
    private MessageBatch createUploadMessage(ConfigManager configManager, boolean history, BatchId batchId) throws JSONException {
        MessageBatch batchMessage = MessageBatch.create(
                history,
                configManager,
                configManager.getCookies(batchId.getMpid()),
                batchId);
        return batchMessage;
    }

    public void updateSessionEndTime(String sessionId, long endTime, long sessionLength) {
        SessionService.updateSessionEndTime(getDatabase(), sessionId, endTime, sessionLength);
    }

    public void updateSessionAttributes(String sessionId, String attributes) {
        SessionService.updateSessionAttributes(getDatabase(), sessionId, attributes);
    }

    public void updateSessionStatus(String sessionId, String status) {
        SessionService.updateSessionStatus(getDatabase(), sessionId, status);
    }

    public BaseMPMessage getSessionForSessionEndMessage(String sessionId, Location location, Set<Long> mpIds) throws JSONException {
        Cursor selectCursor = null;
        try {
            selectCursor = SessionService.getSessionForSessionEndMessage(getDatabase(), sessionId);
            BaseMPMessage endMessage = null;
            if (selectCursor.moveToFirst()) {
                long start = selectCursor.getLong(0);
                long end = selectCursor.getLong(1);
                long foregroundLength = selectCursor.getLong(2);
                String attributes = selectCursor.getString(3);
                JSONObject sessionAttributes = null;
                if (null != attributes) {
                    sessionAttributes = new JSONObject(attributes);
                }

                // Create a session-end message.
                endMessage = createMessageSessionEnd(sessionId, start, end, foregroundLength,
                        sessionAttributes, location, mpIds);
                endMessage.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
            }
            return endMessage;
        } finally {
            if (selectCursor != null && !selectCursor.isClosed()) {
                selectCursor.close();
            }
        }
    }

    BaseMPMessage createMessageSessionEnd(String sessionId, long start, long end, long foregroundLength, JSONObject sessionAttributes, Location location, Set<Long> mpIds) throws JSONException {
        int eventCounter = mPreferences.getInt(Constants.PrefKeys.EVENT_COUNTER, 0);
        resetEventCounter();
        InternalSession session = new InternalSession();
        session.mSessionID = sessionId;
        session.mSessionStartTime = start;
        JSONArray spanningMpids = new JSONArray();
        long storageMpid = Constants.TEMPORARY_MPID;
        for (Long mpid : mpIds) {
            //We do not need to associate a SessionEnd message with any particular MPID, as long as it is not == Constants.TEMPORARY_MPID.
            if (mpid != Constants.TEMPORARY_MPID) {
                spanningMpids.put(mpid);
                storageMpid = mpid;
            }
        }
        BaseMPMessage message = new BaseMPMessage.Builder(Constants.MessageType.SESSION_END)
                .timestamp(end)
                .attributes(sessionAttributes)
                .build(session, location, storageMpid);
        message.put(Constants.MessageKey.EVENT_COUNTER, eventCounter);
        message.put(Constants.MessageKey.SESSION_LENGTH, foregroundLength);
        message.put(Constants.MessageKey.SESSION_LENGTH_TOTAL, (end - start));
        message.put(Constants.MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo());
        message.put(Constants.MessageKey.SESSION_SPANNING_MPIDS, spanningMpids);
        return message;
    }

    private void resetEventCounter() {
        mPreferences.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, 0).apply();
    }


    public List<String> getOrphanSessionIds(String apiKey) {
        return SessionService.getOrphanSessionIds(getDatabase(), apiKey);
    }


    public void insertSession(BaseMPMessage message, String apiKey, JSONObject appInfo, JSONObject deviceInfo) throws JSONException {
        String appInfoString = appInfo.toString();
        String deviceInfoString = deviceInfo.toString();
        SessionService.insertSession(getDatabase(), message, apiKey, appInfoString, deviceInfoString, message.getMpId());
    }


    /**
     * Reporting Service Methods
     */

    public void insertReportingMessages(List<JsonReportingMessage> reportingMessages, long mpId) {
        MPDatabase db = getDatabase();
        try {
            db.beginTransaction();
            for (int i = 0; i < reportingMessages.size(); i++) {
                ReportingService.insertReportingMessage(db, reportingMessages.get(i), mpId);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.verbose("Error inserting reporting message: " + e.toString());
        } finally {
            db.endTransaction();
        }
    }


    /**
     * Upload Service Methods
     */


    public void cleanupUploadMessages() {
        UploadService.cleanupUploadMessages(getDatabase());
    }

    public List<ReadyUpload> getReadyUploads() {
        return UploadService.getReadyUploads(getDatabase());
    }

    public int deleteUpload(int id) {
        return UploadService.deleteUpload(getDatabase(), id);
    }

    public void insertAliasRequest(JSONObject request, UploadSettings uploadSettings) {
        UploadService.insertAliasRequest(getDatabase(), request, uploadSettings);
    }

    /**
     * UserAttribute Service Methods
     */

    public Map<String, Object> getUserAttributeSingles(long mpId) {
        if (getDatabase() != null) {
            Map<String, String> stringifiedAttributes = UserAttributesService.getUserAttributesSingles(getDatabase(), mpId);
            Map<String, Object> typedAttributes = new HashMap<>();
            for (Map.Entry<String, String> stringifiedAttribute : stringifiedAttributes.entrySet()) {
                String key = stringifiedAttribute.getKey();
                String value = stringifiedAttribute.getValue();
                typedAttributes.put(key, MPUtility.toNumberOrString(value));
            }
            return typedAttributes;
        }
        return null;
    }

    public TreeMap<String, List<String>> getUserAttributeLists(long mpId) {
        if (getDatabase() != null) {
            return UserAttributesService.getUserAttributesLists(getDatabase(), mpId);
        }
        return null;
    }


    public JSONObject getAllUserAttributesJson(long mpId) {
        Map<String, Object> attributes = getUserAttributes(null, mpId);
        JSONObject jsonAttributes = new JSONObject();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            if (entry.getValue() instanceof List) {
                List<String> attributeList = (List<String>) value;
                JSONArray jsonArray = new JSONArray();
                for (String attribute : attributeList) {
                    jsonArray.put(attribute);
                }
                try {
                    jsonAttributes.put(entry.getKey(), jsonArray);
                } catch (JSONException ignored) {
                }
            } else {
                try {
                    Object entryValue = entry.getValue();
                    if (entryValue == null) {
                        entryValue = JSONObject.NULL;
                    }
                    jsonAttributes.put(entry.getKey(), entryValue.toString());
                } catch (JSONException e) {

                }
            }
        }
        return jsonAttributes;
    }

    public Map<String, Object> getUserAttributes(long mpId) {
        return getUserAttributes(null, mpId);
    }

    public Map<String, Object> getUserAttributes(final UserAttributeListenerWrapper listener, final long mpId) {
        Map<String, Object> allUserAttributes = new HashMap<String, Object>();
        if (listener == null || Looper.getMainLooper() != Looper.myLooper()) {
            Map<String, Object> userAttributes = getUserAttributeSingles(mpId);
            Map<String, List<String>> userAttributeLists = getUserAttributeLists(mpId);
            if (listener != null) {
                listener.onUserAttributesReceived(userAttributes, userAttributeLists, mpId);
            }
            if (userAttributes != null) {
                allUserAttributes.putAll(userAttributes);
            }
            if (userAttributeLists != null) {
                allUserAttributes.putAll(userAttributeLists);
            }
            return allUserAttributes;
        } else {
            MParticle instance = MParticle.getInstance();
            if (instance != null) {
                instance.Internal().getMessageManager().getMessageHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        final Map<String, Object> attributeSingles = getUserAttributeSingles(mpId);
                        final Map<String, List<String>> attributeLists = getUserAttributeLists(mpId);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onUserAttributesReceived(attributeSingles, attributeLists, mpId);
                            }
                        });
                    }
                });
            }
            return null;
        }
    }

    public List<AttributionChange> setUserAttribute(UserAttributeResponse userAttribute) {
        List<AttributionChange> attributionChanges = new ArrayList<AttributionChange>();
        if (getDatabase() == null) {
            return attributionChanges;
        }
        Map<String, Object> currentValues = getUserAttributes(null, userAttribute.mpId);
        MPDatabase db = getDatabase();
        try {
            db.beginTransaction();
            long time = System.currentTimeMillis();
            if (userAttribute.attributeLists != null) {
                for (Map.Entry<String, List<String>> entry : userAttribute.attributeLists.entrySet()) {
                    String key = entry.getKey();
                    List<String> attributeValues = entry.getValue();
                    Object oldValue = currentValues.get(key);
                    if (oldValue != null && oldValue instanceof List && oldValue.equals(attributeValues)) {
                        continue;
                    }
                    int deleted = UserAttributesService.deleteAttributes(db, key, userAttribute.mpId);
                    boolean isNewAttribute = deleted == 0;
                    for (String attributeValue : attributeValues) {
                        UserAttributesService.insertAttribute(db, key, attributeValue, time, true, userAttribute.mpId);
                    }
                    attributionChanges.add(new AttributionChange(key, attributeValues, oldValue, false, isNewAttribute, userAttribute.time, userAttribute.mpId));
                }
            }
            if (userAttribute.attributeSingles != null) {
                for (Map.Entry<String, Object> entry : userAttribute.attributeSingles.entrySet()) {
                    String key = entry.getKey();
                    String attributeValue = null;
                    if (entry.getValue() != null) {
                        attributeValue = entry.getValue().toString();
                    }
                    Object oldValue = currentValues.get(key);
                    if (oldValue != null && oldValue instanceof String && ((String) oldValue).equalsIgnoreCase(attributeValue)) {
                        continue;
                    }
                    int deleted = UserAttributesService.deleteAttributes(db, key, userAttribute.mpId);
                    boolean isNewAttribute = deleted == 0;
                    UserAttributesService.insertAttribute(db, key, attributeValue, time, false, userAttribute.mpId);
                    attributionChanges.add(new AttributionChange(key, attributeValue, oldValue, false, isNewAttribute, userAttribute.time, userAttribute.mpId));
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.error(e, "Error while adding user attributes: ", e.toString());
        } finally {
            db.endTransaction();
        }
        return attributionChanges;
    }


    public void removeUserAttribute(UserAttributeRemoval container, MessageManagerCallbacks callbacks) {
        Map<String, Object> currentValues = getUserAttributes(null, container.mpId);
        MPDatabase db = getDatabase();
        try {
            db.beginTransaction();
            int deleted = UserAttributesService.deleteAttributes(db, container.key, container.mpId);
            if (callbacks != null && deleted > 0) {
                callbacks.attributeRemoved(container.key, container.mpId);
                callbacks.logUserAttributeChangeMessage(container.key, null, currentValues.get(container.key), true, false, container.time, container.mpId);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
        } finally {
            db.endTransaction();
        }
    }

    public void resetDatabaseForWorkspaceSwitching() {
        MPDatabase db = getDatabase();
        try {
            db.beginTransaction();
            BreadcrumbService.deleteAll(db);
            MessageService.deleteAll(db);
            ReportingService.deleteAll(db);
            SessionService.deleteAll(db);
            UserAttributesService.deleteAll(db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
        } finally {
            db.endTransaction();
        }
    }

    public static class AttributionChange {
        private String key;
        private Object newValue;
        private Object oldValue;
        private boolean deleted;
        private boolean isNewAttribute;
        private long time;
        private long mpId;

        public AttributionChange(String key, Object newValue, Object oldValue, boolean deleted, boolean isNewAttribute, long time, long mpId) {
            this.key = key;
            this.newValue = newValue;
            this.oldValue = oldValue;
            this.deleted = deleted;
            this.isNewAttribute = isNewAttribute;
            this.time = time;
            this.mpId = mpId;
        }

        public String getKey() {
            return key;
        }

        public Object getNewValue() {
            return newValue;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public boolean isNewAttribute() {
            return isNewAttribute;
        }

        public long getTime() {
            return time;
        }

        public long getMpId() {
            return mpId;
        }
    }

    public static class ReadyUpload {
        private int id;
        private String message;
        private boolean isAliasRequest;
        private UploadSettings uploadSettings;

        public ReadyUpload(int id, boolean isAliasRequest, String message, UploadSettings uploadSettings) {
            this.id = id;
            this.message = message;
            this.isAliasRequest = isAliasRequest;
            this.uploadSettings = uploadSettings;
        }

        public int getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }

        public boolean isAliasRequest() {
            return isAliasRequest;
        }

        public UploadSettings getUploadSettings() {
            return uploadSettings;
        }
    }

    public static class UserAttributeRemoval {
        public String key;
        public long time;
        public long mpId;
    }

    public static class UserAttributeResponse {
        public Map<String, Object> attributeSingles;
        public Map<String, List<String>> attributeLists;
        public long time;
        public long mpId;
    }
}
