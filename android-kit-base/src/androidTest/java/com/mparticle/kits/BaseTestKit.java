package com.mparticle.kits;

import android.content.Context;

import java.util.List;
import java.util.Map;

public class BaseTestKit extends KitIntegration {
    static OnKitCreateListener onKitCreateListener;

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) throws IllegalArgumentException {
        if (onKitCreateListener != null) {
            onKitCreateListener.onKitCreate(this);
            onKitCreateListener = null;
        }
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    public interface OnKitCreateListener {
        void onKitCreate(BaseTestKit kitIntegration);
    }

    public static void setOnKitCreate(OnKitCreateListener listener) {
        onKitCreateListener = listener;
    }
}
