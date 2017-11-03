package com.mparticle.internal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.dto.AttributionChange;
import com.mparticle.internal.dto.UserAttributeRemoval;
import com.mparticle.internal.dto.UserAttributeResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
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
    public static final int STORE_REPORTING_MESSAGE_LIST = 9;
    public static final int REMOVE_USER_ATTRIBUTE = 10;
    public static final int SET_USER_ATTRIBUTE = 11;
    public static final int INCREMENT_USER_ATTRIBUTE = 12;
    public static final int INSTALL_REFERRER_UPDATED = 13;

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
        try {
            if (!databaseAvailable()) {
                return;
            }
            mMessageManagerCallbacks.delayedStart();
        }catch (Exception e) {
            Logger.verbose(e.toString());
        }
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
            case INSTALL_REFERRER_UPDATED:
                try {
                    mMParticleDBManager.updateSessionInstallReferrer((String) msg.obj, mMessageManagerCallbacks.getDeviceAttributes().getAppInfo(mContext, true));
                }catch (Exception e) {
                    Logger.error(e, "Error updating session attributes in mParticle DB.");
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
                    Long mpid = (Long)msg.obj;
                    List<String> sessionIds = mMParticleDBManager.getOrphanSessionIds(mMessageManagerCallbacks.getApiKey());
                    for (String sessionId: sessionIds) {
                        Map.Entry<String, Set<Long>> entry = new HashMap.SimpleEntry<String, Set<Long>>(sessionId, Collections.singleton(mpid));
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
            case STORE_REPORTING_MESSAGE_LIST:
                try{
                    MessageManager.ReportingMpidMessage reportingMessages = (MessageManager.ReportingMpidMessage)msg.obj;
                    mMParticleDBManager.insertReportingMessages((List<JsonReportingMessage>)reportingMessages.reportingMessages, reportingMessages.mpid);
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
        List<AttributionChange> attributionChanges = mMParticleDBManager.setUserAttribute(response);
        for (AttributionChange attributionChange : attributionChanges) {
            logUserAttributeChanged(attributionChange);
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
        List<AttributionChange> attributionChanges = mMParticleDBManager.setUserAttribute(wrapper);
        for (AttributionChange attributeChange: attributionChanges) {
            logUserAttributeChanged(attributeChange);
        }
        MParticle.getInstance().getKitManager().setUserAttribute(key, newValue);
    }

    private void dbInsertSession(MPMessage message) throws JSONException {
        try {
            DeviceAttributes deviceAttributes =  mMessageManagerCallbacks.getDeviceAttributes();
            mMParticleDBManager.insertSession(message, mMessageManagerCallbacks.getApiKey(), deviceAttributes.getAppInfo(mContext), deviceAttributes.getDeviceInfo(mContext));
        } catch (MParticleApiClientImpl.MPNoConfigException ex) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing");
        }
    }

    private void logUserAttributeChanged(AttributionChange attributionChange) {
        mMessageManagerCallbacks.logUserAttributeChangeMessage(
                attributionChange.getKey(),
                attributionChange.getNewValue(),
                attributionChange.getOldValue(),
                attributionChange.isDeleted(),
                attributionChange.isNewAttribute(),
                attributionChange.getTime(),
                attributionChange.getMpId());
    }

}
