package com.mparticle.internal.embedded;

import android.net.Uri;

import org.json.JSONObject;

interface ISurveyProvider {
    Uri getSurveyUrl(JSONObject userAttributes);
}
