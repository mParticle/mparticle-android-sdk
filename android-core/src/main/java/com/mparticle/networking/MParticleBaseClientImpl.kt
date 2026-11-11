package com.mparticle.networking

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.mparticle.BuildConfig
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.internal.database.UploadSettings
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale

open class MParticleBaseClientImpl(private val mContext: Context?, private val mConfigManager: ConfigManager?) : MParticleBaseClient {
    private var mRequestHandler: BaseNetworkConnection
    private val mPreferences: SharedPreferences?

    @JvmField
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var mApiKey: String?

    init {
        mPreferences = mContext!!.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        mRequestHandler = NetworkConnection(mConfigManager, mPreferences)
        mApiKey = mConfigManager!!.apiKey
    }

    override var requestHandler: BaseNetworkConnection
        get() = mRequestHandler
        set(handler) {
            mRequestHandler = handler
        }

    @Throws(IOException::class)
    protected open fun makeUrlRequest(endpoint: Endpoint?, connection: MPConnection?): MPConnection? = makeUrlRequest(endpoint, connection, null)

    @Throws(IOException::class)
    protected open fun makeUrlRequest(endpoint: Endpoint?, connection: MPConnection?, payload: String?): MPConnection? =
        makeUrlRequest(endpoint, connection, payload, true)

    @Throws(IOException::class)
    protected open fun makeUrlRequest(endpoint: Endpoint?, connection: MPConnection?, identity: Boolean): MPConnection? =
        makeUrlRequest(endpoint, connection, null, identity)

    @Throws(IOException::class)
    open fun makeUrlRequest(endpoint: Endpoint?, connection: MPConnection?, payload: String?, identity: Boolean): MPConnection? =
        mRequestHandler.makeUrlRequest(endpoint, connection, payload, identity)

    protected open fun getHeaderDateString(): String {
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
        return format.format(Date())
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, UnsupportedEncodingException::class)
    protected open fun getHeaderHashString(request: MPConnection?, date: String?, message: String?, apiSecret: String?): String? {
        val method = request!!.requestMethod
        val path = request.url.defaultUrl.file
        val hashString =
            StringBuilder()
                .append(method)
                .append("\n")
                .append(date)
                .append("\n")
                .append(path)
        if (message != null) {
            hashString.append(message)
        }
        return MPUtility.hmacSha256Encode(apiSecret, hashString.toString())
    }

    open fun getNextRequestTime(endpoint: Endpoint?): Long = mPreferences!!.getLong(endpoint!!.name + ":" + Constants.PrefKeys.NEXT_REQUEST_TIME, 0)

    @Throws(MalformedURLException::class)
    protected open fun getUrl(endpoint: Endpoint?): MPUrl = getUrl(endpoint, null, null, null)

    @Throws(MalformedURLException::class)
    protected open fun getUrl(endpoint: Endpoint?, mpId: Long): MPUrl {
        val audienceQueryParams = HashMap<String, String>()
        audienceQueryParams["mpid"] = mpId.toString()
        return getUrl(endpoint, null, audienceQueryParams)
    }

    @Throws(MalformedURLException::class)
    protected open fun getUrl(endpoint: Endpoint?, identityPath: String?): MPUrl = getUrl(endpoint, identityPath, null, null)

    @Throws(MalformedURLException::class)
    protected open fun getUrl(endpoint: Endpoint?, identityPath: String?, audienceQueryParams: HashMap<String, String>?): MPUrl =
        getUrl(endpoint, identityPath, audienceQueryParams, null)

    @Throws(MalformedURLException::class)
    protected open fun getUrl(
        endpoint: Endpoint?,
        identityPath: String?,
        audienceQueryParams: HashMap<String, String>?,
        uploadSettings: UploadSettings?,
    ): MPUrl {
        val networkOptions =
            if (uploadSettings == null) mConfigManager!!.networkOptions else uploadSettings.networkOptions
        val domainMapping = networkOptions.getDomain(endpoint)
        val apiKey = if (uploadSettings == null) mApiKey else uploadSettings.apiKey
        val usingCustomBaseURL = !MPUtility.isEmpty(networkOptions.customBaseURL)

        val host = resolveHost(endpoint!!, networkOptions, domainMapping, apiKey)
        val url = host.url
        val defaultDomain = host.defaultDomain
        val isDefaultDomain = host.isDefaultDomain

        val uri: Uri
        val subdirectory: String
        val pathPrefix: String
        val pathPostfix: String?
        var overridesSubdirectory = domainMapping != null && domainMapping.isOverridesSubdirectory
        if (usingCustomBaseURL && overridesSubdirectory) {
            Logger.warning(
                "NetworkOptions: customBaseURL with overridesSubdirectory is unsupported for CDN routing; " +
                    "overridesSubdirectory will be ignored for " + endpoint.name + ".",
            )
            overridesSubdirectory = false
        }
        return when (endpoint) {
            Endpoint.CONFIG -> {
                pathPrefix = if (usingCustomBaseURL) "/config/v4/" else "$SERVICE_VERSION_4/"
                subdirectory = if (overridesSubdirectory) "" else pathPrefix
                pathPostfix = "$mApiKey/config"
                val builder =
                    Uri
                        .Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + pathPostfix)
                        .appendQueryParameter("av", MPUtility.getAppVersionName(mContext))
                        .appendQueryParameter("sv", Constants.MPARTICLE_VERSION)
                if (mConfigManager!!.dataplanId != null) {
                    builder.appendQueryParameter("plan_id", mConfigManager.dataplanId)
                    val dataplanVersion = mConfigManager.dataplanVersion
                    if (dataplanVersion != null) {
                        if (dataplanVersion in 1..1000) {
                            builder.appendQueryParameter("plan_version", mConfigManager.dataplanVersion.toString())
                        } else {
                            Logger.warning(
                                "Dataplan version of $dataplanVersion is out of range and will not be used to fetch remote dataplan. " +
                                    "Version must be between 1 and 1000.",
                            )
                        }
                    }
                }
                MPUrl.getUrl(
                    builder.build().toString(),
                    generateDefaultURL(isDefaultDomain, builder.build(), defaultDomain, "$SERVICE_VERSION_4/$pathPostfix"),
                )
            }

            Endpoint.EVENTS -> {
                pathPrefix = if (usingCustomBaseURL) "/nativeevents/v2/" else "$SERVICE_VERSION_2/"
                subdirectory = if (overridesSubdirectory) "" else pathPrefix
                pathPostfix = "$apiKey/events"
                uri =
                    Uri
                        .Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + pathPostfix)
                        .build()

                MPUrl.getUrl(
                    uri.toString(),
                    generateDefaultURL(isDefaultDomain, uri, defaultDomain, "$SERVICE_VERSION_2/$pathPostfix"),
                )
            }

            Endpoint.ALIAS -> {
                pathPrefix = if (usingCustomBaseURL) "/nativeevents/v1/identity/" else "$SERVICE_VERSION_1/identity/"
                subdirectory = if (overridesSubdirectory) "" else pathPrefix
                pathPostfix = "$apiKey/alias"
                uri =
                    Uri
                        .Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + pathPostfix)
                        .build()
                MPUrl.getUrl(
                    uri.toString(),
                    generateDefaultURL(isDefaultDomain, uri, defaultDomain, "$SERVICE_VERSION_1/identity/$pathPostfix"),
                )
            }

            Endpoint.IDENTITY -> {
                pathPrefix = if (usingCustomBaseURL) "/identity/v1/" else "$SERVICE_VERSION_1/"
                subdirectory = if (overridesSubdirectory) "" else pathPrefix
                pathPostfix = identityPath
                uri =
                    Uri
                        .Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(subdirectory + pathPostfix)
                        .build()
                MPUrl.getUrl(
                    uri.toString(),
                    generateDefaultURL(isDefaultDomain, uri, defaultDomain, "$SERVICE_VERSION_1/$pathPostfix"),
                )
            }

            Endpoint.AUDIENCE -> {
                pathPostfix =
                    if (usingCustomBaseURL) {
                        "/nativeevents/v1/$mApiKey/audience"
                    } else {
                        "$SERVICE_VERSION_1/$mApiKey/audience"
                    }
                uri =
                    Uri
                        .Builder()
                        .scheme(BuildConfig.SCHEME)
                        .encodedAuthority(url)
                        .path(pathPostfix)
                        .appendQueryParameter("mpid", mConfigManager!!.mpid.toString())
                        .build()
                MPUrl.getUrl(
                    uri.toString(),
                    generateDefaultURL(isDefaultDomain, uri, defaultDomain, "$SERVICE_VERSION_1/$mApiKey/audience"),
                )
            }
        }
    }

    /**
     * Generates a new URL using the default domain when domain mapping is specified.
     * This method creates a new URI based on the existing URI, but replaces the domain with the default domain.
     *
     * @param isDefaultDomain If the default domain is true, return null without generating a new URL.
     *                        If the default domain is false, generate and return a new URL.
     * @param uri             The existing URI which includes the domain mapping.
     * @param defaultDomain   The default domain name to be used in the new URI.
     * @param path            path to be used in the new URI.
     * @return A new URI with the same path and scheme as the original URI, but with the default domain.
     */
    @Throws(MalformedURLException::class)
    protected open fun generateDefaultURL(isDefaultDomain: Boolean, uri: Uri?, defaultDomain: String?, path: String?): MPUrl? {
        if (isDefaultDomain) {
            return null
        }
        if (uri != null) {
            val uriBuilder = Uri.parse(uri.toString()).buildUpon()
            if (!defaultDomain.isNullOrEmpty()) {
                uriBuilder.encodedAuthority(defaultDomain)
            }
            if (!path.isNullOrEmpty()) {
                uriBuilder.path(path)
            }
            return MPUrl.getUrl(uriBuilder.build().toString(), null)
        }
        return null
    }

    internal open fun getPodUrl(urlPrefix: String?, pod: String?, enablePodRedirection: Boolean): String? {
        if (urlPrefix != null) {
            val newUrl = if (enablePodRedirection) "$urlPrefix.$pod" else urlPrefix
            return "$newUrl.mparticle.com"
        }
        return null
    }

    /**
     * Resolves the host(s) used to build the endpoint URL. Returns three values:
     * `url` — host used for the request, `defaultDomain` — host used as the
     * fallback for [generateDefaultURL], and `isDefaultDomain` — true when
     * `url` is the unmodified mParticle default.
     *
     * Priority: `customBaseURL` → per-endpoint `DomainMapping` → default.
     */
    private fun resolveHost(endpoint: Endpoint, networkOptions: NetworkOptions, domainMapping: DomainMapping?, apiKey: String?): ResolvedHost {
        val defaultUrl = NetworkOptionsManager.getDefaultUrl(endpoint)
        val customBaseURL = networkOptions.customBaseURL

        if (!MPUtility.isEmpty(customBaseURL)) {
            if (domainMapping != null &&
                !MPUtility.isEmpty(domainMapping.url) &&
                defaultUrl != domainMapping.url
            ) {
                Logger.warning(
                    "NetworkOptions: customBaseURL is set; domain mapping for " + endpoint.name + " is ignored.",
                )
            }
            // When custom CNAME is used, the default-domain URL still needs the pod prefix
            // so MPConnectionTest matching and pinning fallbacks continue to work.
            val defaultDomain = if (endpoint == Endpoint.CONFIG) defaultUrl else getPodUrl(defaultUrl, null, false)
            return ResolvedHost(customBaseURL, defaultDomain, false)
        }

        val domainMappingUrl = domainMapping?.url
        val isDefaultDomain = MPUtility.isEmpty(domainMappingUrl) || defaultUrl == domainMappingUrl
        var url: String? = if (isDefaultDomain) defaultUrl else domainMappingUrl
        var defaultDomain: String? = defaultUrl

        if (endpoint != Endpoint.CONFIG) {
            if (isDefaultDomain) {
                // Default domain gets the pod prefix.
                url = getPodUrl(url, mConfigManager!!.getPodPrefix(apiKey.orEmpty()), mConfigManager.isDirectUrlRoutingEnabled)
            } else {
                // Domain-mapped: always generate the default with the original (un-pod-prefixed) host.
                defaultDomain = getPodUrl(defaultDomain, null, false)
            }
        }
        return ResolvedHost(url, defaultDomain, isDefaultDomain)
    }

    private class ResolvedHost(val url: String?, val defaultDomain: String?, val isDefaultDomain: Boolean)

    enum class Endpoint(value: Int) {
        CONFIG(1),
        IDENTITY(2),
        EVENTS(3),
        AUDIENCE(4),
        ALIAS(5),
        ;

        @JvmField
        var value: Int = value

        companion object {
            @JvmStatic
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            fun parseInt(value: Int): Endpoint? = when (value) {
                1 -> CONFIG
                2 -> IDENTITY
                3 -> EVENTS
                4 -> AUDIENCE
                5 -> ALIAS
                else -> null
            }
        }
    }

    companion object {
        private const val SERVICE_VERSION_1 = "/v1"
        private const val SERVICE_VERSION_2 = "/v2"
        private const val SERVICE_VERSION_4 = "/v4"

        protected const val REQUEST_ID: String = "request_id"
    }
}
