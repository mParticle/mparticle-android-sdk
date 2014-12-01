package com.mparticle.internal.embedded;

import android.net.Uri;

import org.json.JSONObject;

/**
 * Created by sdozor on 11/25/14.
 */
interface ISurveyProvider {
    Uri getSurveyUrl(JSONObject userAttributes);
}
