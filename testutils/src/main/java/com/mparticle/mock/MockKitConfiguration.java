package com.mparticle.mock;

import android.util.SparseBooleanArray;

import com.mparticle.internal.Logger;
import com.mparticle.kits.KitConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is necessary b/c SparseBooleanArray is not available while unit testing.
 */
public class MockKitConfiguration extends KitConfiguration {

    @Override
    public KitConfiguration parseConfiguration(JSONObject json) throws JSONException {
        mTypeFilters = new MockSparseBooleanArray();
        mNameFilters = new MockSparseBooleanArray();
        mAttributeFilters = new MockSparseBooleanArray();
        mScreenNameFilters = new MockSparseBooleanArray();
        mScreenAttributeFilters = new MockSparseBooleanArray();
        mUserIdentityFilters = new MockSparseBooleanArray();
        mUserAttributeFilters = new MockSparseBooleanArray();
        mCommerceAttributeFilters = new MockSparseBooleanArray();
        mCommerceEntityFilters = new MockSparseBooleanArray();
        return super.parseConfiguration(json);
    }

    public static KitConfiguration createKitConfiguration(JSONObject json) throws JSONException{
        return new MockKitConfiguration().parseConfiguration(json);
    }

    public static KitConfiguration createKitConfiguration() throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 42);
        return new MockKitConfiguration().parseConfiguration(jsonObject);
    }

    @Override
    protected SparseBooleanArray convertToSparseArray(JSONObject json) {
        SparseBooleanArray map = new MockSparseBooleanArray();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getInt(key) == 1);
            }catch (JSONException jse){
                Logger.error("Issue while parsing kit configuration: " + jse.getMessage());
            }
        }
        return map;
    }
    class MockSparseBooleanArray extends SparseBooleanArray {
        @Override
        public boolean get(int key) {
            return get(key, false);
        }

        @Override
        public boolean get(int key, boolean valueIfKeyNotFound) {
            System.out.print("SparseArray getting: " + key);
            if (map.containsKey(key)) {
                return map.get(key);
            }else{
                return valueIfKeyNotFound;
            }
        }

        Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
        @Override
        public void put(int key, boolean value) {
            map.put(key, value);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }
}