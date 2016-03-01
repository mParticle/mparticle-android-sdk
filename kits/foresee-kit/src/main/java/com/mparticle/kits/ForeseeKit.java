package com.mparticle.kits;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This is not really a true embedded kit - it only supports getSurveyUrl, which is Foresee's
 * primary use-case on the client-side. Everything else is forwarded server-side.
 *
 */
class ForeseeKit extends KitIntegration {

    public static final String ROOT_URL = "rootUrl";
    public static final String CLIENT_ID = "clientId";
    public static final String SURVEY_ID = "surveyId";
    public static final String SEND_APP_VERSION = "sendAppVersion";

    @Override
    public String getName() {
        return "Foresee";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        return null;
    }

    /**
     * example: http://survey.foreseeresults.com/survey/display?cid=8NNxB5BIVJdMBEBUBJ1Fpg==&sid=link&cpp[custid]=1234
     */
    @Override
    public Uri getSurveyUrl(Map<String, String> userAttributes) {
        String baseUrl = getSettings().get(ROOT_URL);
        if (baseUrl == null){
            return null;
        }

        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
        builder = builder
                .appendQueryParameter("cid", getSettings().get(CLIENT_ID))
                .appendQueryParameter("sid", getSettings().get(SURVEY_ID))
                .appendQueryParameter("rid", UUID.randomUUID().toString());

        StringBuilder cpps = new StringBuilder();
        if (Boolean.parseBoolean(getSettings().get(SEND_APP_VERSION))){
            try {
                PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
                String version = pInfo.versionName;
                cpps.append("cpp[appversion]=").append(version).append("&");
            }catch (PackageManager.NameNotFoundException nnfe){

            }
        }
        for (Map.Entry<String, String> entry : userAttributes.entrySet()){
            cpps.append("cpp[").append(KitUtils.sanitizeAttributeKey(entry.getKey())).append("]=").append(entry.getValue()).append("&");
        }
        //remove the extra &
        if (cpps.length() > 0){
            cpps.delete(cpps.length()-1, cpps.length());
        }

        builder.appendQueryParameter("cpps", cpps.toString());

        return builder.build();
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }
}
