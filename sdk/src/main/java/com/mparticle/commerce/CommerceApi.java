package com.mparticle.commerce;

import android.content.Context;

public class CommerceApi {

    private CommerceApi(){}

    Context mContext;
    public CommerceApi(Context context) {
        mContext = context;
    }

    public Cart cart() {
        return Cart.getInstance(mContext);
    }

}
