package com.mparticle.kits;

import android.net.Uri;

import org.json.JSONObject;

interface ISurveyProvider {
    Uri getSurveyUrl(JSONObject userAttributes);
}
