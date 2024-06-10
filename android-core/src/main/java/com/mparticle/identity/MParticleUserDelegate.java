package com.mparticle.identity;

import android.content.Context;
import android.os.Build;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListenerType;
import com.mparticle.audience.AudienceResponse;
import com.mparticle.audience.AudienceTask;
import com.mparticle.audience.BaseAudienceTask;
import com.mparticle.consent.ConsentState;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.KitKatHelper;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * package-private
 **/
class MParticleUserDelegate {
    private AppStateManager mAppStateManager;
    private ConfigManager mConfigManager;
    private MessageManager mMessageManager;
    private KitManager mKitManager;

    MParticleUserDelegate(AppStateManager appStateManager, ConfigManager configManager, MessageManager messageManager, KitManager kitManager) {
        mAppStateManager = appStateManager;
        mConfigManager = configManager;
        mMessageManager = messageManager;
        mKitManager = kitManager;
    }

    public Map<String, Object> getUserAttributes(long mpId) {
        return mMessageManager.getUserAttributes(null, mpId);
    }

    public Map<String, Object> getUserAttributes(final UserAttributeListenerType listener, long mpId) {
        return mMessageManager.getUserAttributes(new UserAttributeListenerWrapper(listener), mpId);
    }

    public Map<MParticle.IdentityType, String> getUserIdentities(long mpId) {
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
     */
    boolean setUserAttribute(String key, Object value, long userMpId, boolean synchronously) {
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            if (MPUtility.isEmpty(key)) {
                Logger.warning("Error while setting user attribute - called with null key. This is a no-op.");
                return false;
            }
            if (key.length() > Constants.LIMIT_ATTR_KEY) {
                Logger.warning("Error while setting user attribute - attribute keys cannot be longer than " + Constants.LIMIT_ATTR_KEY + " characters. Attribute not set: " + key);
                return false;
            }

            if (value instanceof List) {
                List<Object> values = (List<Object>) value;

                List<String> clonedList = new ArrayList<String>();
                try {
                    int totalLength = 0;
                    for (int i = 0; i < values.size(); i++) {
                        totalLength += values.get(i).toString().length();
                        if (totalLength > Constants.LIMIT_ATTR_VALUE) {
                            Logger.warning("Error while setting user attribute - attribute lists cannot contain values of combined length greater than " + Constants.LIMIT_ATTR_VALUE + " characters. Attribute not set.");
                            return false;
                        } else {
                            clonedList.add(values.get(i).toString());
                        }
                    }
                    Logger.debug("Setting user attribute list: " + key + " with values: " + values.toString());
                    mMessageManager.setUserAttribute(key, clonedList, userMpId, synchronously);
                    mKitManager.setUserAttributeList(key, clonedList, userMpId);
                } catch (Exception e) {
                    Logger.warning("Error while setting user attribute - " + e.toString());
                    return false;
                }
            } else {
                String stringValue = null;
                if (value != null) {
                    stringValue = value.toString();
                    if (stringValue.length() > Constants.LIMIT_ATTR_VALUE) {
                        Logger.warning("Error while setting user attribute - attribute values cannot be longer than " + Constants.LIMIT_ATTR_VALUE + " characters. Attribute not set.");
                        return false;
                    }
                    Logger.debug("Setting user attribute: " + key + " with value: " + stringValue);
                    mMessageManager.setUserAttribute(key, stringValue, userMpId, synchronously);
                    mKitManager.setUserAttribute(key, stringValue, userMpId);
                } else {
                    Logger.debug("Setting user tag: " + key);
                    mMessageManager.setUserAttribute(key, stringValue, userMpId, synchronously);
                    mKitManager.setUserTag(key, userMpId);
                }
            }
            return true;
        }
        return false;
    }

    public boolean setUserAttributeList(String key, Object value, long userMpId) {
        if (value == null) {
            Logger.warning("setUserAttributeList called with a null list, this is a no-op.");
            return false;
        }
        return setUserAttribute(key, value, userMpId);
    }

    public boolean incrementUserAttribute(String key, Number value, long userMpId) {
        if (key == null) {
            Logger.warning("incrementUserAttribute called with a null key. Ignoring...");
            return false;
        }
        Logger.debug("Incrementing user attribute: " + key + " with value " + value);
        mMessageManager.incrementUserAttribute(key, value, userMpId);
        return true;
    }

    public boolean removeUserAttribute(String key, long userMpId) {
        if (MPUtility.isEmpty(key)) {
            Logger.debug("removeUserAttribute called with an empty key.");
            return false;
        }
        Logger.debug("Removing user attribute: \"" + key + "\" for mpId: " + userMpId);
        mMessageManager.removeUserAttribute(key, userMpId);
        mKitManager.removeUserAttribute(key, userMpId);
        return true;
    }


    static void setUserIdentities(MParticleUserDelegate userDelegate, Map<MParticle.IdentityType, String> identities, long mpid) {
        if (identities != null) {
            //some legacy consumers of Identity (kits) require customer ID first
            //this is a mitigation for those that have not adopted the new Identity API interfaces.
            if (identities.containsKey(MParticle.IdentityType.CustomerId)) {
                userDelegate.setUserIdentity(identities.get(MParticle.IdentityType.CustomerId), MParticle.IdentityType.CustomerId, mpid);
            }
            if (identities.containsKey(MParticle.IdentityType.Email)) {
                userDelegate.setUserIdentity(identities.get(MParticle.IdentityType.Email), MParticle.IdentityType.Email, mpid);
            }

            for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
                MParticle.IdentityType identityType = entry.getKey();
                if (identityType != MParticle.IdentityType.CustomerId && identityType != MParticle.IdentityType.Email) {
                    String value = entry.getValue();
                    userDelegate.setUserIdentity(value, identityType, mpid);
                }
            }
        }
    }

    boolean setUser(Context context, long previousMpid, long newMpid, Map<MParticle.IdentityType, String> identities, UserAliasHandler userAliasHandler, boolean isLoggedIn) {
        setUserIdentities(this, identities, newMpid);
        // if the mpid remains equal to the temporary_mpid, as the case could be when a network request fails
        // or on startup, then there is no reason to do anything
        if (newMpid == Constants.TEMPORARY_MPID) {
            return false;
        }
        mConfigManager.mergeUserConfigs(Constants.TEMPORARY_MPID, newMpid);
        mMessageManager.getMParticleDBManager().updateMpId(Constants.TEMPORARY_MPID, newMpid);
        mConfigManager.deleteUserStorage(Constants.TEMPORARY_MPID);

        if (userAliasHandler != null && previousMpid != newMpid) {
            try {
                userAliasHandler.onUserAlias(
                        MParticleUserImpl.getInstance(context, previousMpid, this),
                        MParticleUserImpl.getInstance(context, newMpid, this)
                );
            } catch (Exception e) {
                Logger.error("Error while executing UserAliasHandler: " + e.toString());
            }
        }
        mConfigManager.setMpid(newMpid, isLoggedIn);
        return true;

    }

    public ConsentState getConsentState(long mpid) {
        return mConfigManager.getConsentState(mpid);
    }

    public void setConsentState(ConsentState state, long mpid) {
        ConsentState oldState = getConsentState(mpid);
        mConfigManager.setConsentState(state, mpid);
        mKitManager.onConsentStateUpdated(oldState, state, mpid);
    }

    public boolean isLoggedIn(Long mpid) {
        return mConfigManager.getUserStorage(mpid).isLoggedIn();
    }

    public long getFirstSeenTime(Long mpid) {
        return mConfigManager.getUserStorage(mpid).getFirstSeenTime();
    }

    public long getLastSeenTime(Long mpid) {
        if (mpid == mConfigManager.getMpid()) {
            return System.currentTimeMillis();
        } else {
            return mConfigManager.getUserStorage(mpid).getLastSeenTime();
        }
    }

    public AudienceTask<AudienceResponse> getUserAudiences(long mpId) {
        if (mMessageManager != null && mMessageManager.mUploadHandler != null) {
            return mMessageManager.mUploadHandler.fetchUserAudiences(mpId);
        } else {
            BaseAudienceTask task = new BaseAudienceTask();
            task.setFailed(new AudienceResponse(IdentityApi.UNKNOWN_ERROR,
                    "Error while fetching user audiences"));
            return task;
        }
    }
}