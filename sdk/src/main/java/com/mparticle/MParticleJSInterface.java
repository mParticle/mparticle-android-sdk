package com.mparticle;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.mparticle.MParticle.EventType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Javascript interface to be used for {@code Webview} analytics.
 *
 */
public class MParticleJSInterface {
    private static final String TAG = Constants.LOG_TAG;
    private Context mContext;
    private MParticle mApiInstance;

    //the following keys are sent from the JS library as a part of each event
    private static String JS_KEY_EVENT_NAME = "EventName";
    private static String JS_KEY_EVENT_CATEGORY = "EventCategory";
    private static String JS_KEY_USER_ATTRIBUTES = "UserAttributes";
    private static String JS_KEY_SESSION_ATTRIBUTES = "SessionAttributes";
    private static String JS_KEY_USER_IDENTITIES = "UserIdentities";
    private static String JS_KEY_STORE = "Store";
    private static String JS_KEY_EVENT_ATTRIBUTES = "EventAttributes";
    private static String JS_KEY_SDK_VERSION = "SDKVersion";
    private static String JS_KEY_SESSION_ID = "SessionId";
    private static String JS_KEY_EVENT_DATATYPE = "EventDataType";
    private static String JS_KEY_DEBUG = "Debug";
    private static String JS_KEY_TIMESTAMP = "Timestamp";
    private static String JS_KEY_LOCATION = "Location";
    private static String JS_KEY_OPTOUT = "OptOut";

    public MParticleJSInterface(Context c, MParticle apiInstance) {
        mContext = c;
        mApiInstance = apiInstance;
    }

    @JavascriptInterface
    public void logEvent(String data) {
        Log.d(TAG, "Received event from WebView, processing...");

        try {
            JSONObject event = new JSONObject(data);

            mApiInstance.logEvent(event.getString(JS_KEY_EVENT_NAME),
                    convertEventType(event.getInt(JS_KEY_EVENT_CATEGORY)),
                    getEventAttributes(event.getJSONObject(JS_KEY_EVENT_ATTRIBUTES)));
        } catch (JSONException e) {
            Log.w(TAG, "Error deserializing JSON event from WebView: " + e.getMessage());
        }
    }

    private Map<String, String> getEventAttributes(JSONObject attributes) {
        if (null != attributes) {
            Iterator keys = attributes.keys();

            Map<String, String> parsedAttributes = new HashMap<String, String>();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                try {
                    parsedAttributes.put(key, attributes.getString(key));
                } catch (JSONException e) {
                    Log.w(TAG, "Could not parse event attribute value");
                }
            }

            return parsedAttributes;
        }

        return null;
    }


    private EventType convertEventType(int eventType) {
        switch (eventType) {
            case 1:
                return EventType.Navigation;
            case 2:
                return EventType.Location;
            case 3:
                return EventType.Search;
            case 4:
                return EventType.Transaction;
            case 5:
                return EventType.UserContent;
            case 6:
                return EventType.UserPreference;
            case 7:
                return EventType.Social;
            default:
                return EventType.Other;
        }
    }
}
