package com.mparticle;


import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;

import java.util.HashMap;
import java.util.Map;

public class MPEvent {
    private MParticle.EventType eventType;
    private String eventName;
    private String category;
    private Map<String, String> info;
    private Double duration = null, startTime = null, endTime = null;

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
        double length = getLength();
        if (length > 0){
            builder.append("length: ")
                    .append(length).append("ms")
                    .append("\n");
        }
        if (info != null){
            builder.append("info:\n");
            for (Map.Entry<String, String> entry : info.entrySet())
            {
                builder.append(entry.getKey())
                .append(":")
                .append(entry.getValue())
                .append("\n");
            }
        }
        return builder.toString();
    }

    public String getEventName() {
        return eventName;
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

    public double getLength() {
        if (duration != null) {
            return duration;
        }
        if (endTime != null && startTime != null) {
            double length = endTime - startTime;
            return length > 0 ? length : 0;
        }else {
            return 0;
        }
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
         * the time when this event started.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * @param startTimeMillis
         * @return
         */
        public Builder startTime(double startTimeMillis){
            this.startTime = startTimeMillis;
            return this;
        }

        /**
         *
         * The time when this event ended, in milliseconds.
         *
         * Note that by using {@link #duration(double)}, this value will be ignored.
         *
         * @param endTimeMillis
         * @return
         */
        public Builder endTime(double endTimeMillis){
            this.endTime = endTimeMillis;
            return this;
        }

        public MPEvent build(){
            return new MPEvent(this);
        }
    }
}
