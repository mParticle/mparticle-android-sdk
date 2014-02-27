package com.mparticle.particlebox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.mparticle.MParticle;
import com.mparticle.MParticleJSInterface;

/**
 * Created by sdozor on 2/25/14.
 */
public class WebAppFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_webapp, container, false);
        WebView wv = (WebView) v.findViewById(R.id.webview);
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wv.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("PB web app", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });
        wv.addJavascriptInterface(new MParticleJSInterface(v.getContext(), MParticle.getInstance()), "mParticleAndroid");
        wv.loadUrl("file:///android_asset/pbwebapp.html");

        return v;
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static WebAppFragment newInstance(int sectionNumber) {
        WebAppFragment fragment = new WebAppFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }
}
