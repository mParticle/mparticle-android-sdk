package com.mparticle.particlebox;

import android.os.AsyncTask;
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
import android.widget.EditText;

import com.mparticle.MParticle;
import com.mparticle.MParticleJSInterface;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by sdozor on 2/27/14.
 */
public class NetworkPerformanceFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private EditText url1;

    public NetworkPerformanceFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_networkperformance, container, false);
        url1 = (EditText)v.findViewById(R.id.url1);
        v.findViewById(R.id.button1).setOnClickListener(this);
        v.findViewById(R.id.button2).setOnClickListener(this);
        return v;
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NetworkPerformanceFragment newInstance(int sectionNumber) {
        NetworkPerformanceFragment fragment = new NetworkPerformanceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button1:
                (new AsyncTask<String, Void, Void>() {
                    @Override
                    protected Void doInBackground(String... params) {
                        try{
                            HttpGet httpGet = new HttpGet(params[0]);
                            DefaultHttpClient client = new DefaultHttpClient();
                            HttpResponse response = client.execute(httpGet);
                            HttpEntity entity = response.getEntity();

                        }catch(Exception e){

                        }
                        return null;
                    }
                }).execute(url1.getText().toString());
                break;
            case R.id.button2:
                (new AsyncTask<String, Void, Void>() {
                    private void readStream(InputStream in) throws IOException {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        String result, line = reader.readLine();
                        result = line;
                        while((line=reader.readLine())!=null){
                            result+=line;
                        }
                        String test = "";
                    }
                    @Override
                    protected Void doInBackground(String... params) {
                        try{

                            if (params[0].startsWith("https")){
                                URL url = new URL(params[0]);
                                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                                try {
                                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                    readStream(in);
                                }finally {
                                        urlConnection.disconnect();
                                    }
                            }else{
                                URL url = new URL(params[0]);
                                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                try {
                                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                    readStream(in);
                                }finally {
                                    urlConnection.disconnect();
                                }
                            }


                        }catch(Exception e){

                        }
                        return null;
                    }
                }).execute(url1.getText().toString());

        }
    }
}
