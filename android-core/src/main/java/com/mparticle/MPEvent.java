package com.mparticle;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.internal.messages.BaseMPMessageBuilder;
import com.mparticle.internal.messages.MPEventMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 *
 * Class representation of an event.
 *
 * <code>MPEvent</code> implements the Builder pattern, see {@link com.mparticle.MPEvent.Builder} for more information.
 *
 * @see com.mparticle.MParticle#logEvent(BaseEvent)
 *
 */
public class MPEvent extends BaseEvent {
    private MParticle.EventType eventType;
    private String eventName;
    private String category;
    private Double duration = null, startTime = null, endTime = null;
    private int eventHash;
    private boolean entering = true;
    private boolean screenEvent;

    private MPEvent(){
        super(Type.EVENT);
    }
    private MPEvent(Builder builder){
        super(Type.EVENT);
        if (builder.eventType == null){
            Logger.error("MPEvent created with no event type!");
        } else {
            eventType = builder.eventType;
        }

        if (builder.eventName == null){
            Logger.error("MPEvent created with no event name!");
        } else {
            if (builder.eventName.length() > Constants.LIMIT_ATTR_KEY){
                Logger.error("MPEvent created with too long of a name and will be truncated, the limit is: " + Constants.LIMIT_ATTR_KEY);
                eventName = builder.eventName.substring(0, Constants.LIMIT_ATTR_KEY);
            } else {
                eventName = builder.eventName;
            }
        }

        entering = builder.entering;
        setCustomAttributes(builder.customAttributes);

        if (builder.category != null){
            category = builder.category;
            if (getCustomAttributeStrings() == null){
                setCustomAttributes(new HashMap<String, Object>());
            }
            getCustomAttributeStrings().put(Constants.MessageKey.EVENT_CATEGORY, builder.category);
        }
        if (builder.duration != null){
            duration = builder.duration;
        }
        if (builder.endTime != null){
            endTime = builder.endTime;
        }
        if (builder.startTime != null){
            startTime = builder.startTime;
        }
        if (builder.customFlags != null) {
            setCustomFlags(builder.customFlags);
        }
        if (builder.shouldUploadEvent != null) {
            setShouldUploadEvent(builder.shouldUploadEvent);
        }
        screenEvent = builder.screenEvent;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return super.equals(o) || (o != null && this.toString().equals(o.toString()));
    }

    @Override
    public void setCustomAttributes(@NonNull Map<String, ?> customAttributes) {
        super.setCustomAttributes(customAttributes);
    }

    public MPEvent(@NonNull MPEvent mpEvent) {
        super(Type.EVENT);
        eventType = mpEvent.eventType;
        eventName = mpEvent.eventName;
        if (mpEvent.getCustomAttributes() != null) {
            setCustomAttributes(mpEvent.getCustomAttributes());
        }else {
            setCustomAttributes(null);
        }
        category = mpEvent.category;
        duration = mpEvent.duration;
        endTime = mpEvent.endTime;
        startTime = mpEvent.startTime;
        setCustomFlags(mpEvent.getCustomFlags());
        entering = mpEvent.entering;
        screenEvent = mpEvent.screenEvent;
        setShouldUploadEvent(mpEvent.isShouldUploadEvent());
        InternalListenerManager.getListener().onCompositeObjects(mpEvent, this);
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (eventName != null) {
            builder.append("Event name: ")
            .append(eventName)
            .append("\n");
        }
        if (eventType != null){
            builder.append("type: ")
            .append(eventType.name())
            .append("\n");
        }
        Double length = getLength();
        if (length != null && length > 0){
            builder.append("length: ")
                    .append(length).append("ms")
                    .append("\n");
        }
        if (getCustomAttributeStrings() != null){
            builder.append("customAttributes:\n");
            List<String> sortedKeys = new ArrayList(getCustomAttributeStrings().keySet());
            Collections.sort(sortedKeys);
            for (String key : sortedKeys)
            {
                builder.append(key)
                .append(":")
                .append(getCustomAttributeStrings().get(key))
                .append("\n");
            }
        }
        if (getCustomFlags() != null) {
            builder.append("custom flags:\n");
            builder.append(getCustomFlags().toString());
        }
        return builder.toString();
    }


    @NonNull
    public String getEventName() {
        return eventName;
    }

    public boolean isScreenEvent() {
        return screenEvent;
    }

    protected MPEvent setScreenEvent(boolean screenEvent) {
        this.screenEvent = screenEvent;
        setType(screenEvent ? Type.SCREEN_VIEW : Type.EVENT);
        return this;
    }

    public int getEventHash() {
        if (eventHash == 0){
            eventHash = MPUtility.mpHash(eventType.ordinal() + eventName);
        }
        return eventHash;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    @NonNull
    public MParticle.EventType getEventType() {
        return eventType;
    }

    @Nullable
    public Double getLength() {
        if (duration != null) {
            return duration;
        }
        if (endTime != null && startTime != null) {
            double length = endTime - startTime;
            return length > 0 ? length : 0;
        }
        return null;
    }

    boolean getNavigationDirection() {
        return entering;
    }

    @NonNull
    public BaseMPMessageBuilder getMessage() {
        return new MPEventMessage.Builder(Constants.MessageType.EVENT)
                .customEventType(getEventType())
                .name(getEventName())
                .length(getLength())
                .flags(getCustomFlags())
                .attributes(MPUtility.enforceAttributeConstraints(getCustomAttributeStrings()));
    }

    /**
     * Class used to build an {@link com.mparticle.MPEvent} object.
     *
     * @see com.mparticle.MParticle#logEvent(BaseEvent)
     */
    public static class Builder {
        private boolean screenEvent;
        private MParticle.EventType eventType;
        private String eventName;
        private String category;
        private Map<String, ?> customAttributes;
        private Double duration = null, startTime = null, endTime = null;
        private Map<String, List<String>> customFlags = null;
        private boolean entering = true;
        private Boolean shouldUploadEvent;

        private Builder(){}

        /**
         * Starting point of the builder with two required parameters. The rest of the fields
         * of this class are optional. Once the desired fields have been set, use {@link #build()} to
         * create the {@link com.mparticle.MPEvent} object.
         *
         *
         * @param eventName the name of the event to be tracked (required)
         * @param eventType the type of the event to be tracked (required)
         */
        public Builder(@NonNull String eventName,@NonNull MParticle.EventType eventType){
            this.eventName = eventName;
            this.eventType = eventType;
        }

        /**
         * Starting point of the builder with two required parameters. The rest of the fields
         * of this class are optional. Once the desired fields have been set, use {@link #build()} to
         * create the {@link com.mparticle.MPEvent} object.
         *
         *
         * @param eventName the name of the event to be tracked (required)
         */
        public Builder(@NonNull String eventName){
            this.eventName = eventName;
            this.eventType = MParticle.EventType.Other;
        }


        /**
         * Use this to convert an existing MPEvent to a Builder, useful in modifying
         * and duplicating events.
         *
         * @param event
         */
        public Builder(@NonNull MPEvent event) {
            this.eventName = event.getEventName();
            this.eventType = event.getEventType();
            this.category = event.getCategory();
            this.customAttributes = event.getCustomAttributes();
            this.duration = event.duration;
            this.startTime = event.startTime;
            this.endTime = event.endTime;
            this.customFlags = event.getCustomFlags();
            this.entering = event.entering;
            this.screenEvent = event.screenEvent;
            this.shouldUploadEvent = event.isShouldUploadEvent();
        }

        /**
         * The name of the event to be tracked, required not null.
         *
         * @param eventName
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder eventName(@NonNull String eventName){
            if (eventName != null) {
                this.eventName = eventName;
            }
            return this;
        }

        /**
         * The type of the event to be tracked.
         *
         * @param eventType
         * @return returns this builder for easy method chaining
         *
         * @see com.mparticle.MParticle.EventType
         */
        @NonNull
        public Builder eventType(@NonNull MParticle.EventType eventType){
            if (eventType != null) {
                this.eventType = eventType;
            }
            return this;
        }

        /**
         * Add a custom flag to this event. Flag keys can have multiple values - if the provided flag key already has an associated
         * value, the value will be appended.
         *
         * @param key (required) a flag key, retrieve this from the mParticle docs or solution team for your intended services(s)
         * @param value (required) a flag value to be send to the service indicated by the flag key
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder addCustomFlag(@Nullable String key, @Nullable String value) {
            if (customFlags == null) {
                customFlags = new HashMap<String, List<String>>();
            }
            if (!customFlags.containsKey(key)) {
                customFlags.put(key, new LinkedList<String>());
            }
            customFlags.get(key).add(value);
            return this;
        }

        /**
         * Bulk add custom flags to this event. This will replace any flags previously set via {@link MPEvent.Builder#addCustomFlag(String, String)}
         *
         * @param customFlags (required) a map containing the custom flags for the MPEvent
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder customFlags(@Nullable Map<String, List<String>> customFlags) {
            this.customFlags = customFlags;
            return this;
        }

        /**
         *
         * The Google Analytics category with which to associate this event.
         *
         * @param category
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder category(@Nullable String category){
            this.category = category;
            return this;
        }

        /**
         * The total length of this event, in milliseconds.
         *
         * This will override {@link #startTime(double)} and {@link #endTime(double)}.
         *
         * @param durationMillis
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder duration(double durationMillis){
            this.duration = durationMillis;
            return this;
        }

        @NonNull
        public Builder customAttributes(@Nullable Map<String, ?> customAttributes) {
            this.customAttributes = customAttributes;
            return this;
        }

        /**
         * Manually set the time when this event started - should be epoch time in milliseconds.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * Also, you can use {@link #startTime()} and {@link #endTime()}, rather than setting the time manually.
         *
         * @param startTimeMillis
         * @return returns this builder for easy method chaining
         */
        @NonNull
        private Builder startTime(double startTimeMillis){
            this.startTime = startTimeMillis;
            return this;
        }

        /**
         * Events can have a duration associate with them. This method will set the start time to the current time.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder startTime(){
            return startTime(System.currentTimeMillis());
        }

        /**
         * Events can have a duration associate with them. This method will set the end time to the current time.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder endTime(){
            return endTime(System.currentTimeMillis());
        }

        /**
         * Manually set the time when this event ended - should be epoch time milliseconds.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * @param endTimeMillis
         * @return returns this builder for easy method chaining
         */
        private Builder endTime(double endTimeMillis){
            this.endTime = endTimeMillis;
            return this;
        }

        /**
         * Beta API, subject to change. Used internally to signify if a user is entering or exiting a screen.
         *
         *
         * @param entering True if a user is navigating to a screen, False when navigating away
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder internalNavigationDirection(boolean entering){
            this.entering = entering;
            return this;
        }

        /**
         * Manually choose to skip uploading this event to mParticle server and only forward to kits.
         *
         * Note that if this method is not called, the default is to upload to mParticle as well as
         * forward to kits to match the previous behavior.
         *
         * @param shouldUploadEvent
         * @return returns this builder for easy method chaining
         */
        @NonNull
        public Builder shouldUploadEvent(boolean shouldUploadEvent) {
            this.shouldUploadEvent = shouldUploadEvent;
            return this;
        }

        /**
         * Create the MPEvent. In development mode this method will throw an IllegalStateException if this
         * MPEvent is invalid.
         *
         * @return returns the MPEvent object to be logged
         *
         * @see MParticle#logEvent(BaseEvent)
         */
        @NonNull
        public MPEvent build(){
            return new MPEvent(this);
        }

        /**
         * Use this method to deserialize the result of {@link #toString()}. This can be used to persist an event object across app sessions.
         *
         * @param builderString a string originally acquired by calling {@link #toString()}
         * @return returns this builder for easy method chaining
         */
        @Nullable
        public static Builder parseString(@NonNull String builderString){
            Builder builder = null;
            try{
                JSONObject json = new JSONObject(builderString);
                builder = new Builder(json.getString(EVENT_NAME), MParticle.EventType.valueOf(json.getString(EVENT_TYPE)));
                builder.category = json.optString(EVENT_CATEGORY);
                if (json.has(EVENT_DURATION)){
                    builder.duration = json.getDouble(EVENT_DURATION);
                }
                if (json.has(EVENT_START_TIME)){
                    builder.startTime = json.getDouble(EVENT_START_TIME);
                }
                if (json.has(EVENT_END_TIME)){
                    builder.endTime = json.getDouble(EVENT_END_TIME);
                }
                if (json.has(EVENT_INFO)){
                    JSONObject infoObject = json.getJSONObject(EVENT_INFO);
                    Map<String, String> info = new HashMap<String, String>();
                    Iterator<?> keys = infoObject.keys();

                    while( keys.hasNext() ){
                        String key = (String)keys.next();
                        info.put(key, infoObject.getString(key));
                    }
                    builder.customAttributes = new HashMap<>(info);
                }
                if (json.has(EVENT_CUSTOM_FLAGS)) {
                    JSONObject flags = json.getJSONObject(EVENT_CUSTOM_FLAGS);
                    Map<String, List<String>> cFlags = new HashMap<String, List<String>>();
                    Iterator<String> keys = flags.keys();

                    while( keys.hasNext() ){
                        String key = keys.next();
                        JSONArray values = flags.getJSONArray(key);
                        cFlags.put(key, new LinkedList<String>());
                        for (int i = 0; i < values.length(); i++) {
                            cFlags.get(key).add(values.getString(i));
                        }
                    }
                    builder.customFlags = cFlags;
                }
                if (json.has(EVENT_SHOULD_UPLOAD_EVENT)) {
                    builder.shouldUploadEvent = json.getBoolean(EVENT_SHOULD_UPLOAD_EVENT);
                }

                return builder;
            }catch (Exception e){
                Logger.warning("Failed to deserialize MPEvent.Builder: " + e.toString());
                return builder;
            }
        }

        private final static String EVENT_TYPE = "eventType";
        private final static String EVENT_CUSTOM_FLAGS = "customFlags";
        private final static String EVENT_NAME = "eventName";
        private final static String EVENT_CATEGORY = "category";
        private final static String EVENT_DURATION = "duration";
        private final static String EVENT_INFO = "customAttributes";
        private final static String EVENT_START_TIME= "startTime";
        private final static String EVENT_END_TIME= "endTime";
        private final static String EVENT_SHOULD_UPLOAD_EVENT = "shouldUploadEvent";

        /**
         * Use this method to serialize an event builder to persist the object across app sessions. The JSON string
         * produced by this method should be passed to {@link #parseString(String)}.
         *
         * @return a JSON object describing this builder
         */
        @Override
        @NonNull
        public String toString() {
            try{
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(EVENT_TYPE, eventType.toString());
                jsonObject.put(EVENT_NAME, eventName);
                if (category != null) {
                    jsonObject.put(EVENT_CATEGORY, category);
                }
                if (duration != null){
                    jsonObject.put(EVENT_DURATION, duration);
                }
                if (customAttributes != null){
                    JSONObject jsonInfo = new JSONObject();
                    for (Map.Entry<String, ?> entry : customAttributes.entrySet())
                    {
                        jsonInfo.put(entry.getKey(), entry.getValue());
                    }
                    jsonObject.put(EVENT_INFO, jsonInfo);
                }
                if (startTime != null){
                    jsonObject.put(EVENT_START_TIME, startTime);
                }
                if (endTime != null){
                    jsonObject.put(EVENT_END_TIME, endTime);
                }
                if (customFlags != null) {
                    JSONObject flagsObject = new JSONObject();
                    for (Map.Entry<String, List<String>> entry : customFlags.entrySet()) {
                        List<String> values = entry.getValue();
                        JSONArray valueArray = new JSONArray(values);
                        flagsObject.put(entry.getKey(), valueArray);
                    }
                    jsonObject.put(EVENT_CUSTOM_FLAGS, flagsObject);
                }
                if (shouldUploadEvent != null) {
                    jsonObject.put(EVENT_SHOULD_UPLOAD_EVENT, shouldUploadEvent);
                }
                return jsonObject.toString();
            }catch (JSONException jse){
                Logger.warning("Failed to serialize MPEvent.Builder: " + jse.toString());
            }
            return super.toString();
        }
    }
}
