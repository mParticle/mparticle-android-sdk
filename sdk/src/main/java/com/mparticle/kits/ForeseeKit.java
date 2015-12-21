package com.mparticle.kits;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.UUID;

/**
 * This is not really a true embedded kit - it only supports getSurveyUrl, which is Foresee's
 * primary use-case on the client-side. Everything else is forwarded server-side.
 *
 */
class ForeseeKit extends AbstractKit implements ISurveyProvider {

    public static final String ROOT_URL = "rootUrl";
    public static final String CLIENT_ID = "clientId";
    public static final String SURVEY_ID = "surveyId";
    public static final String SEND_APP_VERSION = "sendAppVersion";

    @Override
    public String getName() {
        return "Foresee";
    }

    @Override
    public boolean isOriginator(String uri) {
        return false;
    }

    @Override
    protected AbstractKit update() {
        return this;
    }

    /**
     *
     *
     * example: http://survey.foreseeresults.com/survey/display?cid=8NNxB5BIVJdMBEBUBJ1Fpg==&sid=link&cpp[custid]=1234
     */
    @Override
    public Uri getSurveyUrl(JSONObject userAttributes) {
        String baseUrl = properties.get(ROOT_URL);
        if (baseUrl == null){
            return null;
        }

        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
        builder = builder
                .appendQueryParameter("cid",properties.get(CLIENT_ID))
                .appendQueryParameter("sid", properties.get(SURVEY_ID))
                .appendQueryParameter("rid", UUID.randomUUID().toString());

        StringBuilder cpps = new StringBuilder();
        if (Boolean.parseBoolean(properties.get(SEND_APP_VERSION))){
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                String version = pInfo.versionName;
                cpps.append("cpp[appversion]=").append(version).append("&");
            }catch (PackageManager.NameNotFoundException nnfe){

            }
        }

        Iterator<?> keys = userAttributes.keys();

        while( keys.hasNext() ){
            String key = (String)keys.next();
            try {
                Object value = userAttributes.get(key);
                String strValue = "";

                if (value instanceof String) {
                    strValue = (String) value;
                }else if (value instanceof Integer){
                    strValue = Integer.toString((Integer)value);
                }else if (value instanceof Boolean){
                    strValue = Boolean.toString((Boolean)value);
                }else if (value instanceof Double){
                    strValue = Double.toString((Double)value);
                }else if (value != null){
                    strValue = value.toString();
                }
                cpps.append("cpp[").append(key).append("]=").append(strValue).append("&");
            }catch (Exception e){

            }
        }

        //remove the extra &
        if (cpps.length() > 0){
            cpps.delete(cpps.length()-1, cpps.length());
        }

        builder.appendQueryParameter("cpps", cpps.toString());

        return builder.build();
    }
}
