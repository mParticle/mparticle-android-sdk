package com.mparticle.networking;

import android.util.Base64;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

public final class Certificate{
    private String alias;
    private String certificate;

    private Certificate(String alias, String certificate) {
        this.alias = alias;
        this.certificate = certificate;
    }

    public static Certificate with(String alias, String certificate) {
        if (MPUtility.isEmpty(alias) || MPUtility.isEmpty(certificate)) {
            Logger.warning(String.format("Alias and Certificate values must both be non-empty strings. Unable to build certificate with Alias = %s and Certificate = %s.", alias, certificate));
            return null;
        }
        return new Certificate(alias, certificate);
    }

    public String getAlias() {
        return alias;
    }

    public String getCertificate() {
        return certificate;
    }

    static Certificate withCertificate(JSONObject jsonObject) {
        try {
            String alias = jsonObject.getString("alias");
            String certificate = jsonObject.getString("certificate");
            return new Certificate(alias, certificate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            return new JSONObject()
                    .put("alias", getAlias())
                    .put("certificate", getCertificate());
        }
        catch (JSONException jse) {
            Logger.error(jse);
        }
        return jsonObject;
    }
}