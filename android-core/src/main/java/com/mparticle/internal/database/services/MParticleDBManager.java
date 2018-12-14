package com.mparticle.internal.database.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;

import com.mparticle.UserAttributeListener;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.DatabaseHelper;
import com.mparticle.internal.DeviceAttributes;
import com.mparticle.internal.JsonReportingMessage;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageBatch;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.MessageManagerCallbacks;
import com.mparticle.internal.InternalSession;

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

    public MParticleDBManager(Context context) {
        this.mContext = context;
        mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mDatabaseHelper = new DatabaseHelper(context);
    }

    /**
     * Creates a new SQLiteDatabase instance, if the Database has not been opened yet, and returns
     * an instance. Each instance is a singleton, per thread.
     * @return
     */
    public SQLiteDatabase getDatabase() {
        return mDatabaseHelper.getWritableDatabase();
    }

    public void updateMpId(long oldMpId, long newMpId) {
        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        new BreadcrumbService().updateMpId(db, oldMpId, newMpId);
        new MessageService().updateMpId(db, oldMpId, newMpId);
        new ReportingService().updateMpId(db, oldMpId, newMpId);
        new SessionService().updateMpId(db, oldMpId, newMpId);
        new UserAttributesService().updateMpId(db, oldMpId, newMpId);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     *
     *
     * Breadcumb Service Methods
     *
     *
     */


    public void insertBreadcrumb(MessageManager.BaseMPMessage message, String apiKey) throws JSONException {
        BreadcrumbService.insertBreadcrumb(getDatabase(), mContext, message, apiKey, message.getMpId());
    }

    public void appendBreadcrumbs(MessageManager.BaseMPMessage message) throws JSONException {
        JSONArray breadcrumbs = BreadcrumbService.getBreadcrumbs(getDatabase(), mContext, message.getMpId());
        if (!MPUtility.isEmpty(breadcrumbs)) {
            message.put(Constants.MessageType.BREADCRUMB, breadcrumbs);
        }
    }

    /**
     *
     *
     * Message Service Methods
     *
     *
     */

    public void cleanupMessages() {
        MessageService.cleanupMessages(getDatabase());
    }

    public void insertMessage(String apiKey, MessageManager.BaseMPMessage message) throws JSONException {
        MessageService.insertMessage(getDatabase(), apiKey, message, message.getMpId());
        if (sMessageListener != null) {
            sMessageListener.onMessageStored(message);
        }
    }

    private static MessageListener sMessageListener;

    static void setMessageListener(MessageListener messageListener){
        sMessageListener = messageListener;
    }

    public void updateSessionInstallReferrer(String sessionId, JSONObject appInfo) {
        SessionService.updateSessionInstallReferrer(getDatabase(), appInfo, sessionId);
    }

    public interface MessageListener {
        void onMessageStored(MessageManager.BaseMPMessage message);
    }

    /**
     *
     *
     * Prepare Messages for Upload
     *
     *
     */

    public void createSessionHistoryUploadMessage(ConfigManager configManager, DeviceAttributes deviceAttributes, String currentSessionId) throws JSONException {
        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        try {
            List<MessageService.ReadyMessage> readyMessages = MessageService.getSessionHistory(db, currentSessionId);
            if (readyMessages.size() <= 0) {
                db.setTransactionSuccessful();
                return;
            }

            HashMap<String, Map<Long, MessageBatch>> uploadMessagesBySessionMpid = getUploadMessageBySessionMpidMap(readyMessages, db, configManager, true);

            List<JSONObject> deviceInfos = SessionService.processSessions(db, uploadMessagesBySessionMpid);
            for (JSONObject deviceInfo : deviceInfos) {
                deviceAttributes.updateDeviceInfo(mContext, deviceInfo);
            }
            createUploads(uploadMessagesBySessionMpid, db, deviceAttributes, configManager, currentSessionId, true);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    public void createMessagesForUploadMessage(ConfigManager configManager, DeviceAttributes deviceAttributes, String currentSessionId, boolean sessionHistoryEnabled) throws JSONException {
        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        try {
            List<MessageService.ReadyMessage> readyMessages = MessageService.getMessagesForUpload(db);
            if (readyMessages.size() <= 0) {
                db.setTransactionSuccessful();
                return;
            }
            HashMap<String, Map<Long, MessageBatch>> uploadMessagesBySessionMpid = getUploadMessageBySessionMpidMap(readyMessages, db, configManager, false, sessionHistoryEnabled);

            List<ReportingService.ReportingMessage> reportingMessages = ReportingService.getReportingMessagesForUpload(db);
            for (ReportingService.ReportingMessage reportingMessage : reportingMessages) {
                Map<Long, MessageBatch> batchMap = uploadMessagesBySessionMpid.get(reportingMessage.getSessionId());
                if (batchMap == null) {
                    //if there's no matching session id then just use the first batch object
                    batchMap = uploadMessagesBySessionMpid.values().iterator().next();
                }
                MessageBatch batch = batchMap.get(reportingMessage.getMpid());
                if (batch == null) {
                    batch = batchMap.values().iterator().next();
                }
                if (batch != null) {
                    batch.addReportingMessage(reportingMessage.getMsgObject());
                }
                ReportingService.deleteReportingMessage(db, reportingMessage.getReportingMessageId());
            }
            List<JSONObject> deviceInfos = SessionService.processSessions(db, uploadMessagesBySessionMpid);
            for (JSONObject deviceInfo : deviceInfos) {
                deviceAttributes.updateDeviceInfo(mContext, deviceInfo);
            }
            createUploads(uploadMessagesBySessionMpid, db, deviceAttributes, configManager, currentSessionId, false, sessionHistoryEnabled);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    public void deleteMessagesAndSessions(String currentSessionId) {
        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        MessageService.deleteOldMessages(db, currentSessionId);
        SessionService.deleteSessions(db, currentSessionId);
        db.endTransaction();
    }

    private HashMap<String, Map<Long, MessageBatch>> getUploadMessageBySessionMpidMap(List<MessageService.ReadyMessage> readyMessages, SQLiteDatabase db, ConfigManager configManager, boolean isHistory) throws JSONException {
        return getUploadMessageBySessionMpidMap(readyMessages, db, configManager, isHistory, false);
    }

    private HashMap<String, Map<Long, MessageBatch>> getUploadMessageBySessionMpidMap(List<MessageService.ReadyMessage> readyMessages, SQLiteDatabase db, ConfigManager configManager, boolean isHistory, boolean markAsUpload) throws JSONException {
        HashMap<String, Map<Long, MessageBatch>> uploadMessagesBySessionMpid = new HashMap<String, Map<Long, MessageBatch>>(2);
        int highestUploadedMessageId = -1;
        for (MessageService.ReadyMessage readyMessage : readyMessages) {
            MessageBatch uploadMessage = null;
            Map<Long, MessageBatch> sessionMessageBatch = uploadMessagesBySessionMpid.get(readyMessage.getSessionId());
            if (sessionMessageBatch != null) {
                uploadMessage = sessionMessageBatch.get(readyMessage.getMpid());
            }
            if (uploadMessage == null) {
                uploadMessage = createUploadMessage(configManager, true, readyMessage.getMpid());
                if (uploadMessagesBySessionMpid.get(readyMessage.getSessionId()) == null) {
                    Map<Long, MessageBatch> messageBatchMap = new HashMap<Long, MessageBatch>();
                    messageBatchMap.put(readyMessage.getMpid(), uploadMessage);
                    uploadMessagesBySessionMpid.put(readyMessage.getSessionId(), messageBatchMap);
                } else {
                    uploadMessagesBySessionMpid.get(readyMessage.getSessionId()).put(readyMessage.getMpid(), uploadMessage);
                }
            }
            int messageLength = readyMessage.getMessage().length();
            JSONObject msgObject = new JSONObject(readyMessage.getMessage());
            if (messageLength + uploadMessage.getMessageLengthBytes() > Constants.LIMIT_MAX_UPLOAD_SIZE) {
                break;
            }
            if (isHistory) {
                uploadMessage.addSessionHistoryMessage(msgObject);
            } else {
                uploadMessage.addMessage(msgObject);
            }
            uploadMessage.incrementMessageLengthBytes(messageLength);
            highestUploadedMessageId = readyMessage.getMessageId();
        }
        if (markAsUpload) {
            //else mark the messages as uploaded, so next time around it'll be included in session history
            MessageService.markMessagesAsUploaded(db, highestUploadedMessageId);
        } else {
            //if this is a session-less message, or if session history is disabled, just delete it
            MessageService.deleteMessages(db, highestUploadedMessageId);
        }
        return uploadMessagesBySessionMpid;
    }

    private void createUploads(Map<String, Map<Long, MessageBatch>> uploadMessagesBySessionMpid, SQLiteDatabase db, DeviceAttributes deviceAttributes, ConfigManager configManager, String currentSessionId, boolean historyMessages) {
        createUploads(uploadMessagesBySessionMpid, db, deviceAttributes, configManager, currentSessionId, historyMessages, false);
    }

    private void createUploads(Map<String, Map<Long, MessageBatch>> uploadMessagesBySessionMpid, SQLiteDatabase db, DeviceAttributes deviceAttributes, ConfigManager configManager, String currentSessionId, boolean historyMessages, boolean sessionHistoryEnabled) {
        for (Map.Entry<String, Map<Long, MessageBatch>> session : uploadMessagesBySessionMpid.entrySet()) {
            for (Map.Entry<Long, MessageBatch> mpidMessage : session.getValue().entrySet()) {
                MessageBatch uploadMessage = mpidMessage.getValue();
                if (uploadMessage != null) {
                    String sessionId = session.getKey();
                    //for upgrade scenarios, there may be no device or app info associated with the session, so create it now.
                    if (uploadMessage.getAppInfo() == null) {
                        uploadMessage.setAppInfo(deviceAttributes.getAppInfo(mContext));
                    }
                    if (uploadMessage.getDeviceInfo() == null || sessionId.equals(currentSessionId)) {
                        uploadMessage.setDeviceInfo(deviceAttributes.getDeviceInfo(mContext));
                    }
                    JSONArray messages;
                    if (historyMessages) {
                        messages = uploadMessage.getSessionHistoryMessages();
                    } else {
                        messages = uploadMessage.getMessages();
                    }
                    JSONArray identities = findIdentityState(configManager, messages, mpidMessage.getKey());
                    uploadMessage.setIdentities(identities);
                    JSONObject userAttributes = findUserAttributeState(messages, mpidMessage.getKey());
                    uploadMessage.setUserAttributes(userAttributes);
                    UploadService.insertUpload(db, uploadMessage, configManager.getApiKey());
                    //if this was to process session history, or
                    //if we're never going to process history AND
                    //this batch contains a previous session, then delete the session
                    if (!historyMessages && !sessionHistoryEnabled && !sessionId.equals(currentSessionId)) {
                        SessionService.deleteSessions(db, currentSessionId);
                    }
                }
            }
        }
    }

    /**
     * Look for the last UAC message to find the end-state of user attributes
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
                }catch (JSONException jse) {

                }catch (NullPointerException npe) {

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
     * Look for the last UIC message to find the end-state of user identities
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
                }catch (JSONException jse) {

                }catch (NullPointerException npe) {

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
    private MessageBatch createUploadMessage(ConfigManager configManager, boolean history, long mpid) throws JSONException {
        MessageBatch batchMessage = MessageBatch.create(
                history,
                configManager,
                configManager.getCookies(mpid),
                mpid);
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

    public MessageManager.BaseMPMessage getSessionForSessionEndMessage(String sessionId, Location location, Set<Long> mpIds) throws JSONException {
        Cursor selectCursor = null;
        try {
            selectCursor = SessionService.getSessionForSessionEndMessage(getDatabase(), sessionId);
            MessageManager.BaseMPMessage endMessage = null;
            if (selectCursor.moveToFirst()) {
                long start = selectCursor.getLong(0);
                long end = selectCursor.getLong(1);
                long foregroundLength = selectCursor.getLong(2);
                String attributes = selectCursor.getString(3);
                JSONObject sessionAttributes = null;
                if (null != attributes) {
                    sessionAttributes = new JSONObject(attributes);
                }

                // create a session-end message
                endMessage = createMessageSessionEnd(sessionId, start, end, foregroundLength,
                        sessionAttributes, location, mpIds);
                endMessage.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
            }
            return endMessage;
        }
        finally {
            if (selectCursor != null && !selectCursor.isClosed()) {
                selectCursor.close();
            }
        }
    }

    MessageManager.BaseMPMessage createMessageSessionEnd(String sessionId, long start, long end, long foregroundLength, JSONObject sessionAttributes, Location location, Set<Long> mpIds) throws JSONException{
        int eventCounter = mPreferences.getInt(Constants.PrefKeys.EVENT_COUNTER, 0);
        resetEventCounter();
        InternalSession session = new InternalSession();
        session.mSessionID = sessionId;
        session.mSessionStartTime = start;
        JSONArray spanningMpids = new JSONArray();
        long storageMpid = Constants.TEMPORARY_MPID;
        for (Long mpid: mpIds) {
            //we do not need to associate a SessionEnd message with any particular MPID, as long as it is not == Constants.TEMPORARY_MPID
            if (mpid != Constants.TEMPORARY_MPID) {
                spanningMpids.put(mpid);
                storageMpid = mpid;
            }
        }
        MessageManager.BaseMPMessage message = new MessageManager.BaseMPMessage.Builder(Constants.MessageType.SESSION_END, session, location, storageMpid)
                .timestamp(end)
                .attributes(sessionAttributes)
                .build();
        message.put(Constants.MessageKey.EVENT_COUNTER, eventCounter);
        message.put(Constants.MessageKey.SESSION_LENGTH, foregroundLength);
        message.put(Constants.MessageKey.SESSION_LENGTH_TOTAL, (end - start));
        message.put(Constants.MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo());
        message.put(Constants.MessageKey.SESSION_SPANNING_MPIDS, spanningMpids);
        return message;
    }

    private void resetEventCounter(){
        mPreferences.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, 0).apply();
    }


    public List<String> getOrphanSessionIds(String apiKey) {
        return SessionService.getOrphanSessionIds(getDatabase(), apiKey);
    }


    public void insertSession(MessageManager.BaseMPMessage message, String apiKey, JSONObject appInfo, JSONObject deviceInfo) throws JSONException {
        String appInfoString = appInfo.toString();
        String deviceInfoString = deviceInfo.toString();
        SessionService.insertSession(getDatabase(), message, apiKey, appInfoString, deviceInfoString, message.getMpId());
    }


    /**
     *
     *
     * Reporting Service Methods
     *
     *
     */

    public void insertReportingMessages(List<JsonReportingMessage> reportingMessages, long mpId) {
        SQLiteDatabase db = getDatabase();
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
     *
     *
     * Upload Service Methods
     *
     *
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



    /**
     *
     *
     * UserAttribute Service Methods
     *
     *
     */

    public TreeMap<String, String> getUserAttributeSingles(long mpId) {
        if (getDatabase() != null) {
            return UserAttributesService.getUserAttributesSingles(getDatabase(), mpId);
        }
        return null;
    }

    public TreeMap<String, List<String>> getUserAttributeLists(long mpId) {
        if (getDatabase() != null) {
            return UserAttributesService.getUserAttributesLists(getDatabase(), mpId);
        }
        return null;
    }


    public JSONObject getAllUserAttributesJson(long mpId)  {
        Map<String, Object> attributes = getUserAttributes(null, mpId);
        JSONObject jsonAttributes = new JSONObject();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            if (entry.getValue() instanceof List) {
                List<String> attributeList = (List<String>)value;
                JSONArray jsonArray = new JSONArray();
                for (String attribute : attributeList) {
                    jsonArray.put(attribute);
                }
                try {
                    jsonAttributes.put(entry.getKey(), jsonArray);
                } catch (JSONException e) {

                }
            }else {
                try {
                    Object entryValue = entry.getValue();
                    if (entryValue == null) {
                        entryValue = JSONObject.NULL;
                    }
                    jsonAttributes.put(entry.getKey(), entryValue);
                } catch (JSONException e) {

                }
            }
        }
        return jsonAttributes;
    }

    public Map<String, Object> getUserAttributes(long mpId) {
        return getUserAttributes(null, mpId);
    }

    public Map<String, Object> getUserAttributes(final UserAttributeListener listener, final long mpId) {
        Map<String, Object> allUserAttributes = new HashMap<String, Object>();
        if (listener == null || Looper.getMainLooper() != Looper.myLooper()) {
            Map<String, String> userAttributes = getUserAttributeSingles(mpId);
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
        }else {
            new AsyncTask<Void, Void, UserAttributeResponse>() {
                @Override
                protected UserAttributeResponse doInBackground(Void... params) {
                    UserAttributeResponse response = new UserAttributeResponse();
                    response.attributeSingles = getUserAttributeSingles(mpId);
                    response.attributeLists = getUserAttributeLists(mpId);
                    return response;
                }

                @Override
                protected void onPostExecute(UserAttributeResponse attributes) {
                    if (listener != null) {
                        listener.onUserAttributesReceived(attributes.attributeSingles, attributes.attributeLists, mpId);
                    }
                }
            }.execute();
            return null;
        }
    }

    public List<AttributionChange> setUserAttribute(UserAttributeResponse userAttribute) {
        List<AttributionChange> attributionChanges = new ArrayList<AttributionChange>();
        if (getDatabase() == null){
            return attributionChanges;
        }
        Map<String, Object> currentValues = getUserAttributes(null, userAttribute.mpId);
        SQLiteDatabase db = getDatabase();
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
                for (Map.Entry<String, String> entry : userAttribute.attributeSingles.entrySet()) {
                    String key = entry.getKey();
                    String attributeValue = entry.getValue();
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
        }catch (Exception e){
            Logger.error(e, "Error while adding user attributes: ", e.toString());
        } finally {
            db.endTransaction();
        }
        return attributionChanges;
    }


    public void removeUserAttribute(UserAttributeRemoval container, MessageManagerCallbacks callbacks) {
        Map<String, Object> currentValues = getUserAttributes(null, container.mpId);
        SQLiteDatabase db = getDatabase();
        try {
            db.beginTransaction();
            int deleted = UserAttributesService.deleteAttributes(db, container.key, container.mpId);
            if (callbacks != null && deleted > 0) {
                callbacks.attributeRemoved(container.key, container.mpId);
                callbacks.logUserAttributeChangeMessage(container.key, null, currentValues.get(container.key), true, false, container.time, container.mpId);
            }
            db.setTransactionSuccessful();
        }catch (Exception e) {

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

        public ReadyUpload(int id, String message) {
            this.id = id;
            this.message = message;
        }


        public int getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class UserAttributeRemoval {
        public String key;
        public long time;
        public long mpId;
    }

    public static class UserAttributeResponse {
        public Map<String, String> attributeSingles;
        public Map<String, List<String>> attributeLists;
        public long time;
        public long mpId;
    }
}
