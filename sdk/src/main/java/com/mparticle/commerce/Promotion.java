package com.mparticle.commerce;


import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

public class Promotion {

    public static String CLICK = "click";
    public static String VIEW = "view";

    private String mCreative = null;
    private String mId = null;
    private String mName = null;
    private String mPosition = null;

    public Promotion() {
        super();
    }

    public Promotion(Promotion promotion) {
        if (promotion != null) {
            mCreative = promotion.getCreative();
            mId = promotion.getId();
            mName = promotion.getName();
            mPosition = promotion.getPosition();
        }
    }

    public String getCreative() {
        return mCreative;
    }

    public Promotion setCreative(String creative) {
        mCreative = creative;
        return this;
    }

    public String getId() {
        return mId;
    }

    public Promotion setId(String id) {
        mId = id;
        return this;
    }

    public String getName() {
        return mName;
    }

    public Promotion setName(String name) {
        mName = name;
        return this;
    }

    public String getPosition() {
        return mPosition;
    }

    public Promotion setPosition(String position) {
        mPosition = position;
        return this;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            if (!MPUtility.isEmpty(mId)) {
                json.put("id", mId);
            }
            if (!MPUtility.isEmpty(mName)) {
                json.put("nm", mName);
            }
            if (!MPUtility.isEmpty(mCreative)) {
                json.put("cr", mCreative);
            }
            if (!MPUtility.isEmpty(mPosition)) {
                json.put("ps", mPosition);
            }
        } catch (JSONException jse) {

        }
        return json;
    }
}
