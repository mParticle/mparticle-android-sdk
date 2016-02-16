package com.mparticle.kits.mobileapptracker;

import android.net.Uri;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MATUrlRequester {
    public void requestDeeplink(MATDeferredDplinkr dplinkr) {
        String deeplink = "";
        InputStream is = null;
        
        // Construct deeplink endpoint url
        Uri.Builder uri = new Uri.Builder();
        uri.scheme("https")
           .authority(dplinkr.getAdvertiserId() + "." + MATConstants.DEEPLINK_DOMAIN)
           .appendPath("v1")
           .appendPath("link.txt")
           .appendQueryParameter("platform", "android")
           .appendQueryParameter("advertiser_id", dplinkr.getAdvertiserId())
           .appendQueryParameter("ver", MATConstants.SDK_VERSION)
           .appendQueryParameter("package_name", dplinkr.getPackageName())
           .appendQueryParameter("ad_id", ((dplinkr.getGoogleAdvertisingId() != null) ? dplinkr.getGoogleAdvertisingId() : dplinkr.getAndroidId()))
           .appendQueryParameter("user_agent", dplinkr.getUserAgent());
        
        if (dplinkr.getGoogleAdvertisingId() != null) {
            uri.appendQueryParameter("google_ad_tracking_disabled", Integer.toString(dplinkr.getGoogleAdTrackingLimited()));
        }
        
        try {
            URL myurl = new URL(uri.build().toString());
            HttpURLConnection conn = (HttpURLConnection) myurl.openConnection();
            // Set TUNE conversion key in request header
            conn.setRequestProperty("X-MAT-Key", dplinkr.getConversionKey());
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            
            conn.connect();
            
            boolean error = false;
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
            } else {
                error = true;
                is = conn.getErrorStream();
            }
            
            deeplink = MATUtils.readStream(is);
            MATDeeplinkListener listener = dplinkr.getListener();
            if (listener != null) {
                if (error) {
                    // Notify listener of error
                    listener.didFailDeeplink(deeplink);
                } else {
                    // Notify listener of deeplink url
                    listener.didReceiveDeeplink(deeplink);
                }
            }
        } catch (Exception e) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, e, "Error while querying Tune web service for deep links.");
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, e, "Error while querying Tune web service for deep links.");
            }
        }
    }
}
