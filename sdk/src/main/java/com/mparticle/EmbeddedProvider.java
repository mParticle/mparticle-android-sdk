package com.mparticle;

import android.content.Context;
import android.util.SparseBooleanArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by sdozor on 3/13/14.
 */
abstract class EmbeddedProvider implements IEmbeddedKit {

    final static String KEY_ID = "id";
    private final static String KEY_PROPERTIES = "as";
    private final static String KEY_FILTERS = "hs";
    private final static String KEY_EVENT_LIST = "eventList";
    private final static String KEY_ATTRIBUTE_LIST = "attributeList";
    private final static String KEY_EVENT_TYPES = "et";
    private final static String KEY_EVENT_NAMES = "ec";
    private final static String KEY_EVENT_ATTRIBUTES = "ea";

    //If set to true, our sdk honor user's optout wish. If false, we still collect data on opt-ed out users, but only for reporting
    private static final String HONOR_OPT_OUT = "honorOptOut";

    protected HashMap<String, String> properties = new HashMap<String, String>(0);
    protected SparseBooleanArray types = new SparseBooleanArray(0);
    protected SparseBooleanArray names = new SparseBooleanArray(0);
    protected SparseBooleanArray attributes = new SparseBooleanArray(0);
    protected HashSet<String> includedEvents, includedAttributes;

    protected Context context;

    public EmbeddedProvider(Context context) throws ClassNotFoundException{
        this.context = context;
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
            if (json.has(KEY_EVENT_TYPES)){
                types = convertToSparseArray(json.getJSONObject(KEY_EVENT_TYPES));
            }
            if (json.has(KEY_EVENT_NAMES)){
                names = convertToSparseArray(json.getJSONObject(KEY_EVENT_NAMES));
            }
            if (json.has(KEY_EVENT_ATTRIBUTES)){
                attributes = convertToSparseArray(json.getJSONObject(KEY_EVENT_ATTRIBUTES));
            }
        }

        return this;
    }

    private SparseBooleanArray convertToSparseArray(JSONObject json){
        SparseBooleanArray map = new SparseBooleanArray();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getBoolean(key));
            }catch (JSONException jse){
                ConfigManager.log(MParticle.LogLevel.ERROR, "Issue while parsing embedded kit configuration: " + jse.getMessage());
            }
        }
        return map;
    }

    public boolean optedOut(){
        return Boolean.parseBoolean(properties.containsKey(HONOR_OPT_OUT) ? properties.get(HONOR_OPT_OUT) : "true")
                && !MParticle.getInstance().mConfigManager.getSendOoEvents();
    }

    private static int hash(String input) {
        int hash = 0;

        if (input == null || input.length() == 0)
            return hash;

        char[] chars = input.toLowerCase().toCharArray();

        for (char c : chars) {
            hash = ((hash << 5) - hash) + c;
        }

        return hash;
    }

    protected boolean shouldSend(MParticle.EventType type, String name){
        int typeHash = hash(type.toString());
        int typeNameHash = hash(type.toString() + name);
        return types.get(typeHash, true) && names.get(typeNameHash, true);
    }

    protected JSONObject filterAttributes(MParticle.EventType type, String name, JSONObject eventAttributes){
        Iterator attIterator = eventAttributes.keys();
        String nameType = type + name;
        while (attIterator.hasNext()){
            String attributeKey = (String)attIterator.next();
            int hash = hash(nameType + attributeKey);
            if (!attributes.get(hash, true)){
                attIterator.remove();
            }
        }
        return eventAttributes;
    }

    public abstract String getName();


    public abstract boolean isOriginator(String uri);

    protected abstract EmbeddedProvider update();
}
