package com.mparticle;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by sdozor on 11/25/14.
 */
public class EmbeddedForesee extends EmbeddedProvider implements ISurveyProvider{
    public EmbeddedForesee(Context context) throws ClassNotFoundException {
        super(context);
    }

    @Override
    public String getName() {
        return "Foresee";
    }

    @Override
    public boolean isOriginator(String uri) {
        return false;
    }

    @Override
    protected EmbeddedProvider update() {
        return null;
    }

    @Override
    public void logEvent(MParticle.EventType type, String name, Map<String, String> eventAttributes) throws Exception {

    }

    @Override
    public void logTransaction(MPProduct transaction) throws Exception {

    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {

    }

    @Override
    public void setLocation(Location location) {

    }

    @Override
    public void setUserAttributes(JSONObject mUserAttributes) {

    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {

    }

    @Override
    public void logout() {

    }

    @Override
    public void removeUserIdentity(String id) {

    }

    @Override
    public void handleIntent(Intent intent) {

    }

    @Override
    public void startSession() {

    }

    @Override
    public void endSession() {

    }

    @Override
    public String getSurveyUrl(JSONObject userAttributes) {
        return null;
    }
}
