package com.mparticle.internal;

import android.content.SharedPreferences;
import android.os.Build;

import com.mparticle.BuildConfig;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MParticleBaseClientImpl {
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    private SSLSocketFactory mSocketFactory;
    private boolean alreadyWarned;
    private SharedPreferences mPreferences;
    protected static NetworkListener mListener = BaseNetworkListener.Empty();

    /**
     * Default throttle time - in the worst case scenario if the server is busy, the soonest
     * the SDK will attempt to contact the server again will be after this 2 hour window.
     */
    static final long DEFAULT_THROTTLE_MILLIS = 1000*60*60*2;
    static final long MAX_THROTTLE_MILLIS = 1000*60*60*24;

    public MParticleBaseClientImpl(SharedPreferences sharedPreferences) {
        mPreferences = sharedPreferences;
    }

    protected HttpURLConnection makeUrlRequest(HttpURLConnection connection) throws IOException {
        return makeUrlRequest(connection, null);
    }

    protected HttpURLConnection makeUrlRequest(HttpURLConnection connection, String payload) throws IOException {
        return makeUrlRequest(connection, payload, true);
    }

    protected HttpURLConnection makeUrlRequest(HttpURLConnection connection, boolean identity) throws IOException {
        return makeUrlRequest(connection, null, identity);
    }

    protected HttpURLConnection makeUrlRequest(HttpURLConnection connection, String payload, boolean identity) throws IOException {
        try {
            mListener.onSend(connection, payload);

            if (payload != null) {
                String messageString = payload.toString();
                GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(connection.getOutputStream()));
                try {
                    zos.write(messageString.getBytes());
                } finally {
                    zos.close();
                }
            }

            //gingerbread seems to dislike pinning w/ godaddy. Being that GB is near-dead anyway, just disable pinning for it.
            if (!BuildConfig.MP_DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && connection instanceof HttpsURLConnection) {
                try {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(getSocketFactory());
                } catch (Exception e) {

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
            mListener.onReceive(connection);
        }
        catch (IOException ex) {
            mListener.onException(ex);
            throw ex;
        }
        return connection;
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

    void setNextAllowedRequestTime(HttpURLConnection connection) {
        long throttle = DEFAULT_THROTTLE_MILLIS;
        if (connection != null) {
            //most HttpUrlConnectionImpl's are case insensitive, but the interface
            //doesn't actually restrict it so let's be safe and check.
            String retryAfter = connection.getHeaderField("Retry-After");
            if (MPUtility.isEmpty(retryAfter)) {
                retryAfter = connection.getHeaderField("retry-after");
            }
            try {
                long parsedThrottle = Long.parseLong(retryAfter) * 1000;
                if (parsedThrottle > 0) {
                    throttle = Math.min(parsedThrottle, MAX_THROTTLE_MILLIS);
                }
            } catch (NumberFormatException nfe) {
                Logger.debug("Unable to parse retry-after header, using default.");
            }
        }

        long nextTime = System.currentTimeMillis() + throttle;
        setNextRequestTime(nextTime);
    }


    long getNextRequestTime() {
        return mPreferences.getLong(Constants.PrefKeys.NEXT_REQUEST_TIME, 0);
    }

    void setNextRequestTime(long timeMillis) {
        mPreferences.edit().putLong(Constants.PrefKeys.NEXT_REQUEST_TIME, timeMillis).apply();
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

    protected String getHeaderDateString() {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        return format.format(new Date());
    }

    protected String getHeaderHashString(HttpURLConnection request, String date, String message, String apiSecret) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String method = request.getRequestMethod();
        String path = request.getURL().getFile();
        StringBuilder hashString = new StringBuilder()
                .append(method)
                .append("\n")
                .append(date)
                .append("\n")
                .append(path);
        if (message != null) {
            hashString.append(message);
        }
        return MPUtility.hmacSha256Encode(apiSecret, hashString.toString());
    }

    public interface NetworkListener {
        void onSend(HttpURLConnection connection, String content);
        void onReceive(HttpURLConnection connection);
        void onException(Exception ex);
    }

    public abstract static class BaseNetworkListener implements NetworkListener {
        public void onSend(HttpURLConnection connection, String content) {
            networkSend(connection.getURL(), connection.getRequestProperties(), content);
        }

        public void onReceive(HttpURLConnection connection) {
            StringBuilder responseBuilder = new StringBuilder();
            int responseCode = -1;
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line + '\n');
                }
                in.close();
                responseCode = connection.getResponseCode();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            networkReceive(responseCode, responseBuilder.toString());
        }

        private static NetworkListener Empty() {
            return new NetworkListener() {
                @Override
                public void onSend(HttpURLConnection connection, String content) {
                    //do nothing
                }

                @Override
                public void onReceive(HttpURLConnection connection) {
                    //do nothing
                }

                @Override
                public void onException(Exception ex) {
                    //do nothing
                }
            };
        }

        protected abstract void networkSend(URL url, Map<String, List<String>> headers, String payload);
        protected abstract void networkReceive(int responseCode, String payload);
        public abstract void onException(Exception ex);
    }
}
