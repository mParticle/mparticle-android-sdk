package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sdozor on 3/27/14.
 */
class ProviderPersistence extends JSONObject{

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


    ProviderPersistence(JSONObject config, Context context, SharedPreferences mpPreferences) throws JSONException{
        super();
        JSONArray configPersistence = config.getJSONArray(KEY_PERSISTENCE);
        for (int i = 0; i < configPersistence.length(); i++){

            JSONObject values = new JSONObject();
            if (configPersistence.getJSONObject(i).has(KEY_PERSISTENCE_ANDROID)){
                JSONArray files = configPersistence.getJSONObject(i).getJSONArray(KEY_PERSISTENCE_ANDROID);

                for (int fileIndex = 0; fileIndex < files.length(); fileIndex++){
                    JSONObject fileObject = files.getJSONObject(fileIndex);
                    SharedPreferences preferences = context.getSharedPreferences(fileObject.getString(KEY_PERSISTENCE_FILE), fileObject.getInt(KEY_PERSISTENCE_MODE));
                    JSONArray fileObjects = fileObject.getJSONArray(KEY_PERSISTENCE_KEY_LIST);
                    SharedPreferences.Editor editor = preferences.edit();
                    for (int keyIndex = 0; keyIndex < fileObjects.length(); keyIndex++){
                        final int type = fileObjects.getJSONObject(keyIndex).getInt(KEY_PERSISTENCE_TYPE);
                        final String key = fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_KEY);
                        final String mpKey = fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_MPVAR);
                        final String mpPersistenceKey = MPPREFIX + mpKey;
                        boolean contains = preferences.contains(key);
                        boolean containsMpKey = preferences.contains(mpPersistenceKey);
                        Object defaultValue;
                        switch (type) {
                            case PERSISTENCE_TYPE_STRING:
                                defaultValue = applyMacro(fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_DEFAULT), mpPreferences);
                                if (contains) {
                                    values.put(mpKey, preferences.getString(key, (String) defaultValue));
                                } else {
                                    if (!containsMpKey){
                                        editor.putString(mpPersistenceKey , (String) defaultValue);
                                    }
                                    values.put(mpKey, preferences.getString(mpPersistenceKey, (String) defaultValue));
                                }
                                break;
                            case PERSISTENCE_TYPE_INT:
                                defaultValue = fileObjects.getJSONObject(keyIndex).getInt(KEY_PERSISTENCE_DEFAULT);
                                if (contains) {
                                    values.put(mpKey, preferences.getInt(key, (Integer) defaultValue));
                                } else {
                                    if (!containsMpKey){
                                        editor.putInt(mpPersistenceKey, (Integer) defaultValue);
                                    }
                                    values.put(mpKey, preferences.getInt(mpPersistenceKey, (Integer) defaultValue));
                                }
                                break;
                            case PERSISTENCE_TYPE_BOOLEAN:
                                defaultValue = fileObjects.getJSONObject(keyIndex).getBoolean(KEY_PERSISTENCE_DEFAULT);
                                if (contains) {
                                    values.put(mpKey, preferences.getBoolean(key, (Boolean) defaultValue));
                                } else {
                                    if (!containsMpKey){
                                        editor.putBoolean(mpPersistenceKey, (Boolean) defaultValue);
                                    }
                                    values.put(mpKey, preferences.getBoolean(mpPersistenceKey, (Boolean) defaultValue));
                                }
                                break;
                            case PERSISTENCE_TYPE_FLOAT:
                                defaultValue = fileObjects.getJSONObject(keyIndex).getDouble(KEY_PERSISTENCE_DEFAULT);
                                if (contains) {
                                    values.put(mpKey, preferences.getFloat(key, (Float) defaultValue));
                                } else {
                                    if (!containsMpKey){
                                        editor.putFloat(mpPersistenceKey, (Float) defaultValue);
                                    }
                                    values.put(mpKey, preferences.getFloat(mpPersistenceKey, (Float) defaultValue));
                                }
                                break;
                            case PERSISTENCE_TYPE_LONG:
                                defaultValue = fileObjects.getJSONObject(keyIndex).getLong(KEY_PERSISTENCE_DEFAULT);
                                if (contains) {
                                    values.put(mpKey, preferences.getLong(key, (Long) defaultValue));
                                } else {
                                    if (!containsMpKey){
                                        editor.putLong(mpPersistenceKey, (Long) defaultValue);
                                    }
                                    values.put(mpKey, preferences.getLong(mpPersistenceKey, (Long) defaultValue));
                                }
                                break;
                        }
                        editor.commit();
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


    private String applyMacro(String defaultString, SharedPreferences preferences) {
        if (!TextUtils.isEmpty(defaultString) && defaultString.startsWith("%")){
            if (defaultString.toUpperCase().equals(MACRO_GUID_NO_DASHES)){
                String value = preferences.getString(Constants.PrefKeys.MACRO_GN, null);
                if (value == null){
                    value = UUID.randomUUID().toString().replace("-", "");
                    preferences.edit().putString(Constants.PrefKeys.MACRO_GN, value).commit();
                }
                return value;
            }else if (defaultString.equals(MACRO_OMNITURE_AID)){
                String value = preferences.getString(Constants.PrefKeys.MACRO_OAID, null);
                if (value == null){
                    value = generateAID();
                    preferences.edit().putString(Constants.PrefKeys.MACRO_OAID, value).commit();
                }
                return value;
            }else if (defaultString.equals(MACRO_GUID)){
                String value = preferences.getString(Constants.PrefKeys.MACRO_G, null);
                if (value == null){
                    value = UUID.randomUUID().toString();
                    preferences.edit().putString(Constants.PrefKeys.MACRO_G, value).commit();
                }
                return value;
            }else if (defaultString.equals(MACRO_TIMESTAMP)){
                String value = preferences.getString(Constants.PrefKeys.MACRO_TS, null);
                if (value == null){
                    value = Long.toString(System.currentTimeMillis());
                    preferences.edit().putString(Constants.PrefKeys.MACRO_TS, value).commit();
                }
                return value;
            }else if (defaultString.equals(MACRO_GUID_LEAST_SIG)){
                String value = preferences.getString(Constants.PrefKeys.MACRO_GLSB, null);
                if (value == null){
                    value = Long.toString(UUID.randomUUID().getLeastSignificantBits());
                    preferences.edit().putString(Constants.PrefKeys.MACRO_GLSB, value).commit();
                }
                return value;
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
