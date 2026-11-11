package com.mparticle.networking

import android.net.Uri
import com.mparticle.internal.database.UploadSettings

// Kotlin does not treat `protected` as package-visible, so the tests in this package reach the
// protected URL members through a Java helper. Members win over extensions where they are
// visible, so these only apply to the call sites that could not see the member.

fun MParticleBaseClientImpl.getUrl(endpoint: MParticleBaseClientImpl.Endpoint?): MPUrl = BaseClientAccess.getUrl(this, endpoint)

fun MParticleBaseClientImpl.getUrl(endpoint: MParticleBaseClientImpl.Endpoint?, mpId: Long): MPUrl = BaseClientAccess.getUrl(this, endpoint, mpId)

fun MParticleBaseClientImpl.getUrl(endpoint: MParticleBaseClientImpl.Endpoint?, identityPath: String?): MPUrl = BaseClientAccess.getUrl(this, endpoint, identityPath)

fun MParticleBaseClientImpl.getUrl(
    endpoint: MParticleBaseClientImpl.Endpoint?,
    identityPath: String?,
    audienceQueryParams: HashMap<String, String>?,
): MPUrl = BaseClientAccess.getUrl(this, endpoint, identityPath, audienceQueryParams)

fun MParticleBaseClientImpl.getUrl(
    endpoint: MParticleBaseClientImpl.Endpoint?,
    identityPath: String?,
    audienceQueryParams: HashMap<String, String>?,
    uploadSettings: UploadSettings?,
): MPUrl = BaseClientAccess.getUrl(this, endpoint, identityPath, audienceQueryParams, uploadSettings)

fun MParticleBaseClientImpl.generateDefaultURL(
    isDefaultDomain: Boolean,
    uri: Uri?,
    defaultDomain: String?,
    path: String?,
): MPUrl? = BaseClientAccess.generateDefaultURL(this, isDefaultDomain, uri, defaultDomain, path)
