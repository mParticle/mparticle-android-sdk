package com.mparticle.identity;

import android.os.Build;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.KitKatHelper;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.segmentation.SegmentListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** package-private **/class MParticleUserDelegate {
    private MParticleDBManager mMParticleDBManager;
    private AppStateManager mAppStateManager;
    private ConfigManager mConfigManager;
    private MessageManager mMessageManager;
    private KitManager mKitManager;

    MParticleUserDelegate(AppStateManager appStateManager, ConfigManager configManager, MessageManager messageManager, KitManager kitManager, MParticleDBManager dbManager) {
        mAppStateManager = appStateManager;
        mConfigManager = configManager;
        mMessageManager = messageManager;
        mKitManager = kitManager;
        mMParticleDBManager = dbManager;
    }

    public Map<String, Object> getUserAttributes(long mpId) {
        return mMessageManager.getUserAttributes(null, mpId);
    }

    public Map<String, Object> getUserAttributes(final UserAttributeListener listener, long mpId) {
        return mMessageManager.getUserAttributes(listener, mpId);
    }

    public Map<MParticle.IdentityType, String> getUserIdentities(long mpId){
        return mMessageManager.getUserIdentities(mpId);
    }

    public boolean setUserIdentity(String id, MParticle.IdentityType identityType, long mpId) {
        if (identityType != null) {
            if (id == null) {
                Logger.debug("Removing User Identity type: " + identityType.name());
            } else {
                Logger.debug("Setting User Identity: " + id);
            }

            if (!MPUtility.isEmpty(id) && id.length() > Constants.LIMIT_ATTR_VALUE) {
                Logger.warning("User Identity value length exceeds limit. Will not set id: " + id);
                return false;
            }

            JSONArray userIdentities = mMessageManager.getUserIdentityJson(mpId);
            JSONObject oldIdentity = null;
            try {
                int index = -1;
                for (int i = 0; i < userIdentities.length(); i++) {
                    if (identityType.getValue() == userIdentities.getJSONObject(i).optInt(MessageKey.IDENTITY_NAME)) {
                        oldIdentity = userIdentities.getJSONObject(i);
                        index = i;
                        break;
                    }
                }


                boolean idChanged = true;

                JSONObject newObject = null;
                if (id != null) {
                    newObject = new JSONObject();
                    newObject.put(MessageKey.IDENTITY_NAME, identityType.getValue());
                    newObject.put(MessageKey.IDENTITY_VALUE, id);
                    if (oldIdentity != null) {
                        idChanged = !id.equals(oldIdentity.optString(MessageKey.IDENTITY_VALUE));
                        newObject.put(MessageKey.IDENTITY_DATE_FIRST_SEEN, oldIdentity.optLong(MessageKey.IDENTITY_DATE_FIRST_SEEN, System.currentTimeMillis()));
                        newObject.put(MessageKey.IDENTITY_FIRST_SEEN, false);
                        userIdentities.put(index, newObject);
                    } else {
                        newObject.put(MessageKey.IDENTITY_DATE_FIRST_SEEN, System.currentTimeMillis());
                        newObject.put(MessageKey.IDENTITY_FIRST_SEEN, true);
                        userIdentities.put(newObject);
                    }
                } else {
                    if (oldIdentity == null || index < 0) {
                        Logger.debug("Attempted to remove ID type that didn't exist: " + identityType.name());
                        return false;
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        KitKatHelper.remove(userIdentities, index);
                    } else {
                        JSONArray newIdentities = new JSONArray();
                        for (int i = 0; i < userIdentities.length(); i++) {
                            if (i != index) {
                                newIdentities.put(userIdentities.get(i));
                            }
                        }
                        userIdentities = newIdentities;
                    }
                }
                if (idChanged) {
                    mMessageManager.logUserIdentityChangeMessage(newObject, oldIdentity, userIdentities, mpId);
                }

                if (id == null) {
                    mKitManager.removeUserIdentity(identityType);
                } else {
                    mKitManager.setUserIdentity(id, identityType);
                }
                return true;
            } catch (JSONException e) {
                Logger.error("Error setting identity: " + id);
            }
        }
        return false;
    }

    public boolean setUserAttribute(String key, Object value, long userMpId) {
        return setUserAttribute(key, value, userMpId, false);
    }

    /**
     * setUserAttributes is an asynchronous method, and should normally be run as such. In certain
     * cases, like when an IdentityApiRequest has copyUserAttributes set to true, we need to set the
     * user attributes on the new user synchronously, and only then should we use this flag
     *
     */
    boolean setUserAttribute(String key, Object value, long userMpId, boolean synchronously) {
        if (mConfigManager.isEnabled() && mAppStateManager.getSession().checkEventLimit()) {
            MParticle.getInstance().getAppStateManager().ensureActiveSession();
            if (MPUtility.isEmpty(key)) {
                Logger.warning("setUserAttribute called with null key. This is a noop.");
                return false;
            }
            if (key.length() > Constants.LIMIT_ATTR_NAME) {
                Logger.warning("User attribute keys cannot be longer than " + Constants.LIMIT_ATTR_NAME + " characters, attribute not set: " + key);
                return false;
            }

            if (value != null && value instanceof List) {
                List<Object> values = (List<Object>) value;
                if (values.size() > Constants.LIMIT_USER_ATTR_LIST_LENGTH) {
                    Logger.warning("setUserAttribute called with list longer than " + Constants.LIMIT_USER_ATTR_LIST_LENGTH + " elements, list not set.");
                    return false;
                }
                List<String> clonedList = new ArrayList<String>();
                try {
                    for (int i = 0; i < values.size(); i++) {
                        if (values.get(i).toString().length() > Constants.LIMIT_USER_ATTR_LIST_ITEM_LENGTH) {
                            Logger.warning("setUserAttribute called with list containing element longer than " + Constants.LIMIT_USER_ATTR_LIST_ITEM_LENGTH + " characters, dropping entire list.");
                            return false;
                        } else {
                            clonedList.add(values.get(i).toString());
                        }
                    }
                    Logger.warning("Set user attribute list: " + key + " with values: " + values.toString());
                    mMessageManager.setUserAttribute(key, clonedList, userMpId, synchronously);
                    mKitManager.setUserAttributeList(key, clonedList);
                } catch (Exception e) {
                    Logger.warning("Error while setting attribute list: " + e.toString());
                    return false;
                }
            } else {
                String stringValue = null;
                if (value != null) {
                    stringValue = value.toString();
                    if (stringValue.length() > Constants.LIMIT_USER_ATTR_VALUE) {
                        Logger.warning("setUserAttribute called with stringvalue longer than " + Constants.LIMIT_USER_ATTR_VALUE + " characters. Attribute not set.");
                        return false;
                    }
                    Logger.debug("Set user attribute: " + key + " with value: " + stringValue);
                } else {
                    Logger.debug("Set user tag: " + key);
                }
                mMessageManager.setUserAttribute(key, stringValue, userMpId, synchronously);
                mKitManager.setUserAttribute(key, stringValue);
            }
            return true;
        }
        return false;
    }

    public boolean setUserAttributeList(String key, Object value, long userMpId) {
        if (value == null) {
            Logger.warning("setUserAttributeList called with null list, this is a no-op.");
            return false;
        }
        return setUserAttribute(key, value, userMpId);
    }

    public boolean incrementUserAttribute(String key, int value, long userMpId) {
        if (key == null) {
            Logger.warning("incrementUserAttribute called with null key. Ignoring...");
            return false;
        }
        Logger.debug("Incrementing user attribute: " + key + " with value " + value);
        mMessageManager.incrementUserAttribute(key, value, userMpId);
        return true;
    }

    public boolean removeUserAttribute(String key, long userMpId) {
        if (MPUtility.isEmpty(key)) {
            Logger.debug("removeUserAttribute called with empty key.");
            return false;
        }
        Logger.debug("Removing user attribute: \"" + key + "\" for mpId: " + userMpId);
        mMessageManager.removeUserAttribute(key, userMpId);
        mKitManager.removeUserAttribute(key);
        return true;
    }

    public void getSegments(long timeout, String endpointId, SegmentListener listener) {
        if (mMessageManager != null && mMessageManager.mUploadHandler != null) {
            mMessageManager.mUploadHandler.fetchSegments(timeout, endpointId, listener);
        }
    }


    /**
     * to be used to rectify the use of the placeholder MPID, which occured while request which might
     * change the MPID was in progress. The parameter "mpid" can either be the previous MPID or a new MPID,
     * either way, everything stored under the placeholder MPID will be migrated.
     */
    boolean setUser(long previousMpid, long newMpid, Map<MParticle.IdentityType, String> identities, boolean shouldCopyAttributes) {
        boolean success = true;
        if (shouldCopyAttributes && previousMpid != newMpid) {
            for (Map.Entry<String, Object> entry: mMParticleDBManager.getUserAttributes(previousMpid).entrySet()) {
                if (!setUserAttribute(entry.getKey(), entry.getValue(), newMpid, true)) {
                    success = false;
                }
            }
        }
        if (identities != null) {
            for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
                if (!MPUtility.isEmpty(entry.getValue())) {
                    setUserIdentity(entry.getValue(), entry.getKey(), newMpid);
                }
            }
        }
        // if the mpid remains equal to the temporary_mpid, as the case could be when a network request fails
        // or on startup, then there is not reason to do anything
        if (newMpid == Constants.TEMPORARY_MPID) {
            return false;
        }
        mConfigManager.mergeUserConfigs(Constants.TEMPORARY_MPID, newMpid);
        mMessageManager.getMParticleDBManager().updateMpId(Constants.TEMPORARY_MPID, newMpid);
        mConfigManager.deleteUserConfig(Constants.TEMPORARY_MPID);
        mConfigManager.setMpid(newMpid);
        return success;

    }
}