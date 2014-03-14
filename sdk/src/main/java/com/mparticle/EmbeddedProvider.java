package com.mparticle;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by sdozor on 3/13/14.
 */
public abstract class EmbeddedProvider implements IEmbeddedKit {

    final static String KEY_ID = "id";
    private final static String KEY_PROPERTIES = "as";
    private final static String KEY_FILTERS = "hs";
    private final static String KEY_EVENT_TYPES = "et";
    private final static String KEY_EVENT_NAMES = "ec";
    private final static String KEY_EVENT_ATTRIBUTES = "ea";
    private final static int MAT = 32;

    protected HashMap<String, String> properties;
    protected HashMap<Long, Boolean> types;
    protected HashMap<Long, Boolean> names;
    protected HashMap<Long, Boolean> attributes;
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
        }
        if (json.has(KEY_FILTERS)){
            if (json.has(KEY_EVENT_TYPES)){
                types = convertToHashMap(json.getJSONObject(KEY_EVENT_TYPES));
            }
            if (json.has(KEY_EVENT_NAMES)){
                names = convertToHashMap(json.getJSONObject(KEY_EVENT_NAMES));
            }
            if (json.has(KEY_EVENT_ATTRIBUTES)){
                attributes = convertToHashMap(json.getJSONObject(KEY_EVENT_ATTRIBUTES));
            }
        }
        return this;
    }

    private HashMap<Long, Boolean> convertToHashMap(JSONObject json){
        HashMap<Long, Boolean> map = new HashMap<Long, Boolean>();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
            try {
                String key = iterator.next();
                map.put(Long.parseLong(key), json.getBoolean(key));
            }catch (JSONException jse){
                if (MParticle.getInstance().getDebugMode()){
                    Log.w(Constants.LOG_TAG, "Issue while parsing embedded kit configuration: " + jse.getMessage());
                }
            }
        }
        return map;
    }

    static final EmbeddedProvider createInstance(JSONObject json, Context context) throws JSONException, ClassNotFoundException{
        int id = json.getInt(KEY_ID);
        switch (id){
            case MAT:
                return new EmbeddedMAT(context).parseConfig(json).init();
            default:
                return null;
        }

    }

    protected abstract EmbeddedProvider init();
    public abstract String getName();

}
