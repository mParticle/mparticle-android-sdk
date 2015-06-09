package com.mparticle.internal.embedded;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class Projection {
    private final int mID;
    private final int mMappingId;
    private final int mModuleMappingId;
    private final int mMessageType;
    private final String mMatchType;
    private final String mEventName;
    private final String mAttributeKey;
    private final String mAttributeValue;
    private final int mMaxCustomParams;
    private final boolean mAppendUnmappedAsIs;
    private final boolean mIsDefault;
    private final String mProjectedEventName;
    private final List<AttributeProjection> mAttributeProjectionList;

    public Projection(JSONObject projectionJson) throws JSONException {
        mID = projectionJson.getInt("id");
        mMappingId = projectionJson.optInt("pmid");
        mModuleMappingId = projectionJson.optInt("pmmid");
        if (projectionJson.has("match")) {
            JSONObject match = projectionJson.getJSONObject("match");
            mMessageType = match.optInt("message_type");
            mMatchType = match.optString("event_match_type", "String");
            mEventName = match.optString("event");
            mAttributeKey = match.optString("$MethodName");
            mAttributeValue = match.optString("$AddToCart");
        }else{
            mMessageType = -1;
            mMatchType = "String";
            mEventName = null;
            mAttributeKey = null;
            mAttributeValue = null;
        }
        if (projectionJson.has("behaviors")) {
            JSONObject behaviors = projectionJson.getJSONObject("behaviors");
            mMaxCustomParams = behaviors.optInt("max_custom_params");
            mAppendUnmappedAsIs = behaviors.optBoolean("append_unmapped_as_is");
            mIsDefault = behaviors.optBoolean("is_default");
        }else{
            mMaxCustomParams = 0;
            mAppendUnmappedAsIs = false;
            mIsDefault = false;
        }
        mAttributeProjectionList = new LinkedList<AttributeProjection>();
        if (projectionJson.has("action")) {
            JSONObject action = projectionJson.getJSONObject("action");
            mProjectedEventName = action.optString("projected_event_name");
            if (action.has("attribute_maps")) {
                JSONArray attributeMapList = action.getJSONArray("attribute_maps");

                for (int i = 0; i < attributeMapList.length(); i++) {
                    AttributeProjection attProjection = new AttributeProjection(attributeMapList.getJSONObject(i));
                    mAttributeProjectionList.add(attProjection);
                }
            }
        }else{
            mProjectedEventName = null;
        }
    }

    private class AttributeProjection {
        private final String mProjectedAttributeName;
        private final String mValue;
        private final int mDataType;
        private final String mMatchType;
        private final boolean mIsRequired;

        public AttributeProjection(JSONObject attributeMapJson) {
            mProjectedAttributeName = attributeMapJson.optString("projected_attribute_name");
            mMatchType = attributeMapJson.optString("match_type", "String");
            mValue = attributeMapJson.optString("value");
            mDataType = attributeMapJson.optInt("data_type");
            mIsRequired = attributeMapJson.optBoolean("is_required");
        }
    }
}
