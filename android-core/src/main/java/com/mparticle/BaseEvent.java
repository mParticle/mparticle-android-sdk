package com.mparticle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.messages.BaseMPMessageBuilder;

import org.json.JSONArray;

import java.util.List;
import java.util.Map;

public class BaseEvent {
    private MessageType mType;
    private Map<String, List<String>> mCustomFlags;
    private Map<String, String> mCustomAttributes;

    protected BaseEvent(MessageType type) {
        mType = type;
    }


    @NonNull
    public MessageType getType() {
        return mType;
    }

    protected void setType(MessageType type) {
        this.mType = type;
    }

    /**
     * Retrieve the custom flags set on this event. Custom Flags are used to send data or trigger behavior
     * to individual 3rd-party services that you have enabled for your app. By default, flags are not forwarded
     * to any providers.
     *
     * @return returns the map of custom flags, or null if none are set
     */
    @Nullable
    public Map<String, List<String>> getCustomFlags() {
        return mCustomFlags;
    }


    protected void setCustomFlags(@Nullable Map<String, List<String>> flags) {
        if (flags != null && MPUtility.containsNullKey(flags)) {
            Logger.warning(String.format("disregarding \"MPEvent.customFlag\" value of %s. Key was found to be null", new JSONArray(flags.get(null))));
            flags.remove(null);
        }
        this.mCustomFlags = flags;
    }

    /**
     * Retrieve the Map of custom attributes of the event.
     *
     * @return returns a Map of custom attributes, or null if no custom attributes are set.
     */
    @Nullable
    public Map<String, String> getCustomAttributes() {
        return mCustomAttributes;
    }

    protected void setCustomAttributes(@Nullable Map<String, String> customAttributes) {
        if (customAttributes != null && MPUtility.containsNullKey(customAttributes)) {
            Logger.warning(String.format("disregarding \"Event.customFlag\" value of \"%s\". Key was found to be null", customAttributes.get(null)));
            customAttributes.remove(null);
        }
        this.mCustomAttributes = customAttributes;
    }

    public BaseMPMessageBuilder getMessage() {
        return new BaseMPMessageBuilder(getType().getMessageType());
    }

    public interface MessageType {
        String getMessageType();
    }

    public enum Type implements MessageType {
        SESSION_START("ss"),
        SESSION_END("se"),
        EVENT("e"),
        SCREEN_VIEW("v"),
        COMMERCE_EVENT("cm"),
        OPT_OUT("o"),
        ERROR("x"),
        PUSH_REGISTRATION("pr"),
        REQUEST_HEADER("h"),
        FIRST_RUN("fr"),
        APP_STATE_TRANSITION("ast"),
        PUSH_RECEIVED("pm"),
        BREADCRUMB("bc"),
        NETWORK_PERFORMNACE("npe"),
        PROFILE("pro"),
        USER_ATTRIBUTE_CHANGE("uac"),
        USER_IDENTITY_CHANGE("uic"),
        //place holder, should NOT be sent to server
        MEDIA("media_event");

        private String messageType;

        Type(String messageType) {
            this.messageType = messageType;
        }

        public String getMessageType() {
            return messageType;
        }
    }

}
