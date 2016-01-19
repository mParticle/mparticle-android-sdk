package com.mparticle.kits;

import android.app.Activity;
import android.util.SparseBooleanArray;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by sdozor on 6/15/15.
 */
public class FakeProvider extends AbstractKit {
    public FakeProvider() {
        mTypeFilters = new MockSparseBooleanArray();
        mNameFilters = new MockSparseBooleanArray();
        mAttributeFilters = new MockSparseBooleanArray();
        mScreenNameFilters = new MockSparseBooleanArray();
        mScreenAttributeFilters = new MockSparseBooleanArray();
        mUserIdentityFilters = new MockSparseBooleanArray();
        mUserAttributeFilters = new MockSparseBooleanArray();
        mCommerceAttributeFilters = new MockSparseBooleanArray();
        mCommerceEntityFilters = new MockSparseBooleanArray();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isOriginator(String uri) {
        return false;
    }

    @Override
    protected AbstractKit update() {
        return null;
    }

    @Override
    public Object getInstance(Activity activity) {
        return null;
    }

    @Override
    protected SparseBooleanArray convertToSparseArray(JSONObject json) {
        SparseBooleanArray map = new MockSparseBooleanArray();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getInt(key) == 1);
            }catch (JSONException jse){
                ConfigManager.log(MParticle.LogLevel.ERROR, "Issue while parsing kit configuration: " + jse.getMessage());
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

