package com.mparticle.particlebox;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sdozor on 2/27/14.
 */
public class NetworkPerformanceFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private EditText url1;
    private CheckBox postCheckBox;

    public NetworkPerformanceFragment() {
        super();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_networkperformance, container, false);
        url1 = (EditText) v.findViewById(R.id.url1);
        postCheckBox = (CheckBox) v.findViewById(R.id.postCheckBox);
        v.findViewById(R.id.button1).setOnClickListener(this);
        v.findViewById(R.id.button2).setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button1:
                (new AsyncTask<String, Void, Void>() {
                    @Override
                    protected Void doInBackground(String... params) {
                        try {
                            boolean post = Boolean.parseBoolean(params[1]);
                            if (post) {
                                HttpPost httpPost = new HttpPost(params[0]);
                                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                                for (int i = 0; i < 100; i++) {
                                    nameValuePairs.add(new BasicNameValuePair(i + "", "WHATEVER"));
                                }
                                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                                DefaultHttpClient client = new DefaultHttpClient();
                                HttpResponse response = client.execute(httpPost);
                                HttpEntity entity = response.getEntity();
                                String result = EntityUtils.toString(entity);
                            } else {
                                HttpGet httpGet = new HttpGet(params[0]);
                                DefaultHttpClient client = new DefaultHttpClient();
                                HttpResponse response = client.execute(httpGet);
                                HttpEntity entity = response.getEntity();
                                String result = EntityUtils.toString(entity);
                            }

                        } catch (Exception e) {
                            String test = "";
                        }
                        return null;
                    }
                }).execute(url1.getText().toString(), Boolean.toString(postCheckBox.isChecked()));
                break;
            case R.id.button2:
                (new AsyncTask<String, Void, Void>() {
                    private void readStream(InputStream in) throws IOException {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        String result, line = reader.readLine();
                        result = line;
                        while ((line = reader.readLine()) != null) {
                            result += line;
                        }
                        String test = "";
                    }

                    @Override
                    protected Void doInBackground(String... params) {
                        try {
                            boolean post = Boolean.parseBoolean(params[1]);


                            URL url = new URL(params[0]);

                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            if (post){
                                urlConnection.setDoOutput(true);
                                //urlConnection.setChunkedStreamingMode(0);
                                DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
                                //for (int i = 0; i < 100; i++) {
                                    out.writeBytes("whatever");
                                //}
                                out.flush();
                                out.close();
                            }
                            int responseCode = urlConnection.getResponseCode();

                                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                readStream(in);

                                urlConnection.disconnect();



                        } catch (Exception e) {
                            String test = "";
                        }
                        return null;
                    }
                }).execute(url1.getText().toString(), Boolean.toString(postCheckBox.isChecked()));

        }
    }
}
