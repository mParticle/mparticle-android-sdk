package com.mparticle.kits.mappings;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.kits.CommerceEventUtils;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomMapping {
    static final String MATCH_TYPE_STRING = "S";
    static final String MATCH_TYPE_HASH = "H";
    static final String MATCH_TYPE_FIELD = "F";
    static final String MATCH_TYPE_STATIC = "Sta";
    final int mID;
    final int mMappingId;
    final int mModuleMappingId;
    final int mMaxCustomParams;
    final boolean mAppendUnmappedAsIs;
    final boolean mIsDefault;
    final String mProjectedEventName;
    final List<AttributeMap> mStaticAttributeMapList;
    final List<AttributeMap> mRequiredAttributeMapList;
    private final boolean isSelectorLast;
    final int mOutboundMessageType;
    private List<CustomMappingMatch> matchList = null;
    public final static String PROPERTY_LOCATION_EVENT_FIELD = "EventField";
    public final static String PROPERTY_LOCATION_EVENT_ATTRIBUTE = "EventAttribute";
    public final static String PROPERTY_LOCATION_PRODUCT_FIELD = "ProductField";
    public final static String PROPERTY_LOCATION_PRODUCT_ATTRIBUTE = "ProductAttribute";
    public final static String PROPERTY_LOCATION_PROMOTION_FIELD = "PromotionField";

    public List<CustomMappingMatch> getMatchList() {
        return matchList;
    }

    public CustomMapping(JSONObject projectionJson) throws JSONException {
        mID = projectionJson.getInt("id");
        mMappingId = projectionJson.optInt("pmid");
        mModuleMappingId = projectionJson.optInt("pmmid");

        if (projectionJson.has("matches")) {
            JSONArray matchJson = projectionJson.getJSONArray("matches");
            matchList = new ArrayList<CustomMappingMatch>(matchJson.length());
            for (int i = 0; i < matchJson.length(); i++) {
                CustomMappingMatch match = new CustomMappingMatch(matchJson.getJSONObject(i));
                matchList.add(match);
            }
        } else {
            matchList = new ArrayList<CustomMappingMatch>(1);
            CustomMappingMatch match = new CustomMappingMatch(null);
            matchList.add(match);

        }
        if (projectionJson.has("behavior")) {
            JSONObject behaviors = projectionJson.getJSONObject("behavior");
            mMaxCustomParams = behaviors.optInt("max_custom_params", Integer.MAX_VALUE);
            mAppendUnmappedAsIs = behaviors.optBoolean("append_unmapped_as_is");
            mIsDefault = behaviors.optBoolean("is_default");
            isSelectorLast = behaviors.optString("selector", "foreach").equalsIgnoreCase("last");
        } else {
            mMaxCustomParams = Integer.MAX_VALUE;
            mAppendUnmappedAsIs = false;
            mIsDefault = false;
            isSelectorLast = false;
        }

        if (projectionJson.has("action")) {
            JSONObject action = projectionJson.getJSONObject("action");
            mOutboundMessageType = action.optInt("outbound_message_type", 4);
            mProjectedEventName = action.optString("projected_event_name");
            if (action.has("attribute_maps")) {
                mRequiredAttributeMapList = new LinkedList<AttributeMap>();
                mStaticAttributeMapList = new LinkedList<AttributeMap>();
                JSONArray attributeMapList = action.getJSONArray("attribute_maps");

                for (int i = 0; i < attributeMapList.length(); i++) {
                    AttributeMap attProjection = new AttributeMap(attributeMapList.getJSONObject(i));
                    if (attProjection.mMatchType.startsWith(MATCH_TYPE_STATIC)) {
                        mStaticAttributeMapList.add(attProjection);
                    } else {
                        mRequiredAttributeMapList.add(attProjection);
                    }
                }
                Collections.sort(mRequiredAttributeMapList, new Comparator<AttributeMap>() {
                    @Override
                    public int compare(AttributeMap lhs, AttributeMap rhs) {
                        if (lhs.mIsRequired == rhs.mIsRequired) {
                            return 0;
                        }else if (lhs.mIsRequired && !rhs.mIsRequired) {
                            return -1;
                        }else {
                            return 1;
                        }
                    }
                });
            } else {
                mRequiredAttributeMapList = null;
                mStaticAttributeMapList = null;
            }
        } else {
            mRequiredAttributeMapList = null;
            mStaticAttributeMapList = null;
            mProjectedEventName = null;
            mOutboundMessageType = 4;
        }
    }

    public boolean isDefault() {
        return mIsDefault;
    }

    private ProjectionResult projectMPEvent(MPEvent event) {
        EventWrapper.MPEventWrapper eventWrapper = new EventWrapper.MPEventWrapper(event);
        String eventName = MPUtility.isEmpty(mProjectedEventName) ? event.getEventName() : mProjectedEventName;
        MPEvent.Builder builder = new MPEvent.Builder(event);
        builder.eventName(eventName);
        builder.customAttributes(null);

        Map<String, String> newAttributes = new HashMap<String, String>();
        Set<String> usedAttributes = new HashSet<String>();
        if (!mapAttributes(mRequiredAttributeMapList, eventWrapper, newAttributes, usedAttributes, null, null)) {
            return null;
        }
        if (mStaticAttributeMapList != null) {
            for (int i = 0; i < mStaticAttributeMapList.size(); i++) {
                AttributeMap attProjection = mStaticAttributeMapList.get(i);
                newAttributes.put(attProjection.mProjectedAttributeName, attProjection.mValue);
                usedAttributes.add(attProjection.mValue);
            }
        }
        if (mAppendUnmappedAsIs && mMaxCustomParams > 0 && newAttributes.size() < mMaxCustomParams) {
            Map<String, String> originalAttributes;
            if (event.getCustomAttributeStrings() != null) {
                originalAttributes = new HashMap<String, String>(event.getCustomAttributeStrings());
            } else {
                originalAttributes = new HashMap<String, String>();
            }
            List<String> sortedKeys = new ArrayList(originalAttributes.keySet());
            Collections.sort(sortedKeys);
            for (int i = 0; (i < sortedKeys.size() && newAttributes.size() < mMaxCustomParams); i++) {
                String key = sortedKeys.get(i);
                if (!usedAttributes.contains(key) && !newAttributes.containsKey(key)) {
                    newAttributes.put(key, originalAttributes.get(key));
                }
            }
        }
        builder.customAttributes(newAttributes);
        return new ProjectionResult(builder.build(), mID);
    }
    public List<ProjectionResult> project(EventWrapper.CommerceEventWrapper commerceEventWrapper) {
        List<ProjectionResult> projectionResults = new LinkedList<ProjectionResult>();
        CommerceEvent commerceEvent = commerceEventWrapper.getEvent();
        int eventType = CommerceEventUtils.getEventType(commerceEvent);
        //TODO Impression projections are not supported for now
        if (eventType == CommerceEventUtils.Constants.EVENT_TYPE_IMPRESSION) {
            return null;
        }else if (eventType == CommerceEventUtils.Constants.EVENT_TYPE_PROMOTION_CLICK || eventType == CommerceEventUtils.Constants.EVENT_TYPE_PROMOTION_VIEW) {
            List<Promotion> promotions = commerceEvent.getPromotions();
            if (promotions == null || promotions.size() == 0){
                ProjectionResult projectionResult = projectCommerceEvent(commerceEventWrapper, null, null);
                if (projectionResult != null) {
                    projectionResults.add(projectionResult);
                }
            }else{
                if (isSelectorLast) {
                    Promotion promotion = promotions.get(promotions.size() - 1);
                    ProjectionResult projectionResult = projectCommerceEvent(commerceEventWrapper, null, promotion);
                    if (projectionResult != null) {
                        projectionResults.add(projectionResult);
                    }
                }else{
                    for (int i = 0; i < promotions.size(); i++) {
                        ProjectionResult projectionResult = projectCommerceEvent(commerceEventWrapper, null, promotions.get(i));
                        if (projectionResult != null) {
                            if (projectionResult.getCommerceEvent() != null) {
                                CommerceEvent foreachCommerceEvent = new CommerceEvent.Builder(projectionResult.getCommerceEvent())
                                        .promotions(null)
                                        .addPromotion(promotions.get(i))
                                        .build();
                                projectionResult.mCommerceEvent = foreachCommerceEvent;
                            }
                            projectionResults.add(projectionResult);
                        }
                    }

                }
            }
        }else {
            List<Product> products = commerceEvent.getProducts();
            if (isSelectorLast){
                Product product = null;
                if (products != null && products.size() > 0){
                    product = products.get(products.size() - 1);
                }
                ProjectionResult projectionResult = projectCommerceEvent(commerceEventWrapper, product, null);
                if (projectionResult != null) {
                    projectionResults.add(projectionResult);
                }
            }else {
                if (products != null) {
                    for (int i = 0; i < products.size(); i++) {
                        ProjectionResult projectionResult = projectCommerceEvent(commerceEventWrapper, products.get(i), null);
                        if (projectionResult != null) {
                            if (projectionResult.getCommerceEvent() != null) {
                                CommerceEvent foreachCommerceEvent = new CommerceEvent.Builder(projectionResult.getCommerceEvent())
                                        .products(null)
                                        .addProduct(products.get(i))
                                        .build();

                                projectionResult.mCommerceEvent = foreachCommerceEvent;
                            }
                            projectionResults.add(projectionResult);
                        }
                    }
                }
            }
        }


        if (projectionResults.size() > 0) {
            return projectionResults;
        }else{
            return null;
        }
    }
    public List<ProjectionResult> project(EventWrapper.MPEventWrapper event) {
        List<ProjectionResult> projectionResults = new LinkedList<ProjectionResult>();

        ProjectionResult projectionResult = projectMPEvent(event.getEvent());
        if (projectionResult != null) {
            projectionResults.add(projectionResult);
        }

        if (projectionResults.size() > 0) {
            return projectionResults;
        }else{
            return null;
        }
    }

    private boolean mapAttributes(List<AttributeMap> projectionList, EventWrapper eventWrapper, Map<String, String> mappedAttributes, Set<String> usedAttributes, Product product, Promotion promotion) {
        if (projectionList != null) {
            for (int i = 0; i < projectionList.size(); i++) {
                AttributeMap attProjection = projectionList.get(i);
                Map.Entry<String, String> entry = null;
                if (attProjection.mMatchType.startsWith(MATCH_TYPE_STRING)) {
                    entry = eventWrapper.findAttribute(attProjection.mLocation, attProjection.mValue, product, promotion);
                } else if (attProjection.mMatchType.startsWith(MATCH_TYPE_HASH)) {
                    entry = eventWrapper.findAttribute(attProjection.mLocation, Integer.parseInt(attProjection.mValue), product, promotion);
                } else if (attProjection.mMatchType.startsWith(MATCH_TYPE_FIELD) && eventWrapper.getEvent() instanceof MPEvent) {
                    //match_type field is a special case for mapping the event name to an attribute, only supported by MPEvent
                    entry = new AbstractMap.SimpleEntry<String, String>(attProjection.mProjectedAttributeName, ((MPEvent)eventWrapper.getEvent()).getEventName());
                }
                if (entry == null || !attProjection.matchesDataType(entry.getValue())) {
                    if (attProjection.mIsRequired) {
                        return false;
                    }else {
                        continue;
                    }
                }

                String key = entry.getKey();
                if (!MPUtility.isEmpty(attProjection.mProjectedAttributeName)) {
                    key = attProjection.mProjectedAttributeName;
                }
                mappedAttributes.put(key, entry.getValue());
                usedAttributes.add(entry.getKey());
            }
        }
        return true;
    }

    private ProjectionResult projectCommerceEvent(EventWrapper.CommerceEventWrapper eventWrapper, Product product, Promotion promotion) {
        Map<String, String> mappedAttributes = new HashMap<String, String>();
        Set<String> usedAttributes = new HashSet<String>();
        if (!mapAttributes(mRequiredAttributeMapList, eventWrapper, mappedAttributes, usedAttributes, product, promotion)) {
            return null;
        }
        if (mStaticAttributeMapList != null) {
            for (int i = 0; i < mStaticAttributeMapList.size(); i++) {
                AttributeMap attProjection = mStaticAttributeMapList.get(i);
                mappedAttributes.put(attProjection.mProjectedAttributeName, attProjection.mValue);
                usedAttributes.add(attProjection.mValue);
            }
        }
        if (mAppendUnmappedAsIs && mMaxCustomParams > 0 && mappedAttributes.size() < mMaxCustomParams) {
            CommerceEvent event = eventWrapper.getEvent();
            Map<String, String> originalAttributes;
            if (event.getCustomAttributeStrings() != null) {
                originalAttributes = new HashMap<String, String>(event.getCustomAttributeStrings());
            } else {
                originalAttributes = new HashMap<String, String>();
            }
            List<String> sortedKeys = new ArrayList(originalAttributes.keySet());
            Collections.sort(sortedKeys);
            for (int i = 0; (i < sortedKeys.size() && mappedAttributes.size() < mMaxCustomParams); i++) {
                String key = sortedKeys.get(i);
                if (!usedAttributes.contains(key) && !mappedAttributes.containsKey(key)) {
                    mappedAttributes.put(key, originalAttributes.get(key));
                }
            }
        }
        if (mOutboundMessageType == 16){
            return new ProjectionResult(
                    new CommerceEvent.Builder(eventWrapper.getEvent())
                            .internalEventName(mProjectedEventName)
                            .customAttributes(mappedAttributes)
                            .build(),
                    mID
            );
        }else {
            return new ProjectionResult(
                    new MPEvent.Builder(mProjectedEventName, MParticle.EventType.Transaction)
                            .customAttributes(mappedAttributes)
                            .build(),
                    mID
            );
        }
    }

    /**
     * All CustomMappingMatches for a given CustomMapping must have the same message type,
     * and a CustomMapping is guaranteed to have at least 1 match.
     * Due to this - just return the message type of the first CustomMappingMatch.
     */
    public int getMessageType() {
        return matchList.get(0).mMessageType;
    }

    static class AttributeMap {
        final String mProjectedAttributeName;
        final String mValue;
        final int mDataType;
        final String mMatchType;
        final boolean mIsRequired;
        final String mLocation;

        public AttributeMap(JSONObject attributeMapJson) {
            mProjectedAttributeName = attributeMapJson.optString("projected_attribute_name");
            mMatchType = attributeMapJson.optString("match_type", "String");
            mValue = attributeMapJson.optString("value");
            mDataType = attributeMapJson.optInt("data_type", 1);
            mIsRequired = attributeMapJson.optBoolean("is_required");
            mLocation = attributeMapJson.optString("property", PROPERTY_LOCATION_EVENT_ATTRIBUTE);
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
                    return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
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

    public static List<CustomMapping.ProjectionResult> projectEvents(MPEvent event, List<CustomMapping> customMappingList, CustomMapping defaultCustomMapping) {
        return projectEvents(event, false, customMappingList, defaultCustomMapping, null);
    }

    public static List<CustomMapping.ProjectionResult> projectEvents(CommerceEvent event, List<CustomMapping> customMappingList, CustomMapping defaultCommerceCustomMapping) {
        if (CommerceEventUtils.getEventType(event) == CommerceEventUtils.Constants.EVENT_TYPE_IMPRESSION) {
            return null;
        }
        List<CustomMapping.ProjectionResult> events = new LinkedList<CustomMapping.ProjectionResult>();
        EventWrapper.CommerceEventWrapper wrapper = new EventWrapper.CommerceEventWrapper(event);
        for (int i = 0; i < customMappingList.size(); i++) {
            CustomMapping customMapping = customMappingList.get(i);
            if (customMapping.isMatch(wrapper)) {
                List<CustomMapping.ProjectionResult> results = customMapping.project(wrapper);
                if (results != null) {
                    events.addAll(results);
                }
            }
        }
        if (events.isEmpty()) {
            if (defaultCommerceCustomMapping != null) {
                events.addAll(defaultCommerceCustomMapping.project(wrapper));
            } else {
                return null;
            }
        }
        return events;
    }

    boolean isMatch(EventWrapper wrapper) {
        if (mIsDefault) {
            return true;
        }
        for (CustomMappingMatch match : matchList) {
            if (!match.isMatch(wrapper)) {
                return false;
            }
        }
        return true;
    }

    public static List<CustomMapping.ProjectionResult> projectEvents(MPEvent event, boolean isScreenEvent, List<CustomMapping> customMappingList, CustomMapping defaultCustomMapping, CustomMapping defaultScreenCustomMapping) {
        List<CustomMapping.ProjectionResult> events = new LinkedList<CustomMapping.ProjectionResult>();

        EventWrapper.MPEventWrapper wrapper = new EventWrapper.MPEventWrapper(event, isScreenEvent);
        for (int i = 0; i < customMappingList.size(); i++) {
            CustomMapping customMapping = customMappingList.get(i);
            if (customMapping.isMatch(wrapper)) {
                List<CustomMapping.ProjectionResult> newEvents = customMapping.project(wrapper);
                if (newEvents != null) {
                    events.addAll(newEvents);
                }
            }
        }

        if (events.isEmpty()) {
            if (isScreenEvent) {
                if (defaultScreenCustomMapping != null) {
                    events.addAll(defaultScreenCustomMapping.project(wrapper));
                } else {
                    return null;
                }
            } else {
                if (defaultCustomMapping != null) {
                    events.addAll(defaultCustomMapping.project(wrapper));
                } else {
                    return null;
                }
            }
        }

        return events;
    }


    public static class ProjectionResult {
        private final MPEvent mEvent;
        private final int mProjectionId;
        private CommerceEvent mCommerceEvent;

        public ProjectionResult(MPEvent event, int projectionId) {
            mEvent = event;
            mCommerceEvent = null;
            mProjectionId = projectionId;
        }

        public ProjectionResult(CommerceEvent commerceEvent, int projectionId) {
            mCommerceEvent = commerceEvent;
            mEvent = null;
            mProjectionId = projectionId;
        }

        public int getProjectionId() {
            return mProjectionId;
        }

        public MPEvent getMPEvent() {
            return mEvent;
        }

        public CommerceEvent getCommerceEvent() {
            return mCommerceEvent;
        }
    }
}