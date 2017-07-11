package com.mparticle.identity;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.MParticleBaseClientImpl;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IdentityApiTest {
    static Context mContext;
    protected CountDownLatch lock = new CountDownLatch(1);

    @BeforeClass
    public static void setup() {
        mContext = InstrumentationRegistry.getContext();
        Looper.prepare();
    }

    @Test
    public void testIdentify() throws Exception {
        setListener(new MParticleBaseClientImpl.BaseNetworkListener() {
            @Override
            protected void networkSend(URL url, Map<String, List<String>> headers, String payload) {
                try {
                    payload = new JSONObject(payload).toString(4);
                } catch (JSONException jse) {
                    // do nothing
                }
                Log.i("Request", url + "\n" + payload);
                for (Map.Entry<String, List<String>> entry: headers.entrySet()) {
                    for (String header: entry.getValue()) {
                        Log.i("header", entry.getKey() + ": " + header);
                    }
                }
            }

            @Override
            protected void networkReceive(int responseCode, String payload) {
                Log.i("Response",responseCode +": " + payload);
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        });
        IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                .email("test@mparticle.com")
                .userIdentity(MParticle.IdentityType.Facebook, "MParticle Facebook")
                .userIdentity(MParticle.IdentityType.Microsoft, "Moot")
                .build();
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .userIdentities(request)
                .build();
        MParticle.start(options);
        lock.await(5, TimeUnit.SECONDS);
    }

    private void setListener(MParticleBaseClientImpl.BaseNetworkListener listener) {
        MParticleIdentityClientImpl.setListener(listener);
    }
}
