package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for pulling persistence from files from *other* SDKs, and serializing itself as a part of a batch.
 * The idea here is that a customer may want to remove an SDK from their app and move it
 * server side via mParticle. Rather than start from stratch, it's crucial that we can query data that
 * the given SDK had been storing client-side.
 */
class ProviderPersistence extends JSONObject {

    static final String KEY_PERSISTENCE = "cms";
    private static final String KEY_PERSISTENCE_ID = "id";
    private static final String KEY_PERSISTENCE_ANDROID = "pr";
    private static final String KEY_PERSISTENCE_FILE = "f";
    private static final String KEY_PERSISTENCE_MODE = "m";
    private static final String KEY_PERSISTENCE_KEY_LIST = "ps";
    private static final String KEY_PERSISTENCE_KEY = "k";
    private static final String KEY_PERSISTENCE_TYPE = "t";
    private static final String KEY_PERSISTENCE_MPVAR = "n";
    private static final String KEY_PERSISTENCE_DEFAULT = "d";
    private static final String MPPREFIX = "mp::";

    private static final int PERSISTENCE_TYPE_STRING = 1;
    private static final int PERSISTENCE_TYPE_INT = 2;
    private static final int PERSISTENCE_TYPE_BOOLEAN = 3;
    private static final int PERSISTENCE_TYPE_FLOAT = 4;
    private static final int PERSISTENCE_TYPE_LONG = 5;


    ProviderPersistence(JSONObject config, Context context) throws JSONException {
        super();
        JSONArray configPersistence = config.getJSONArray(KEY_PERSISTENCE);
        for (int i = 0; i < configPersistence.length(); i++) {

            JSONObject values = new JSONObject();
            if (configPersistence.getJSONObject(i).has(KEY_PERSISTENCE_ANDROID)) {
                JSONArray files = configPersistence.getJSONObject(i).getJSONArray(KEY_PERSISTENCE_ANDROID);

                for (int fileIndex = 0; fileIndex < files.length(); fileIndex++) {
                    JSONObject fileObject = files.getJSONObject(fileIndex);
                    SharedPreferences preferences = context.getSharedPreferences(fileObject.getString(KEY_PERSISTENCE_FILE), fileObject.getInt(KEY_PERSISTENCE_MODE));
                    JSONArray fileObjects = fileObject.getJSONArray(KEY_PERSISTENCE_KEY_LIST);
                    SharedPreferences.Editor editor = preferences.edit();
                    for (int keyIndex = 0; keyIndex < fileObjects.length(); keyIndex++) {
                        final int type = fileObjects.getJSONObject(keyIndex).getInt(KEY_PERSISTENCE_TYPE);
                        final String key = fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_KEY);
                        final String mpKey = fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_MPVAR);
                        final String mpPersistenceKey = MPPREFIX + mpKey;
                        if (preferences.contains(mpPersistenceKey)) {
                            values.put(mpKey, preferences.getString(mpPersistenceKey, null));
                        } else {
                            String resolvedValue = null;
                            if (preferences.contains(key)) {
                                switch (type) {
                                    case PERSISTENCE_TYPE_STRING:
                                        resolvedValue = preferences.getString(key, resolvedValue);
                                        break;
                                    case PERSISTENCE_TYPE_INT:
                                        resolvedValue = Integer.toString(preferences.getInt(key, 0));
                                        break;
                                    case PERSISTENCE_TYPE_BOOLEAN:
                                        resolvedValue = Boolean.toString(preferences.getBoolean(key, false));
                                        break;
                                    case PERSISTENCE_TYPE_FLOAT:
                                        resolvedValue = Float.toString(preferences.getFloat(key, 0));
                                        break;
                                    case PERSISTENCE_TYPE_LONG:
                                        resolvedValue = Long.toString(preferences.getLong(key, 0));
                                        break;
                                }
                            } else {
                                resolvedValue = applyMacro(fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_DEFAULT));
                            }

                            editor.putString(mpPersistenceKey, resolvedValue);
                            editor.apply();
                            values.put(mpKey, resolvedValue);
                        }

                    }

                }
            }
            put(Integer.toString(configPersistence.getJSONObject(i).getInt(KEY_PERSISTENCE_ID)), values);

        }

    }

    private static final String MACRO_GUID_NO_DASHES = "%gn%";
    private static final String MACRO_OMNITURE_AID = "%oaid%";
    private static final String MACRO_GUID = "%g%";
    private static final String MACRO_TIMESTAMP = "%ts%";
    private static final String MACRO_GUID_LEAST_SIG = "%glsb%";

    /**
     * Macros are used so that the /config API call can come from a CDN (not user-specific).
     */
    private static String applyMacro(String defaultString) {
        if (!MPUtility.isEmpty(defaultString) && defaultString.startsWith("%")) {
            defaultString = defaultString.toLowerCase();
            if (defaultString.equalsIgnoreCase(MACRO_GUID_NO_DASHES)) {
                return UUID.randomUUID().toString().replace("-", "");
            } else if (defaultString.equals(MACRO_OMNITURE_AID)) {
                return generateAID();
            } else if (defaultString.equals(MACRO_GUID)) {
                return UUID.randomUUID().toString();
            } else if (defaultString.equals(MACRO_TIMESTAMP)) {
                return Long.toString(System.currentTimeMillis());
            } else if (defaultString.equals(MACRO_GUID_LEAST_SIG)) {
                return Long.toString(UUID.randomUUID().getLeastSignificantBits());
            }
        }
        return defaultString;
    }

    private static String generateAID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        uuid = uuid.toUpperCase();

        Pattern firstPattern = Pattern.compile("^[89A-F]");
        Pattern secondPattern = Pattern.compile("^[4-9A-F]");
        Matcher firstMatcher = firstPattern.matcher(uuid.substring(0, 16));
        Matcher secondMatcher = secondPattern.matcher(uuid.substring(16, 32));

        SecureRandom r = new SecureRandom();
        String vi_hi = firstMatcher.replaceAll(String.valueOf(r.nextInt(7)));
        String vi_lo = secondMatcher.replaceAll(String.valueOf(r.nextInt(3)));

        StringBuilder aidBuilder = new StringBuilder(33);
        aidBuilder.append(vi_hi);
        aidBuilder.append("-");
        aidBuilder.append(vi_lo);

        return aidBuilder.toString();
    }
}
