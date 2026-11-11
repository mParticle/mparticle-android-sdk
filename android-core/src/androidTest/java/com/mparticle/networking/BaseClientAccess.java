package com.mparticle.networking;

import android.net.Uri;

import com.mparticle.internal.database.UploadSettings;

import java.net.MalformedURLException;
import java.util.HashMap;

/**
 * Same-package access to the protected URL members of {@link MParticleBaseClientImpl}.
 * Kotlin does not treat {@code protected} as package-visible, so the Kotlin tests reach these
 * members through this class. Do not widen the members in production code to avoid it.
 */
public final class BaseClientAccess {
    private BaseClientAccess() {}

    public static MPUrl getUrl(MParticleBaseClientImpl client, MParticleBaseClientImpl.Endpoint endpoint)
            throws MalformedURLException {
        return client.getUrl(endpoint);
    }

    public static MPUrl getUrl(MParticleBaseClientImpl client, MParticleBaseClientImpl.Endpoint endpoint, long mpId)
            throws MalformedURLException {
        return client.getUrl(endpoint, mpId);
    }

    public static MPUrl getUrl(MParticleBaseClientImpl client, MParticleBaseClientImpl.Endpoint endpoint, String identityPath)
            throws MalformedURLException {
        return client.getUrl(endpoint, identityPath);
    }

    public static MPUrl getUrl(
            MParticleBaseClientImpl client,
            MParticleBaseClientImpl.Endpoint endpoint,
            String identityPath,
            HashMap<String, String> audienceQueryParams)
            throws MalformedURLException {
        return client.getUrl(endpoint, identityPath, audienceQueryParams);
    }

    public static MPUrl getUrl(
            MParticleBaseClientImpl client,
            MParticleBaseClientImpl.Endpoint endpoint,
            String identityPath,
            HashMap<String, String> audienceQueryParams,
            UploadSettings uploadSettings)
            throws MalformedURLException {
        return client.getUrl(endpoint, identityPath, audienceQueryParams, uploadSettings);
    }

    public static MPUrl generateDefaultURL(
            MParticleBaseClientImpl client, boolean isDefaultDomain, Uri uri, String defaultDomain, String path)
            throws MalformedURLException {
        return client.generateDefaultURL(isDefaultDomain, uri, defaultDomain, path);
    }
}
