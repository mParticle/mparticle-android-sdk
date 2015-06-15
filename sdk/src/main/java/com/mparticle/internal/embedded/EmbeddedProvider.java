package com.mparticle.internal.embedded;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.SparseBooleanArray;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base EmbeddedProvider - all EKs subclass this. The primary function is to parse the common EK configuration structures
 * such as filters.
 */
abstract class EmbeddedProvider {

    final static String KEY_ID = "id";
    private final static String KEY_PROPERTIES = "as";
    private final static String KEY_FILTERS = "hs";
    private final static String KEY_BRACKETING = "bk";
    private final static String KEY_EVENT_LIST = "eventList";
    private final static String KEY_ATTRIBUTE_LIST = "attributeList";
    private final static String KEY_EVENT_TYPES_FILTER = "et";
    private final static String KEY_EVENT_NAMES_FILTER = "ec";
    private final static String KEY_EVENT_ATTRIBUTES_FILTER = "ea";
    private final static String KEY_SCREEN_NAME_FILTER = "svec";
    private final static String KEY_SCREEN_ATTRIBUTES_FILTER = "svea";
    private final static String KEY_USER_IDENTITY_FILTER = "uid";
    private final static String KEY_USER_ATTRIBUTE_FILTER = "ua";
    private final static String KEY_BRACKETING_LOW = "lo";
    private final static String KEY_BRACKETING_HIGH = "hi";

    //If set to true, our sdk honor user's optout wish. If false, we still collect data on opt-ed out users, but only for reporting
    private static final String HONOR_OPT_OUT = "honorOptOut";
    private static final String KEY_PROJECTIONS = "pr";
    protected final EmbeddedKitManager mEkManager;

    protected HashMap<String, String> properties = new HashMap<String, String>(0);
    protected SparseBooleanArray mTypeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mNameFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mScreenNameFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mScreenAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mUserIdentityFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mUserAttributeFilters = new SparseBooleanArray(0);
    protected HashSet<String> includedEvents, includedAttributes;
    protected int lowBracket = 0;
    protected int highBracket = 101;

    protected Context context;
    private boolean mRunning = true;
    private LinkedList<Projection> projectionList;
    private Projection defaultProjection = null;

    public EmbeddedProvider(EmbeddedKitManager ekManager) {
        this.mEkManager = ekManager;
        this.context = ekManager.getContext();
    }

    protected EmbeddedProvider parseConfig(JSONObject json) throws JSONException {

        if (json.has(KEY_PROPERTIES)){
            JSONObject propJson = json.getJSONObject(KEY_PROPERTIES);
            for (Iterator<String> iterator = propJson.keys(); iterator.hasNext();) {
                String key = iterator.next();
                properties.put(key, propJson.getString(key));
            }
            if (propJson.has(KEY_EVENT_LIST)){
                try {
                    JSONArray inclusions = new JSONArray(propJson.getString(KEY_EVENT_LIST));
                    includedEvents = new HashSet<String>(inclusions.length());
                    for (int i = 0; i < inclusions.length(); i++){
                        includedEvents.add(inclusions.getString(i).toLowerCase());
                    }
                }catch (JSONException jse){

                }
            }
            if (propJson.has(KEY_ATTRIBUTE_LIST)){
                try {
                    JSONArray inclusions = new JSONArray(propJson.getString(KEY_ATTRIBUTE_LIST));
                    includedAttributes = new HashSet<String>(inclusions.length());
                    for (int i = 0; i < inclusions.length(); i++){
                        includedAttributes.add(inclusions.getString(i).toLowerCase());
                    }
                }catch (JSONException jse){

                }
            }
        }
        if (json.has(KEY_FILTERS)){
            JSONObject filterJson = json.getJSONObject(KEY_FILTERS);
            if (filterJson.has(KEY_EVENT_TYPES_FILTER)){
                mTypeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_EVENT_TYPES_FILTER));
            }else {
                mTypeFilters.clear();
            }
            if (filterJson.has(KEY_EVENT_NAMES_FILTER)){
                mNameFilters = convertToSparseArray(filterJson.getJSONObject(KEY_EVENT_NAMES_FILTER));
            }else{
                mNameFilters.clear();
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTES_FILTER)){
                mAttributeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_EVENT_ATTRIBUTES_FILTER));
            }else{
                mAttributeFilters.clear();
            }
            if (filterJson.has(KEY_SCREEN_NAME_FILTER)){
                mScreenNameFilters = convertToSparseArray(filterJson.getJSONObject(KEY_SCREEN_NAME_FILTER));
            }else{
                mScreenNameFilters.clear();
            }
            if (filterJson.has(KEY_SCREEN_ATTRIBUTES_FILTER)){
                mScreenAttributeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_SCREEN_ATTRIBUTES_FILTER));
            }else{
                mScreenAttributeFilters.clear();
            }
            if (filterJson.has(KEY_USER_IDENTITY_FILTER)){
                mUserIdentityFilters = convertToSparseArray(filterJson.getJSONObject(KEY_USER_IDENTITY_FILTER));
            }else{
                mUserIdentityFilters.clear();
            }
            if (filterJson.has(KEY_USER_ATTRIBUTE_FILTER)){
                mUserAttributeFilters = convertToSparseArray(filterJson.getJSONObject(KEY_USER_ATTRIBUTE_FILTER));
            }else{
                mUserAttributeFilters.clear();
            }
        }

        if (json.has(KEY_BRACKETING)){
            JSONObject bracketing = json.getJSONObject(KEY_BRACKETING);
            lowBracket = bracketing.optInt(KEY_BRACKETING_LOW, 0);
            highBracket = bracketing.optInt(KEY_BRACKETING_HIGH, 101);
        }else{
            lowBracket = 0;
            highBracket = 101;
        }
        projectionList = new LinkedList<Projection>();
        defaultProjection = null;
        if (json.has(KEY_PROJECTIONS)){
            JSONArray projections = json.getJSONArray(KEY_PROJECTIONS);
            for (int i = 0; i < projections.length(); i++){
                Projection projection = new Projection(projections.getJSONObject(i));
                if (projection.isDefault()){
                    defaultProjection = projection;
                }else {
                    projectionList.add(projection);
                }
            }
        }
        if (defaultProjection == null){
            projectionList = null;
        }

        return this;
    }

    private SparseBooleanArray convertToSparseArray(JSONObject json){
        SparseBooleanArray map = new SparseBooleanArray();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getInt(key) == 1);
            }catch (JSONException jse){
                ConfigManager.log(MParticle.LogLevel.ERROR, "Issue while parsing embedded kit configuration: " + jse.getMessage());
            }
        }
        return map;
    }

    private boolean shouldHonorOptOut() {
        if (properties.containsKey(HONOR_OPT_OUT)){
            String optOut = properties.get(HONOR_OPT_OUT);
            return Boolean.parseBoolean(optOut);
        }
        return true;
    }

    public boolean disabled(){
        return !passesBracketing()
                || (shouldHonorOptOut() && !mEkManager.getConfigurationManager().isEnabled());
    }

    private boolean passesBracketing() {
        int userBucket = mEkManager.getConfigurationManager().getUserBucket();
        return userBucket >= lowBracket && userBucket < highBracket;
    }

    protected boolean shouldLogEvent(MPEvent event){
        int typeHash = MPUtility.mpHash(event.getEventType().ordinal() + "");
        return mTypeFilters.get(typeHash, true) && mNameFilters.get(event.getEventHash(), true);
    }

    public boolean shouldLogScreen(String screenName) {
        int nameHash = MPUtility.mpHash("0" + screenName);
        if (mScreenNameFilters.size() > 0 && !mScreenNameFilters.get(nameHash, true)){
            return false;
        }
        return true;
    }

    protected Map<String, String> filterEventAttributes(MParticle.EventType eventType, String eventName, SparseBooleanArray filter, Map<String, String> eventAttributes){
        if (eventAttributes != null && eventAttributes.size() > 0 && filter != null && filter.size() > 0) {

            String eventTypeStr = "0";
            if (eventType != null){
                eventTypeStr = eventType.ordinal() + "";
            }
            Iterator<Map.Entry<String, String>> attIterator = eventAttributes.entrySet().iterator();
            Map<String, String> newAttributes = new HashMap<String, String>();
            while (attIterator.hasNext()) {
                Map.Entry<String, String> entry = attIterator.next();
                String key = entry.getKey();
                int hash = MPUtility.mpHash(eventTypeStr + eventName + key);
                if (filter.get(hash, true)) {
                    newAttributes.put(key, entry.getValue());
                }
            }
            return newAttributes;
        }else{
            return eventAttributes;
        }
    }

    public abstract String getName();

    public abstract boolean isOriginator(String uri);

    protected abstract EmbeddedProvider update();

    public JSONObject filterAttributes(SparseBooleanArray attributeFilters, JSONObject attributes) {
        if (attributes != null && attributeFilters != null && attributeFilters.size() > 0
                && attributes.length() > 0) {
            Iterator<String> attIterator = attributes.keys();
            JSONObject newAttributes = new JSONObject();
            while (attIterator.hasNext()) {
                String entry = attIterator.next();
                int hash = MPUtility.mpHash(entry);
                if (attributeFilters.get(hash, true)) {
                    try {
                        newAttributes.put(entry, attributes.getString(entry));
                    }catch (JSONException jse){

                    }
                }
            }
            return newAttributes;
        }else{
            return attributes;
        }
    }

    public void logEvent(MPEvent event) throws Exception {

    }
    public void logTransaction(MPProduct transaction) throws Exception {

    }
    void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {

    }
    void setLocation(Location location) {

    }
    void setUserAttributes(JSONObject mUserAttributes) {

    }
    void removeUserAttribute(String key) {

    }
    void setUserIdentity(String id, MParticle.IdentityType identityType) {

    }
    void logout() {

    }
    void removeUserIdentity(String id) {

    }
    void handleIntent(Intent intent) {

    }
    void startSession() {

    }
    void endSession() {

    }

    public boolean isRunning() {
        return mRunning;
    }

    public boolean shouldSetIdentity(MParticle.IdentityType identityType) {
        return mUserIdentityFilters == null || mUserIdentityFilters.size() == 0 || mUserIdentityFilters.get(identityType.getValue(), true);
    }

    public void setRunning(boolean running) {
        this.mRunning = running;
    }

    public List<MPEvent> projectEvents(MPEvent event) {
        List<MPEvent> events = new LinkedList<MPEvent>();

        if (defaultProjection == null) {
            events.add(event);
            return events;
        }

        MPEventWrapper wrapper = new MPEventWrapper(event);
        for (int i = 0; i < projectionList.size(); i++){
            Projection projection = projectionList.get(i);
            if (projection.isMatch(wrapper)){
                MPEvent newEvent = projection.project(wrapper);
                if (newEvent != null) {
                    events.add(newEvent);
                }
            }
        }

        if (events.isEmpty()){
            events.add(defaultProjection.project(wrapper));
        }

        return events;
    }

    class MPEventWrapper {
        private final MPEvent mEvent;

        public MPEventWrapper(MPEvent event) {
            this.mEvent = event;
        }

        private Map<Integer, String> attributeHashes;

        public Map<Integer, String> getAttributeHashes() {
            if (attributeHashes == null) {
                attributeHashes = new HashMap<Integer, String>();
                for (Map.Entry<String, String> entry : mEvent.getInfo().entrySet()) {
                    int hash = MPUtility.mpHash(mEvent.getEventType().ordinal() + mEvent.getEventName() + entry.getKey());
                    attributeHashes.put(hash, entry.getKey());
                }
            }
            return attributeHashes;
        }

        public MPEvent getEvent() {
            return mEvent;
        }
    }
}
