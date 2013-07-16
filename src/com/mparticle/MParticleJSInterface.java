package com.mparticle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.mparticle.MParticleAPI.EventType;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class MParticleJSInterface {
	private static final String TAG = Constants.LOG_TAG;
	private Context mContext;
	private MParticleAPI mApiInstance;
	
	public MParticleJSInterface(Context c, MParticleAPI apiInstance) {
		mContext = c;
		mApiInstance = apiInstance;
	}
	
	@JavascriptInterface
	public void logEvent(String data) {
		Log.d(TAG, "Received event from webview, processing...");
		
		try {
			JSONObject event = new JSONObject(data);

			mApiInstance.logEvent(event.getString("EventName"), 
					convertEventType(event.getInt("EventDataType")), 
					getEventAttributes(event.getJSONObject("EventAttributes")));
		} catch (JSONException e) {
			Log.w(TAG, "Error deserializing JSON event from webview");
		}
	}
	
	private Map<String,String> getEventAttributes(JSONObject attributes)
	{
		if(null != attributes) {
			Iterator keys = attributes.keys();
			
			Map<String,String> parsedAttributes = new HashMap<String,String>();
			
			while(keys.hasNext()) {
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
		switch(eventType) {
		case 1:
			return EventType.NAVIGATION;
		case 2:
			return EventType.PAGEVIEW;
		case 3:
			return EventType.SEARCH;
		case 4:
			return EventType.PURCHASE;
		case 5:
			return EventType.ACTION;
		default:
			return EventType.OTHER;
		}
	}
}
