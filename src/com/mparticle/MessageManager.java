package com.mparticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.mparticle.MessageDatabase.MessageTable;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class MessageManager {

	private final static String TAG = "MParticle";

	private static MessageManager sMessageManager;
	private Context mContext;
	private MessageDatabase mDB;
	// temporary list for development/testing
	public List<JSONObject> messages = new ArrayList<JSONObject>();

	public interface MessageType {
		public static final String SESSION_START = "ss";
		public static final String SESSION_END = "se";
		public static final String CUSTOM_EVENT = "e";
		public static final String SCREEN_VIEW = "v";
		public static final String OPT_OUT = "o";
	}
	public interface MessageKey {
		public static final String TYPE = "dt";
		public static final String ID = "id";
		public static final String TIMESTAMP = "ct";
		public static final String APPLICATION_KEY = "a";
		public static final String APPLICATION_VERSION = "av";
		public static final String MPARTICLE_VERSION = "sdk";
		public static final String DATA_CONNECTION = "dct";
		public static final String LATITUDE = "lat";
		public static final String LONGITUDE = "lng";
		public static final String SESSION_ID = "sid";
		public static final String SESSION_LENGTH = "sls";
		public static final String ATTRIBUTES = "attrs";
		public static final String NAME = "n";
		// device keys
		public static final String DEVICE_ID = "duid";
		public static final String MANUFACTURER = "dma";
		public static final String PLATFORM = "dp";
		public static final String OS_VERSION = "dosv";
		public static final String MODEL = "dmdl";
		public static final String SCREEN_HEIGHT = "dsh";
		public static final String SCREEN_WIDTH = "dsw";
		public static final String DEVICE_COUNTRY = "dsw";
		public static final String DEVICE_LOCALE_COUNTRY = "dlc";
		public static final String DEVICE_LOCALE_LANGUAGE = "dll";
		// network keys
		public static final String NETWORK_COUNTRY = "nc";
		public static final String NETWORK_CARRIER = "nca";
		public static final String MOBILE_NETWORK_CODE = "mnc";
		public static final String MOBILE_COUNTRY_CODE = "mcc";
	}

	private MessageManager(Context context) {
		mContext = context.getApplicationContext();
		mDB = new MessageDatabase(mContext);
	}

	public static MessageManager getInstance(Context context) {
		if (null == MessageManager.sMessageManager) {
			MessageManager.sMessageManager = new MessageManager(context);
		}
		return MessageManager.sMessageManager;
    }

	public void handleMessage(String messageType, Map<String, String> eventData) {
		try {
			JSONObject eventObject = new JSONObject();
			eventObject.put(MessageKey.TYPE, messageType);
			eventObject.put(MessageKey.ID, UUID.randomUUID().toString());
			eventObject.put(MessageKey.TIMESTAMP, System.currentTimeMillis());
			if (null!=eventData) {
				eventObject.put(MessageKey.ATTRIBUTES, eventData);
			}
			messages.add(eventObject);

	        try {
	            SQLiteDatabase db = mDB.getWritableDatabase();
	            ContentValues values = new ContentValues();
	            values.put(MessageTable.MESSAGE_TYPE, messageType);
	            values.put(MessageTable.MESSAGE_TIME, eventObject.getLong(MessageKey.TIMESTAMP));
	            values.put(MessageTable.UUID, eventObject.getString(MessageKey.ID));
	            values.put(MessageTable.MESSAGE, eventObject.toString());
	            db.insert("messages", null, values);
	        } catch (SQLiteException e) {
	            Log.e(TAG, "Error saving event to mParticle DB", e);
	        } finally {
	            mDB.close();
	        }

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
