package com.mparticle.networking

import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RestrictTo
import com.mparticle.BuildConfig
import com.mparticle.MParticle
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Logger
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.util.HashSet
import java.util.zip.GZIPOutputStream
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

open class NetworkConnection internal constructor(private var mConfigManager: ConfigManager?, sharedPreferences: SharedPreferences?) :
    BaseNetworkConnection(sharedPreferences) {
    private var mSocketFactory: SSLSocketFactory? = null
    private var alreadyWarned = false

    @Throws(IOException::class)
    override fun makeUrlRequest(
        endpoint: MParticleBaseClientImpl.Endpoint?,
        connection: MPConnection?,
        payload: String?,
        identity: Boolean,
    ): MPConnection {
        try {
            // Gingerbread seems to dislike pinning w/ godaddy. Being that GB is near-dead anyway, just disable pinning for it.
            if (isPostGingerBread() && connection!!.isHttps && !shouldDisablePinning()) {
                try {
                    connection.sslSocketFactory = getSocketFactory()
                } catch (e: Exception) {
                    Logger.error("Error occurred while setting SSL socket : $e")
                }
            }

            if (payload != null) {
                val zos = getOutputStream(connection!!)
                try {
                    zos.write(payload.toByteArray())
                } finally {
                    zos.close()
                }
            }

            if (!identity) {
                val statusCode = connection!!.responseCode
                if (statusCode == 400 && !alreadyWarned) {
                    alreadyWarned = true
                    Logger.error("Bad API request - is the correct API key and secret configured?")
                }
                if ((statusCode == 503 || statusCode == HTTP_TOO_MANY_REQUESTS) && !BuildConfig.MP_DEBUG) {
                    setNextAllowedRequestTime(connection, endpoint)
                }
            }
        } catch (ex: IOException) {
            throw ex
        }
        return connection!!
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected open fun isPostGingerBread(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH

    @Throws(IOException::class)
    protected open fun getOutputStream(connection: MPConnection?): OutputStream = GZIPOutputStream(BufferedOutputStream(connection!!.outputStream))

    /**
     * Custom socket factory used for certificate pinning.
     */
    @Throws(Exception::class)
    protected open fun getSocketFactory(): SSLSocketFactory {
        if (mSocketFactory == null) {
            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType)
            keyStore.load(null, null)

            val cf = CertificateFactory.getInstance("X.509")
            val networkOptions = mConfigManager!!.networkOptions
            val domainMappingList = networkOptions.getDomainMappings()
            val domainMappingSet = HashSet<com.mparticle.networking.Certificate>(NetworkOptionsManager.getDefaultCertificates())
            for (domainMapping in domainMappingList) {
                domainMappingSet.addAll(domainMapping.certificates)
            }
            for (certificate in domainMappingSet) {
                keyStore.setCertificateEntry(certificate.alias, generateCertificate(cf, certificate.certificate))
            }
            val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
            val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
            tmf.init(keyStore)

            val context = SSLContext.getInstance("TLS") // nosemgrep
            context.init(null, tmf.trustManagers, null)
            mSocketFactory = context.socketFactory
        }
        return mSocketFactory!!
    }

    private fun shouldDisablePinning(): Boolean {
        val networkOptions = mConfigManager!!.networkOptions
        return networkOptions.pinningDisabled ||
            (ConfigManager.getEnvironment() == MParticle.Environment.Development && networkOptions.pinningDisabledInDevelopment)
    }

    companion object {
        const val HTTP_TOO_MANY_REQUESTS: Int = 429

        /**
         * Default throttle time - in the worst case scenario if the server is busy, the soonest
         * the SDK will attempt to contact the server again will be after this 2 hour window.
         */
        internal const val DEFAULT_THROTTLE_MILLIS: Long = 1000L * 60 * 60 * 2
        internal const val MAX_THROTTLE_MILLIS: Long = 1000L * 60 * 60 * 24

        @Throws(IOException::class, CertificateException::class)
        private fun generateCertificate(certificateFactory: CertificateFactory, encodedCertificate: String): Certificate? {
            var certificate: Certificate? = null
            val inputStream = ByteArrayInputStream(encodedCertificate.toByteArray())
            try {
                certificate = certificateFactory.generateCertificate(inputStream)
            } catch (e: CertificateException) {
                Logger.error("There is an issue with the SSL certificate: " + e.message)
            } catch (e: Exception) {
                Logger.error("An error occurred while processing the SSL certificate: " + e.message)
            } finally {
                inputStream.close()
            }
            return certificate
        }
    }
}
