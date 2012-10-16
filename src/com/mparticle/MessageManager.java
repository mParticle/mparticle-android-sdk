package com.mparticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class MessageManager {

	private static MessageManager sMessageManager;
	private Context mContext;
	public List<JSONObject> messages = new ArrayList<JSONObject>();

	private MessageManager(Context context) {
		mContext = context.getApplicationContext();
	}

	public static MessageManager getInstance(Context context) {
		if (null == MessageManager.sMessageManager) {
			MessageManager.sMessageManager = new MessageManager(context);
		}
		return MessageManager.sMessageManager;
    }

	public void handleEvent(String eventName, Map<String, String> eventData) {
		try {
			JSONObject eventObject = new JSONObject();
			eventObject.put("event_name", eventName);
			if (null!=eventData) {
				eventObject.put("event_data", eventData);
			}
			eventObject.put("time", System.currentTimeMillis());
			messages.add(eventObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
