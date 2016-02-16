package com.mparticle.kits.mobileapptracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.mparticle.kits.TuneKit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;

public class MATUtils {

    /**
     * Reads an InputStream and converts it to a String
     * @param stream InputStream to read
     * @return String of stream contents
     * @throws IOException Reader was closed when trying to be read
     * @throws UnsupportedEncodingException UTF-8 encoding could not be found
     */
    public static String readStream(InputStream stream) throws IOException, UnsupportedEncodingException {
        if (stream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (String line = null; (line = reader.readLine()) != null;) {
                builder.append(line).append("\n");
            }
            reader.close();
            return builder.toString();
        }
        return "";
    }

    /**
     * Determine the device's user agent and set the corresponding field.
     */
    public static void calculateUserAgent(Context context, TuneKit tuneKit) {
        String userAgent = System.getProperty("http.agent", "");
        if (!TextUtils.isEmpty(userAgent)) {
            tuneKit.setUserAgent(userAgent);
        } else {
            // If system doesn't have user agent,
            // execute Runnable on UI thread to get WebView user agent
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new GetWebViewUserAgent(context, tuneKit));
        }
    }

    /**
     *  Runnable for getting the WebView user agent
     */
    @SuppressLint("NewApi")
    private static class GetWebViewUserAgent implements Runnable {
        private final WeakReference<Context> weakContext;
        private final TuneKit tuneKit;

        public GetWebViewUserAgent(Context context, TuneKit tuneKit) {
            weakContext = new WeakReference<Context>(context);
            this.tuneKit = tuneKit;
        }

        public void run() {
            try {
                Class.forName("android.os.AsyncTask"); // prevents WebView from crashing on certain devices
                if (Build.VERSION.SDK_INT >= 17) {
                    tuneKit.setUserAgent(WebSettings.getDefaultUserAgent(weakContext.get()));
                } else {
                    // Create WebView to set user agent, then destroy WebView
                    WebView wv = new WebView(weakContext.get());
                    tuneKit.setUserAgent(wv.getSettings().getUserAgentString());
                    wv.destroy();
                }
            } catch (Exception e) {
                // Alcatel has WebView implementation that causes getDefaultUserAgent to NPE
                // Reference: https://groups.google.com/forum/#!topic/google-admob-ads-sdk/SX9yb3F_PNk
            } catch (VerifyError e) {
                // Some device vendors have their own WebView implementation which crashes on our init
            }
        }
    }

    public static boolean firstInstall(Context context) {
        // If SharedPreferences value for install exists, set firstInstall false
        SharedPreferences installed = context.getSharedPreferences(MATConstants.PREFS_TUNE, Context.MODE_PRIVATE);
        if (installed.contains(MATConstants.KEY_INSTALL)) {
            return false;
        } else {
            // Set install value in SharedPreferences if not there
            installed.edit().putBoolean(MATConstants.KEY_INSTALL, true).apply();
            return true;
        }
    }
}
