package com.mparticle.internal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.tables.mp.GcmMessageTable;
import com.mparticle.internal.dto.AttributionChangeDTO;
import com.mparticle.internal.dto.GcmMessageDTO;
import com.mparticle.internal.dto.UserAttributeRemoval;
import com.mparticle.internal.dto.UserAttributeResponse;
import com.mparticle.messaging.AbstractCloudMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/* package-private */ class MessageHandler extends Handler {

    private final Context mContext;

    private MParticleDBManager mMParticleDBManager;

    public static final int STORE_MESSAGE = 0;
    public static final int UPDATE_SESSION_ATTRIBUTES = 1;
    public static final int UPDATE_SESSION_END = 2;
    public static final int CREATE_SESSION_END_MESSAGE = 3;
    public static final int END_ORPHAN_SESSIONS = 4;
    public static final int STORE_BREADCRUMB = 5;
    public static final int STORE_GCM_MESSAGE = 6;
    public static final int MARK_INFLUENCE_OPEN_GCM = 7;
    public static final int CLEAR_PROVIDER_GCM = 8;
    public static final int STORE_REPORTING_MESSAGE_LIST = 9;
    public static final int REMOVE_USER_ATTRIBUTE = 10;
    public static final int SET_USER_ATTRIBUTE = 11;
    public static final int INCREMENT_USER_ATTRIBUTE = 12;

    private final MessageManagerCallbacks mMessageManagerCallbacks;

    public MessageHandler(Looper looper, MessageManagerCallbacks messageManager, Context context) {
        super(looper);
        mMessageManagerCallbacks = messageManager;
        mContext = context;
        mMParticleDBManager = new MParticleDBManager(context, DatabaseTables.getInstance(context));
    }

    private boolean databaseAvailable() {
        return mMParticleDBManager.isAvailable();
    }
    @Override
    public void handleMessage(Message msg) {
        if (!databaseAvailable()){
            return;
        }
        mMessageManagerCallbacks.delayedStart();
        switch (msg.what) {
            case STORE_MESSAGE:
                try {

                    MPMessage message = (MPMessage) msg.obj;
                    message.put(MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo());
                    String messageType = message.getString(MessageKey.TYPE);
                    // handle the special case of session-start by creating the
                    // session record first
                    if (MessageType.SESSION_START.equals(messageType)) {
                        dbInsertSession(message);
                    }else{
                        mMParticleDBManager.updateSessionEndTime(message.getSessionId(), message.getLong(MessageKey.TIMESTAMP), 0);
                        message.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
                    }
                    if (MessageType.ERROR.equals(messageType)){
                        mMParticleDBManager.appendBreadcrumbs(message);
                    }
                    if (MessageType.APP_STATE_TRANSITION.equals(messageType)){
                        appendLatestPushNotification(message);
                    }
                    if (MessageType.PUSH_RECEIVED.equals(messageType) &&
                            message.has(MessageKey.PUSH_BEHAVIOR) &&
                            !validateBehaviorFlags(message)){
                        return;
                    }
                    try {
                        mMParticleDBManager.insertMessage(mMessageManagerCallbacks.getApiKey(), message);
                    } catch (MParticleApiClientImpl.MPNoConfigException e) {
                        Logger.error("Unable to process uploads, API key and/or API Secret are missing");
                        return;
                    }

                    mMessageManagerCallbacks.checkForTrigger(message);

                } catch (Exception e) {
                    Logger.error(e, "Error saving message to mParticle DB.");
                }
                break;
            case UPDATE_SESSION_ATTRIBUTES:
                try {
                    JSONObject sessionAttributes = (JSONObject) msg.obj;
                    String sessionId = sessionAttributes.getString(MessageKey.SESSION_ID);
                    String attributes = sessionAttributes.getString(MessageKey.ATTRIBUTES);
                    mMParticleDBManager.updateSessionAttributes(sessionId, attributes);
                } catch (Exception e) {
                    Logger.error(e, "Error updating session attributes in mParticle DB.");
                }
                break;
            case UPDATE_SESSION_END:
                try {
                    Session session = (Session) msg.obj;
                    mMParticleDBManager.updateSessionEndTime(session.mSessionID, session.mLastEventTime, session.getForegroundTime());
                } catch (Exception e) {
                    Logger.error(e, "Error updating session end time in mParticle DB");
                }
                break;
            case CREATE_SESSION_END_MESSAGE:
                try {
                    Map.Entry<String, Set<Long>> entry = (Map.Entry<String, Set<Long>>) msg.obj;
                    MPMessage endMessage = null;
                   try {
                       endMessage = mMParticleDBManager.getSessionForSessionEndMessage(entry.getKey(), ((MessageManager)mMessageManagerCallbacks).getLocation(), entry.getValue());
                   }catch (JSONException jse){
                       Logger.warning("Failed to create mParticle session end message");
                   }
                   if (endMessage != null) {
                            // insert the record into messages with duration
                       try {
                           mMParticleDBManager.insertMessage(mMessageManagerCallbacks.getApiKey(), endMessage);
                       } catch (MParticleApiClientImpl.MPNoConfigException e) {
                           Logger.error("Unable to process uploads, API key and/or API Secret are missing");
                           return;
                       }

                    } else {
                        Logger.error("Error creating session end, no entry for sessionId in mParticle DB");
                    }
                    //1 means this came from ending the session
                    if (msg.arg1 == 1){
                        mMessageManagerCallbacks.endUploadLoop();
                    }
                } catch (Exception e) {
                    Logger.error(e, "Error creating session end message in mParticle DB");
                }finally {

                }
                break;
            case END_ORPHAN_SESSIONS:
                try {
                    // find left-over sessions that exist during startup and end them
                    List<String> sessionIds = mMParticleDBManager.getOrphanSessionIds(mMessageManagerCallbacks.getApiKey());
                    for (String sessionId: sessionIds) {
                        Map.Entry<String, Long> entry = new HashMap.SimpleEntry<String, Long>(sessionId, (Long)msg.obj);
                        sendMessage(obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, 0, 0, entry));
                    }
                } catch (MParticleApiClientImpl.MPNoConfigException ex) {
                    Logger.error("Unable to process initialization, API key and or API Secret is missing");
                } catch (Exception e) {
                    Logger.error(e, "Error processing initialization in mParticle DB");
                }
                break;
            case STORE_BREADCRUMB:
                try {
                    MPMessage message = (MPMessage) msg.obj;
                    message.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
                    try {
                        mMParticleDBManager.insertBreadcrumb(message, mMessageManagerCallbacks.getApiKey());
                    } catch (MParticleApiClientImpl.MPNoConfigException ex) {
                        Logger.error("Unable to process uploads, API key and/or API Secret are missing");
                    }
                } catch (Exception e) {
                    Logger.error(e, "Error saving breadcrumb to mParticle DB");
                }
                break;
            case STORE_GCM_MESSAGE:
                try {
                    AbstractCloudMessage message = (AbstractCloudMessage) msg.obj;
                    mMParticleDBManager.insertGcmMessage(message, msg.getData().getString(GcmMessageTable.APPSTATE));
                } catch (Exception e) {
                    Logger.error(e, "Error saving GCM message to mParticle DB", e.toString());
                }
                break;
            case MARK_INFLUENCE_OPEN_GCM:
                MessageManager.InfluenceOpenMessage message = (MessageManager.InfluenceOpenMessage) msg.obj;
                List<GcmMessageDTO> gcmMessageDTOs = mMParticleDBManager.logInfluenceOpenGcmMessages(message);
                for (GcmMessageDTO gcmMessageDTO: gcmMessageDTOs) {
                    mMessageManagerCallbacks.logNotification(gcmMessageDTO.getId(), gcmMessageDTO.getPayload(), null, gcmMessageDTO.getAppState(), AbstractCloudMessage.FLAG_INFLUENCE_OPEN);
                }
                break;
            case CLEAR_PROVIDER_GCM:
                try {
                    mMParticleDBManager.clearOldProviderGcm();
                }catch (Exception e){
                    Logger.error(e, "Error while clearing provider GCM messages: ", e.toString());
                }
                break;
            case STORE_REPORTING_MESSAGE_LIST:
                try{
                    List<JsonReportingMessage> reportingMessages = (List<JsonReportingMessage>)msg.obj;
                    mMParticleDBManager.insertReportingMessages(reportingMessages);
                }catch (Exception e) {
                    Logger.verbose(e, "Error while inserting reporting messages: ", e.toString());
                }
                break;
            case REMOVE_USER_ATTRIBUTE:
                try {
                    mMParticleDBManager.removeUserAttribute((UserAttributeRemoval)msg.obj, mMessageManagerCallbacks);
                }catch (Exception e) {
                    Logger.error(e, "Error while removing user attribute: ", e.toString());
                }
                break;
            case SET_USER_ATTRIBUTE:
                try {
                    setUserAttributes((UserAttributeResponse)msg.obj);
                } catch (Exception e) {
                    Logger.error(e, "Error while setting user attribute: ", e.toString());
                }
                break;
            case INCREMENT_USER_ATTRIBUTE:
                try {
                    Map.Entry<String, Long> obj = (Map.Entry<String, Long>)msg.obj;
                    incrementUserAttribute(obj.getKey(), msg.arg1, obj.getValue());
                } catch (Exception e) {
                    Logger.error(e, "Error while incrementing user attribute: ", e.toString());
                }
        }
    }

    void setUserAttributes(UserAttributeResponse response) {
        List<AttributionChangeDTO> attributionChangeDTOs = mMParticleDBManager.setUserAttribute(response);
        for (AttributionChangeDTO attributionChangeDTO: attributionChangeDTOs) {
            logUserAttributeChanged(attributionChangeDTO);
        }
    }

    private void incrementUserAttribute(String key, int incrementValue, long mpId) {
        TreeMap<String, String> userAttributes = mMParticleDBManager.getUserAttributeSingles(mpId);

        if (!userAttributes.containsKey(key)) {
            TreeMap<String, List<String>> userAttributeList = mMParticleDBManager.getUserAttributeLists(mpId);
            if (userAttributeList.containsKey(key)) {
                Logger.error("Error while attempting to increment user attribute - existing attribute is a list, which can't be incremented.");
                return;
            }
        }
        String newValue = null;
        String currentValue = userAttributes.get(key);
        if (currentValue == null) {
            newValue = Integer.toString(incrementValue);
        } else {
            try {
                newValue = Integer.toString(Integer.parseInt(currentValue) + incrementValue);
            }catch (NumberFormatException nfe) {
                Logger.error("Error while attempting to increment user attribute - existing attribute is not a number.");
                return;
            }
        }
        UserAttributeResponse wrapper = new UserAttributeResponse();
        wrapper.attributeSingles = new HashMap<String, String>(1);
        wrapper.attributeSingles.put(key, newValue);
        wrapper.mpId = mpId;
        List<AttributionChangeDTO> attributionChangeDTOs = mMParticleDBManager.setUserAttribute(wrapper);
        for (AttributionChangeDTO attributeChangeDTO: attributionChangeDTOs) {
            logUserAttributeChanged(attributeChangeDTO);
        }
        MParticle.getInstance().getKitManager().setUserAttribute(key, newValue);
    }

    private boolean validateBehaviorFlags(MPMessage message) {
        Cursor gcmCursor = null;
        boolean shouldInsert = true;
        int newBehavior = message.optInt(MessageKey.PUSH_BEHAVIOR);
        try {
            Logger.debug("Validating GCM behaviors...");
            long timestamp = 0;
            String contentId = Integer.toString(message.getInt(GcmMessageTable.CONTENT_ID));
            int currentBehaviors = mMParticleDBManager.getCurrentBehaviors(contentId);
            if (currentBehaviors >= 0) {

                //if we're trying to log a direct open, but the push has already been marked influence open, remove direct open from the new behavior
                if (((newBehavior & AbstractCloudMessage.FLAG_DIRECT_OPEN) == AbstractCloudMessage.FLAG_DIRECT_OPEN) &&
                        ((currentBehaviors & AbstractCloudMessage.FLAG_INFLUENCE_OPEN) == AbstractCloudMessage.FLAG_INFLUENCE_OPEN)) {
                    return false;
                }//if we're trying to log an influence open, but the push has already been marked direct open, remove influence open from the new behavior
                else if (((newBehavior & AbstractCloudMessage.FLAG_INFLUENCE_OPEN) == AbstractCloudMessage.FLAG_INFLUENCE_OPEN) &&
                        ((currentBehaviors & AbstractCloudMessage.FLAG_DIRECT_OPEN) == AbstractCloudMessage.FLAG_DIRECT_OPEN)) {
                    return false;
                }
                if ((currentBehaviors & AbstractCloudMessage.FLAG_RECEIVED) == AbstractCloudMessage.FLAG_RECEIVED ){
                    newBehavior &= ~AbstractCloudMessage.FLAG_RECEIVED;
                }
                if ((currentBehaviors & AbstractCloudMessage.FLAG_DISPLAYED) == AbstractCloudMessage.FLAG_DISPLAYED ){
                    newBehavior &= ~AbstractCloudMessage.FLAG_DISPLAYED;
                }

                if ((newBehavior & AbstractCloudMessage.FLAG_DISPLAYED) == AbstractCloudMessage.FLAG_DISPLAYED){
                    timestamp = message.getTimestamp();
                }
                message.put(MessageKey.PUSH_BEHAVIOR, newBehavior);

                if (newBehavior != currentBehaviors) {
                    int updated = mMParticleDBManager.updateGcmBehavior(newBehavior, timestamp, contentId);
                    if (updated > 0) {
                        Logger.debug("Updated GCM with content ID: " + message.getInt(GcmMessageTable.CONTENT_ID) + " and behavior(s): " + getBehaviorString(newBehavior));
                    }
                }else{
                    shouldInsert = false;
                }

            }
        } catch (Exception e) {
            Logger.debug(e, "Failed to update GCM message.");
        }finally {
            if (gcmCursor != null && !gcmCursor.isClosed()){
                gcmCursor.close();
            }
        }
        return shouldInsert;
    }

    private String getBehaviorString(int newBehavior){
        String behavior = "";
        if ((newBehavior & AbstractCloudMessage.FLAG_DIRECT_OPEN) == AbstractCloudMessage.FLAG_DIRECT_OPEN){
            behavior += "direct-open, ";
        }else if ((newBehavior & AbstractCloudMessage.FLAG_INFLUENCE_OPEN) == AbstractCloudMessage.FLAG_INFLUENCE_OPEN){
            behavior += "influence-open, ";
        }else if ((newBehavior & AbstractCloudMessage.FLAG_RECEIVED) == AbstractCloudMessage.FLAG_RECEIVED){
            behavior += "received, ";
        }else if ((newBehavior & AbstractCloudMessage.FLAG_DISPLAYED) == AbstractCloudMessage.FLAG_DISPLAYED){
            behavior += "displayed, ";
        }
        return behavior;
    }

    private void appendLatestPushNotification(MPMessage message) {
        String payload = mMParticleDBManager.getPayload();
        try {
            message.put(MessageKey.PAYLOAD, payload);
        } catch (Exception e) {
            Logger.debug("Failed to append latest push notification payload: " + e.toString());
        }
    }

    private void dbInsertSession(MPMessage message) throws JSONException {
        try {
            DeviceAttributes deviceAttributes =  mMessageManagerCallbacks.getDeviceAttributes();
            mMParticleDBManager.insertSession(message, mMessageManagerCallbacks.getApiKey(), deviceAttributes.getAppInfo(mContext), deviceAttributes.getDeviceInfo(mContext));
        } catch (MParticleApiClientImpl.MPNoConfigException ex) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing");
        }
    }

    private void logUserAttributeChanged(AttributionChangeDTO attributionChangeDTO) {
        mMessageManagerCallbacks.logUserAttributeChangeMessage(
                attributionChangeDTO.getKey(),
                attributionChangeDTO.getNewValue(),
                attributionChangeDTO.getOldValue(),
                attributionChangeDTO.isDeleted(),
                attributionChangeDTO.isNewAttribute(),
                attributionChangeDTO.getTime(),
                attributionChangeDTO.getMpId());
    }

}
