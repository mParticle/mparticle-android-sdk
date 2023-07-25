package com.mparticle.networking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

public final class Certificate {
    private String alias;
    private String certificate;

    private Certificate(String alias, String certificate) {
        this.alias = alias;
        this.certificate = certificate;
    }

    @Nullable
    public static Certificate with(@NonNull String alias, @NonNull String certificate) {
        if (MPUtility.isEmpty(alias) || MPUtility.isEmpty(certificate)) {
            Logger.warning(String.format("Alias and Certificate values must both be non-empty strings. Unable to build certificate with Alias = %s and Certificate = %s.", alias, certificate));
            return null;
        }
        return new Certificate(alias, certificate);
    }

    @NonNull
    public String getAlias() {
        return alias;
    }

    @NonNull
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
    @NonNull
    public String toString() {
        return toJson().toString();
    }

    JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            return new JSONObject()
                    .put("alias", getAlias())
                    .put("certificate", getCertificate());
        } catch (JSONException jse) {
            Logger.error(jse);
        }
        return jsonObject;
    }
}