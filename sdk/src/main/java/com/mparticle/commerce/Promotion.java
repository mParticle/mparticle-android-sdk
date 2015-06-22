package com.mparticle.commerce;


public class Promotion {

    public static String CLICK = "click";
    public static String VIEW = "view";

    private String mCreative = null;
    private String mId = null;
    private String mName = null;
    private String mPosition = null;

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
}
