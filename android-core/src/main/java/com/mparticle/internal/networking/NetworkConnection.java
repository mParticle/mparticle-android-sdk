package com.mparticle.internal.networking;

import android.content.SharedPreferences;
import android.os.Build;

import com.mparticle.BuildConfig;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class NetworkConnection extends BaseNetworkConnection {
    public static final int HTTP_TOO_MANY_REQUESTS = 429;

    private SSLSocketFactory mSocketFactory;
    private boolean alreadyWarned;

    /**
     * Default throttle time - in the worst case scenario if the server is busy, the soonest
     * the SDK will attempt to contact the server again will be after this 2 hour window.
     */
    static final long DEFAULT_THROTTLE_MILLIS = 1000*60*60*2;
    static final long MAX_THROTTLE_MILLIS = 1000*60*60*24;

    NetworkConnection(SharedPreferences sharedPreferences) {
        super(sharedPreferences);
    }

    @Override
    public HttpURLConnection makeUrlRequest(HttpURLConnection connection, String payload, boolean identity) throws IOException {
        try {

            //gingerbread seems to dislike pinning w/ godaddy. Being that GB is near-dead anyway, just disable pinning for it.
            if (!isDebug() && isPostGingerBread() && connection instanceof HttpsURLConnection) {
                try {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(getSocketFactory());
                } catch (Exception e) {

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
                    setNextAllowedRequestTime(connection);
                }
            }
        }
        catch (IOException ex) {
            throw ex;
        }
        return connection;
    }

    boolean isDebug() {
        return BuildConfig.MP_DEBUG;
    }

    boolean isPostGingerBread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    protected OutputStream getOutputStream(HttpURLConnection connection) throws IOException {
        return new GZIPOutputStream(new BufferedOutputStream(connection.getOutputStream()));
    }

    /**
     * Custom socket factory used for certificate pinning.
     */
    protected SSLSocketFactory getSocketFactory() throws Exception{
        if (mSocketFactory == null){
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            keyStore.setCertificateEntry("intca", generateCertificate(cf, Constants.GODADDY_INTERMEDIATE_CRT));
            keyStore.setCertificateEntry("rootca", generateCertificate(cf, Constants.GODADDY_ROOT_CRT));
            keyStore.setCertificateEntry("fiddlerroot", generateCertificate(cf, Constants.FIDDLER_ROOT_CRT));

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            mSocketFactory = context.getSocketFactory();
        }
        return mSocketFactory;
    }


    private static Certificate generateCertificate(CertificateFactory certificateFactory, String encodedCertificate) throws IOException, CertificateException {
        Certificate certificate = null;
        InputStream inputStream = new ByteArrayInputStream( encodedCertificate.getBytes() );
        try {
            certificate = certificateFactory.generateCertificate(inputStream);
        }finally {
            inputStream.close();
        }
        return certificate;
    }


}
