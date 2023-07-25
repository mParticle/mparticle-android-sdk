package com.mparticle.internal.messages;

import static com.mparticle.internal.Constants.MessageKey.ALIAS_REQUEST_TYPE;
import static com.mparticle.internal.Constants.MessageKey.API_KEY;
import static com.mparticle.internal.Constants.MessageKey.DATA;
import static com.mparticle.internal.Constants.MessageKey.DESTINATION_MPID;
import static com.mparticle.internal.Constants.MessageKey.DEVICE_APPLICATION_STAMP_ALIAS;
import static com.mparticle.internal.Constants.MessageKey.END_TIME;
import static com.mparticle.internal.Constants.MessageKey.ENVIRONMENT_ALIAS;
import static com.mparticle.internal.Constants.MessageKey.REQUEST_ID;
import static com.mparticle.internal.Constants.MessageKey.REQUEST_TYPE;
import static com.mparticle.internal.Constants.MessageKey.SOURCE_MPID;
import static com.mparticle.internal.Constants.MessageKey.START_TIME;

import com.mparticle.MParticle;
import com.mparticle.identity.AliasRequest;
import com.mparticle.internal.ConfigManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class MPAliasMessage extends JSONObject {

    public MPAliasMessage(String jsonString) throws JSONException {
        super(jsonString);
    }

    public MPAliasMessage(AliasRequest request, String deviceApplicationStamp, String apiKey) throws JSONException {
        String environment = getStringValue(ConfigManager.getEnvironment());
        String requestId = UUID.randomUUID().toString();

        JSONObject dataJson = new JSONObject()
                .put(SOURCE_MPID, request.getSourceMpid())
                .put(DESTINATION_MPID, request.getDestinationMpid())
                .put(DEVICE_APPLICATION_STAMP_ALIAS, deviceApplicationStamp);

        if (request.getStartTime() != 0) {
            dataJson.put(START_TIME, request.getStartTime());
        }
        if (request.getEndTime() != 0) {
            dataJson.put(END_TIME, request.getEndTime());
        }

        put(DATA, dataJson);
        put(REQUEST_TYPE, ALIAS_REQUEST_TYPE);
        put(REQUEST_ID, requestId);
        put(ENVIRONMENT_ALIAS, environment);
        put(API_KEY, apiKey);
    }

    public AliasRequest getAliasRequest() throws JSONException {
        JSONObject data = getJSONObject(DATA);
        return AliasRequest.builder()
                .destinationMpid(data.getLong(DESTINATION_MPID))
                .sourceMpid(data.getLong(SOURCE_MPID))
                .endTime(data.optLong(END_TIME, 0))
                .startTime(data.optLong(START_TIME, 0))
                .build();
    }

    public String getRequestId() throws JSONException {
        return getString(REQUEST_ID);
    }


    protected String getStringValue(MParticle.Environment environment) {
        switch (environment) {
            case Development:
                return "development";
            case Production:
                return "production";
            default:
                return "";
        }
    }
}