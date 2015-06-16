package com.mparticle.internal.embedded;

import com.mparticle.MPEvent;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Projection {
    static final String MATCH_TYPE_STRING = "S";
    static final String MATCH_TYPE_HASH = "H";
    static final String MATCH_TYPE_FIELD = "F";
    static final String MATCH_TYPE_STATIC = "Sta";
    final int mID;
    final int mMappingId;
    final int mModuleMappingId;
    final int mMessageType;
    final String mMatchType;
    final String mEventName;
    final String mAttributeKey;
    final String mAttributeValue;
    final int mMaxCustomParams;
    final boolean mAppendUnmappedAsIs;
    final boolean mIsDefault;
    final String mProjectedEventName;
    final List<AttributeProjection> mAttributeProjectionList;
    final List<AttributeProjection> mStaticAttributeProjectionList;
    final List<AttributeProjection> mRequiredAttributeProjectionList;
    final int mEventHash;
    AttributeProjection nameFieldProjection = null;

    public Projection(JSONObject projectionJson) throws JSONException {
        mID = projectionJson.getInt("id");
        mMappingId = projectionJson.optInt("pmid");
        mModuleMappingId = projectionJson.optInt("pmmid");
        if (projectionJson.has("match")) {
            JSONObject match = projectionJson.getJSONObject("match");
            mMessageType = match.optInt("message_type");
            mMatchType = match.optString("event_match_type", "String");

            if (mMatchType.startsWith(MATCH_TYPE_HASH)) {
                mEventHash = Integer.parseInt(match.optString("event"));
                mAttributeKey = null;
                mAttributeValue = null;
                mEventName = null;
            } else {
                mEventHash = 0;
                mEventName = match.optString("event");
                mAttributeKey = match.optString("attribute_key");
                mAttributeValue = match.optString("attribute_value");
            }

        } else {
            mEventHash = 0;
            mMessageType = -1;
            mMatchType = "String";
            mEventName = null;
            mAttributeKey = null;
            mAttributeValue = null;
        }
        if (projectionJson.has("behavior")) {
            JSONObject behaviors = projectionJson.getJSONObject("behavior");
            mMaxCustomParams = behaviors.optInt("max_custom_params", Integer.MAX_VALUE);
            mAppendUnmappedAsIs = behaviors.optBoolean("append_unmapped_as_is");
            mIsDefault = behaviors.optBoolean("is_default");
        } else {
            mMaxCustomParams = Integer.MAX_VALUE;
            mAppendUnmappedAsIs = false;
            mIsDefault = false;
        }

        if (projectionJson.has("action")) {
            JSONObject action = projectionJson.getJSONObject("action");
            mProjectedEventName = action.optString("projected_event_name");
            if (action.has("attribute_maps")) {
                mAttributeProjectionList = new LinkedList<AttributeProjection>();
                mRequiredAttributeProjectionList = new LinkedList<AttributeProjection>();
                mStaticAttributeProjectionList = new LinkedList<AttributeProjection>();
                JSONArray attributeMapList = action.getJSONArray("attribute_maps");

                for (int i = 0; i < attributeMapList.length(); i++) {
                    AttributeProjection attProjection = new AttributeProjection(attributeMapList.getJSONObject(i));
                    if (attProjection.mIsRequired) {
                        mRequiredAttributeProjectionList.add(attProjection);
                    } else if (attProjection.mMatchType.startsWith(MATCH_TYPE_STATIC)) {
                        mStaticAttributeProjectionList.add(attProjection);
                    } else if (attProjection.mMatchType.startsWith(MATCH_TYPE_FIELD)) {
                        nameFieldProjection = attProjection;
                    } else {
                        mAttributeProjectionList.add(attProjection);
                    }
                }
            } else {
                mAttributeProjectionList = null;
                mRequiredAttributeProjectionList = null;
                mStaticAttributeProjectionList = null;
            }
        } else {
            mAttributeProjectionList = null;
            mRequiredAttributeProjectionList = null;
            mStaticAttributeProjectionList = null;
            mProjectedEventName = null;
        }
    }

    public boolean isDefault() {
        return mIsDefault;
    }

    public boolean isMatch(EmbeddedProvider.MPEventWrapper eventWrapper) {
        if (mIsDefault) {
            return true;
        }
        MPEvent event = eventWrapper.getEvent();
        if (nameFieldProjection != null && nameFieldProjection.mIsRequired) {
            if (!nameFieldProjection.matchesDataType(event.getEventName())) {
                return false;
            }
        }
        if (mMatchType.startsWith(MATCH_TYPE_HASH) && event.getEventHash() == mEventHash) {
            return true;
        } else if (mMatchType.startsWith(MATCH_TYPE_STRING) &&
                event.getEventName().equalsIgnoreCase(mEventName) &&
                event.getInfo() != null &&
                mAttributeValue.equalsIgnoreCase(event.getInfo().get(mAttributeKey))) {
            return true;
        }
        return false;
    }

    public MPEvent project(EmbeddedProvider.MPEventWrapper eventWrapper) {
        MPEvent event = eventWrapper.getEvent();
        String eventName = MPUtility.isEmpty(mProjectedEventName) ? event.getEventName() : mProjectedEventName;
        MPEvent.Builder builder = new MPEvent.Builder(event);
        builder.eventName(eventName);
        builder.info(null);
        Map<String, String> attributes;
        if (event.getInfo() != null){
            attributes = new HashMap<String, String>(event.getInfo());
        }else{
            attributes = new HashMap<String, String>();
        }
        Map<String, String> newAttributes = new HashMap<String, String>();
        Set<String> usedAttributes = new HashSet<String>();
        if (mRequiredAttributeProjectionList != null) {
            for (int i = 0; i < mRequiredAttributeProjectionList.size(); i++) {
                AttributeProjection attProjection = mRequiredAttributeProjectionList.get(i);
                if (attProjection.mMatchType.startsWith(MATCH_TYPE_STRING)) {
                    String value = attributes.get(attProjection.mValue);
                    if (value != null && attProjection.matchesDataType(value)) {
                        String name = MPUtility.isEmpty(attProjection.mProjectedAttributeName) ? attProjection.mValue : attProjection.mProjectedAttributeName;
                        newAttributes.put(name, value);
                        usedAttributes.add(attProjection.mValue);
                    } else {
                        return null;
                    }
                } else if (attProjection.mMatchType.startsWith(MATCH_TYPE_HASH)) {
                    String key = eventWrapper.getAttributeHashes().get(Integer.parseInt(attProjection.mValue));
                    if (key == null) {
                        return null;
                    }
                    String value = attributes.get(key);
                    if (!attProjection.matchesDataType(value)) {
                        return null;
                    }
                    if (!MPUtility.isEmpty(attProjection.mProjectedAttributeName)) {
                        key = attProjection.mProjectedAttributeName;
                    }
                    newAttributes.put(key, value);
                    usedAttributes.add(attProjection.mValue);
                }
            }
        }
        if (mAttributeProjectionList != null) {
            for (int i = 0; i < mAttributeProjectionList.size(); i++) {
                AttributeProjection attProjection = mAttributeProjectionList.get(i);
                if (attProjection.mMatchType.startsWith(MATCH_TYPE_STRING)) {
                    String value = attributes.get(attProjection.mValue);
                    if (value != null) {
                        String name = MPUtility.isEmpty(attProjection.mProjectedAttributeName) ? attProjection.mValue : attProjection.mProjectedAttributeName;
                        newAttributes.put(name, value);
                        usedAttributes.add(attProjection.mValue);
                    }
                } else if (attProjection.mMatchType.startsWith(MATCH_TYPE_HASH)) {
                    String key = eventWrapper.getAttributeHashes().get(Integer.parseInt(attProjection.mValue));
                    String value = attributes.get(key);
                    if (!MPUtility.isEmpty(attProjection.mProjectedAttributeName)) {
                        key = attProjection.mProjectedAttributeName;
                    }
                    newAttributes.put(key, value);
                    usedAttributes.add(attProjection.mValue);
                }
            }
        }
        if (mStaticAttributeProjectionList != null) {
            for (int i = 0; i < mStaticAttributeProjectionList.size(); i++) {
                AttributeProjection attProjection = mStaticAttributeProjectionList.get(i);
                newAttributes.put(attProjection.mProjectedAttributeName, attProjection.mValue);
                usedAttributes.add(attProjection.mValue);
            }
        }
        if (nameFieldProjection != null) {
            newAttributes.put(nameFieldProjection.mProjectedAttributeName, event.getEventName());
        }
        if (mAppendUnmappedAsIs && mMaxCustomParams > 0 && newAttributes.size() < mMaxCustomParams) {

            List<String> sortedKeys = new ArrayList(attributes.keySet());
            Collections.sort(sortedKeys);
            for (int i = 0; (i < sortedKeys.size() && newAttributes.size() < mMaxCustomParams); i++) {
                String key = sortedKeys.get(i);
                if (!usedAttributes.contains(key) && !newAttributes.containsKey(key)) {
                    newAttributes.put(key, attributes.get(key));
                }
            }
        }
        builder.info(newAttributes);
        return builder.build();
    }

    public int getMessageType() {
        return mMessageType;
    }

    static class AttributeProjection {
        final String mProjectedAttributeName;
        final String mValue;
        final int mDataType;
        final String mMatchType;
        final boolean mIsRequired;

        public AttributeProjection(JSONObject attributeMapJson) {
            mProjectedAttributeName = attributeMapJson.optString("projected_attribute_name");
            mMatchType = attributeMapJson.optString("match_type", "String");
            mValue = attributeMapJson.optString("value");
            mDataType = attributeMapJson.optInt("data_type", 1);
            mIsRequired = attributeMapJson.optBoolean("is_required");
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o) || this.toString().equals(o.toString());
        }

        @Override
        public String toString() {
            return "projected_attribute_name: " + mProjectedAttributeName + "\n" +
                    "match_type: " + mMatchType + "\n" +
                    "value: " + mValue + "\n" +
                    "data_type: " + mDataType + "\n" +
                    "is_required: " + mIsRequired;
        }

        public boolean matchesDataType(String value) {
            switch (mDataType) {
                case 1:
                    return true;
                case 2:
                    try {
                        Integer.parseInt(value);
                        return true;
                    } catch (NumberFormatException nfe) {
                        return false;
                    }
                case 3:
                    return Boolean.parseBoolean(value) || "false".equalsIgnoreCase(value);
                case 4:
                    try {
                        Double.parseDouble(value);
                        return true;
                    } catch (NumberFormatException nfe) {
                        return false;
                    }
                default:
                    return false;
            }
        }
    }
}
