package com.mparticle.networking;

import android.content.SharedPreferences;
import android.os.Build;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class NetworkConnection extends BaseNetworkConnection {
    public static final int HTTP_TOO_MANY_REQUESTS = 429;

    private ConfigManager mConfigManager;
    private SSLSocketFactory mSocketFactory;
    private boolean alreadyWarned;

    /**
     * Default throttle time - in the worst case scenario if the server is busy, the soonest
     * the SDK will attempt to contact the server again will be after this 2 hour window.
     */
    static final long DEFAULT_THROTTLE_MILLIS = 1000 * 60 * 60 * 2;
    static final long MAX_THROTTLE_MILLIS = 1000 * 60 * 60 * 24;

    NetworkConnection(ConfigManager configManager, SharedPreferences sharedPreferences) {
        super(sharedPreferences);
        this.mConfigManager = configManager;
    }

    @Override
    public MPConnection makeUrlRequest(MParticleBaseClientImpl.Endpoint endpoint, MPConnection connection, String payload, boolean identity) throws IOException {
        try {

            //Gingerbread seems to dislike pinning w/ godaddy. Being that GB is near-dead anyway, just disable pinning for it.
            if (isPostGingerBread() && connection.isHttps() && !shouldDisablePinning()) {
                try {
                    connection.setSSLSocketFactory(getSocketFactory());
                } catch (Exception e) {
                    Logger.error("Error occurred while setting SSL socket : " + e);
                }
            }

            if (payload != null) {
                OutputStream zos = getOutputStream(connection);
                try {
                    zos.write(payload.getBytes());
                } finally {
                    zos.close();
                }
            }

            if (!identity) {
                int statusCode = connection.getResponseCode();
                if (statusCode == 400 && !alreadyWarned) {
                    alreadyWarned = true;
                    Logger.error("Bad API request - is the correct API key and secret configured?");
                }
                if ((statusCode == 503 || statusCode == HTTP_TOO_MANY_REQUESTS) && !BuildConfig.MP_DEBUG) {
                    setNextAllowedRequestTime(connection, endpoint);
                }
            }
        } catch (IOException ex) {
            throw ex;
        }
        return connection;
    }

    boolean isPostGingerBread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    protected OutputStream getOutputStream(MPConnection connection) throws IOException {
        return new GZIPOutputStream(new BufferedOutputStream(connection.getOutputStream()));
    }

    /**
     * Custom socket factory used for certificate pinning.
     */
    protected SSLSocketFactory getSocketFactory() throws Exception {
        if (mSocketFactory == null) {
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            NetworkOptions networkOptions = mConfigManager.getNetworkOptions();
            List<DomainMapping> domainMappingList = networkOptions.getDomainMappings();
            Set<com.mparticle.networking.Certificate> domainMappingSet = new HashSet<>(NetworkOptionsManager.getDefaultCertificates());
            for (DomainMapping domainMapping : domainMappingList) {
                domainMappingSet.addAll(domainMapping.getCertificates());
            }
            for (com.mparticle.networking.Certificate certificate : domainMappingSet) {
                keyStore.setCertificateEntry(certificate.getAlias(), generateCertificate(cf, certificate.getCertificate()));
            }
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext context = SSLContext.getInstance("TLS");  // nosemgrep
            context.init(null, tmf.getTrustManagers(), null);
            mSocketFactory = context.getSocketFactory();

        }
        return mSocketFactory;
    }


    private static Certificate generateCertificate(CertificateFactory certificateFactory, String encodedCertificate) throws IOException, CertificateException {
        Certificate certificate = null;
        InputStream inputStream = new ByteArrayInputStream(encodedCertificate.getBytes());
        try {
            certificate = certificateFactory.generateCertificate(inputStream);
        } catch (CertificateException e) {
            Logger.error("There is an issue with the SSL certificate: " + e.getMessage());
        } catch (Exception e) {
            Logger.error("An error occurred while processing the SSL certificate: " + e.getMessage());
        } finally {
            inputStream.close();
        }
        return certificate;
    }

    private boolean shouldDisablePinning() {
        NetworkOptions networkOptions = mConfigManager.getNetworkOptions();
        return networkOptions.pinningDisabled || (ConfigManager.getEnvironment() == MParticle.Environment.Development && networkOptions.pinningDisabledInDevelopment);
    }


}
