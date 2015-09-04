package com.mparticle;


import android.util.Log;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 *
 * Class representation of an event.
 *
 * <code>MPEvent</code> implements the Builder pattern, see {@link com.mparticle.MPEvent.Builder} for more information.
 *
 * @see com.mparticle.MParticle#logEvent(MPEvent)
 *
 */
public class MPEvent {
    private MParticle.EventType eventType;
    private String eventName;
    private String category;
    private Map<String, String> info;
    private Double duration = null, startTime = null, endTime = null;
    private int eventHash;

    private MPEvent(){}
    private MPEvent(Builder builder){
        if (builder.eventType == null){
            ConfigManager.log(MParticle.LogLevel.ERROR, "MPEvent created with no event type!");
        }else{
            eventType = builder.eventType;
        }

        if (builder.eventName == null){
            ConfigManager.log(MParticle.LogLevel.ERROR, "MPEvent created with no event name!");
        }else{
            if (builder.eventName.length() > Constants.LIMIT_NAME){
                ConfigManager.log(MParticle.LogLevel.ERROR, "MPEvent created with too long of a name, the limit is: " + Constants.LIMIT_NAME);
            }
            eventName = builder.eventName;
        }

        info = builder.info;

        if (builder.category != null){
            category = builder.category;
            if (info == null){
                info = new HashMap<String, String>(1);
            }
            info.put(Constants.MessageKey.EVENT_CATEGORY, builder.category);
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
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) || (o != null && this.toString().equals(o.toString()));
    }

    public void setInfo(Map<String, String> info){
        this.info = info;
    }

    public MPEvent(MPEvent mpEvent) {
        eventType = mpEvent.eventType;
        eventName = mpEvent.eventName;
        if (mpEvent.info != null) {
            Map<String, String> shallowCopy = new HashMap<String, String>();
            shallowCopy.putAll(mpEvent.info);
            info = shallowCopy;
        }else {
            info = null;
        }
        category = mpEvent.category;
        duration = mpEvent.duration;
        endTime = mpEvent.endTime;
        startTime = mpEvent.startTime;
    }

    @Override
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
        if (info != null){
            builder.append("info:\n");
            List<String> sortedKeys = new ArrayList(info.keySet());
            Collections.sort(sortedKeys);
            for (String key : sortedKeys)
            {
                builder.append(key)
                .append(":")
                .append(info.get(key))
                .append("\n");
            }
        }
        return builder.toString();
    }

    public String getEventName() {
        return eventName;
    }

    public int getEventHash() {
        if (eventHash == 0){
            eventHash = MPUtility.mpHash(eventType.ordinal() + eventName);
        }
        return eventHash;
    }

    public String getCategory() {
        return category;
    }

    public Map<String, String> getInfo() {
        return info;
    }

    public MParticle.EventType getEventType() {
        return eventType;
    }

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

    /**
     * Class used to build an {@link com.mparticle.MPEvent} object.
     *
     * @see com.mparticle.MParticle#logEvent(MPEvent)
     */
    public static class Builder {
        private MParticle.EventType eventType;
        private String eventName;
        private String category;
        private Map<String, String> info;
        private Double duration = null, startTime = null, endTime = null;


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
        public Builder(String eventName, MParticle.EventType eventType){
            this.eventName = eventName;
            this.eventType = eventType;
        }

        public Builder(MPEvent event) {
            this.eventName = event.getEventName();
            this.eventType = event.getEventType();
            this.category = event.getCategory();
            this.info = event.getInfo();
            this.duration = event.duration;
            this.startTime = event.startTime;
            this.endTime = event.endTime;
        }

        /**
         * the name of the event to be tracked, required not null.
         *
         * @param eventName
         * @return
         */
        public Builder eventName(String eventName){
            if (eventName != null) {
                this.eventName = eventName;
            }
            return this;
        }

        /**
         * the type of the event to be tracked
         *
         * @param eventType
         * @return
         *
         * @see com.mparticle.MParticle.EventType
         */
        public Builder eventType(MParticle.EventType eventType){
            if (eventType != null) {
                this.eventType = eventType;
            }
            return this;
        }

        /**
         *
         * The Google Analytics category with which to associate this event
         *
         * @param category
         * @return
         */
        public Builder category(String category){
            this.category = category;
            return this;
        }

        /**
         * The total length of this event in milliseconds.
         *
         * This will override {@link #startTime(double)} and {@link #endTime(double)}.
         *
         * @param durationMillis
         * @return
         */
        public Builder duration(double durationMillis){
            this.duration = durationMillis;
            return this;
        }

        /**
         * Data attributes to associate with the event
         *
         * @param info
         * @return
         */
        public Builder info(Map<String, String> info){
            this.info = info;
            return this;
        }

        /**
         * Manually set the time when this event started - should be epoch time milliseconds.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * Also, you can use {@link #startTime()} and {@link #endTime()}, rather than setting the time manually.
         *
         * @param startTimeMillis
         * @return
         */
        private Builder startTime(double startTimeMillis){
            this.startTime = startTimeMillis;
            return this;
        }

        /**
         * Events can have a duration associate with them. This method will set the start time to the current time.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * @return
         */
        public Builder startTime(){
            return startTime(System.currentTimeMillis());
        }

        /**
         * Events can have a duration associate with them. This method will set the end time to the current time.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * @return
         */
        public Builder endTime(){
            return endTime(System.currentTimeMillis());
        }

        /**
         *
         * Manually set the time when this event ended - should be epoch time milliseconds.
         *
         *
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * @param endTimeMillis
         * @return
         */
        private Builder endTime(double endTimeMillis){
            this.endTime = endTimeMillis;
            return this;
        }

        public MPEvent build(){
            return new MPEvent(this);
        }

        /**
         * Use this method to deserialize the result of {@link #toString()}. This can be used to persist an event object across app sessions.
         *
         * @param builderString a string originally acquired by calling {@link #toString()}
         * @return
         */
        public static Builder parseString(String builderString){
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
                    builder.info = info;
                }

                return builder;
            }catch (Exception e){
                Log.w(Constants.LOG_TAG, "Failed to deserialize MPEvent.Builder: " + e.toString());
                return builder;
            }
        }

        private final static String EVENT_TYPE = "eventType";
        private final static String EVENT_NAME = "eventName";
        private final static String EVENT_CATEGORY = "category";
        private final static String EVENT_DURATION = "duration";
        private final static String EVENT_INFO = "info";
        private final static String EVENT_START_TIME= "startTime";
        private final static String EVENT_END_TIME= "endTime";

        /**
         * Use this method to serialize an event builder to persist the object across app sessions. The JSON string
         * produced by this method should be passed to {@link #parseString(String)}.
         *
         * @return a JSON object describing this builder
         */
        @Override
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
                if (info != null){
                    JSONObject jsonInfo = new JSONObject();
                    for (Map.Entry<String, String> entry : info.entrySet())
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
                return jsonObject.toString();
            }catch (JSONException jse){
                Log.w(Constants.LOG_TAG, "Failed to serialize MPEvent.Builder: " + jse.toString());
            }
            return super.toString();
        }
    }
}
