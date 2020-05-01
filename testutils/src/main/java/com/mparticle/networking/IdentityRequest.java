package com.mparticle.networking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IdentityRequest extends Request {

    IdentityRequest(Request request) {
        super(request);
    }

    IdentityRequest(MPConnectionTestImpl connectionTest) {
        super(connectionTest);
    }

    public IdentityRequestBody getBody() {
        return IdentityRequestBody.from(getBodyJson());
    }

    public static class ClientSdk {
        public String platform;
        public String sdkVendor;
        public String sdkVersion;

        static ClientSdk from(JSONObject jsonObject) throws JSONException {
            ClientSdk clientSdk = new ClientSdk();
            clientSdk.platform = jsonObject.getString("platform");
            clientSdk.sdkVendor = jsonObject.getString("sdk_vendor");
            clientSdk.sdkVersion = jsonObject.getString("sdk_version");
            return clientSdk;
        }
    }

    public static class IdentityRequestBody {
        public ClientSdk clientSdk;
        public String context;
        public String environment;
        public String requestId;
        public Long requestTimestamp;
        public JSONObject known_identities;
        public Long previousMpid;
        public List<JSONObject> identity_changes = new ArrayList<>();

        public static IdentityRequestBody from(JSONObject jsonObject) {
            try {
                IdentityRequestBody identityRequest = new IdentityRequestBody();
                if (jsonObject.has("client_sdk")) {
                    identityRequest.clientSdk = ClientSdk.from(jsonObject.getJSONObject("client_sdk"));
                }
                identityRequest.context = jsonObject.optString("context");
                identityRequest.environment = jsonObject.optString("environment");
                identityRequest.known_identities = jsonObject.optJSONObject("known_identities");
                identityRequest.requestTimestamp = Long.valueOf(jsonObject.getString("request_timestamp_ms"));
                identityRequest.requestId = jsonObject.getString("request_id");
                identityRequest.previousMpid = Long.valueOf(jsonObject.optString("previous_mpid", "0"));
                JSONArray jsonArray = jsonObject.optJSONArray("identity_changes");
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        identityRequest.identity_changes.add(jsonArray.getJSONObject(i));
                    }
                }
                return identityRequest;
            } catch (JSONException jse) {
                throw new RuntimeException(jse);
            }
        }
    }
}