package com.mparticle.mock;

import android.content.Context;

import com.mparticle.kits.KitIntegration;
import com.mparticle.kits.ReportingMessage;

import java.util.List;
import java.util.Map;


public class MockKit extends KitIntegration {
    @Override
    public String getName() {
        return "Mock Kit";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }
}
