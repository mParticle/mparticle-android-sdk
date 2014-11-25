package com.mparticle;

import org.json.JSONObject;

/**
 * Created by sdozor on 11/25/14.
 */
interface ISurveyProvider {
    String getSurveyUrl(JSONObject userAttributes);
}
