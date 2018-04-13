package com.mparticle.integration_tests;

import android.os.Handler;
import android.os.Looper;

import com.mparticle.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleIdentityClient;
import com.mparticle.internal.MParticleApiClient;
import com.mparticle.internal.MParticleApiClientImpl;
import com.mparticle.internal.networking.MParticleBaseClient;
import com.mparticle.internal.networking.BaseNetworkConnection;
import com.mparticle.utils.MParticleUtils;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class PinningTest extends BaseCleanStartedEachTest {

    boolean called;

    @After
    public void after() {
        for (int i = 0; i < 10; i++) {
            if (called) {
                break;
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!called) {
            fail("not called");
        }
    }

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {
        called = false;
    }

    @Test
    public void testIdentityClientLogin() throws Exception {
        prepareIdentityApiClient("/login", true);
        MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser().build());
    }

    @Test
    public void testIdentityClientLogout() throws Exception {
        prepareIdentityApiClient("/logout", true);
        MParticle.getInstance().Identity().logout(IdentityApiRequest.withEmptyUser().build());
    }

    @Test
    public void testIdentityClientIdentify() throws Exception {
        prepareIdentityApiClient("/identify", true);
        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
    }

    @Test
    public void testIdentityClientModify() throws Exception {
        prepareIdentityApiClient("/modify", true);
        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().build());
    }

    @Test
    public void testMParticleClientFetchConfig() throws Exception {
        prepareMParticleApiClient("/config", true);
        try {
            com.mparticle.internal.AccessUtils.getApiClient().fetchConfig(true);
        }
        catch (MParticleApiClientImpl.MPConfigException ignored) {}
        catch (UnknownHostException ignored) {}
        catch (Exception e) {}
    }

    @Test
    public void testMParticleClientSendMessage() throws Exception {
        prepareMParticleApiClient("/events", true);
        try {
            com.mparticle.internal.AccessUtils.getApiClient().sendMessageBatch(new JSONObject().toString());
        }
        catch (Exception e) {}
    }

    @Test
    public void testMParticleClientFetchAudience() throws Exception {
        prepareMParticleApiClient("/audience", true);
        com.mparticle.internal.AccessUtils.getApiClient().fetchAudiences();
    }


    private void prepareIdentityApiClient(String path, final boolean shouldPin) {
        MParticleUtils.getInstance().setDefaultIdentityClient(mContext);
        com.mparticle.identity.AccessUtils.setIdentityApiClientScheme("https");
        MParticleIdentityClient apiClient = com.mparticle.identity.AccessUtils.getIdentityApiClient();
        setRequestClient(apiClient, path, shouldPin);
    }

    private void prepareMParticleApiClient(String path, final boolean shouldPin) {
        MParticleUtils.getInstance().setDefaultClient(mContext);
        com.mparticle.internal.AccessUtils.setMParticleApiClientProtocol("https");
        MParticleApiClient apiClient = com.mparticle.internal.AccessUtils.getApiClient();
        setRequestClient(apiClient, path, shouldPin);
    }

    private void setRequestClient(MParticleBaseClient client, final String path, final boolean shouldPin) {
        final BaseNetworkConnection requestHandler = client.getRequestHandler();
        client.setRequestHandler(new BaseNetworkConnection(mContext) {
            @Override
            public HttpURLConnection makeUrlRequest(HttpURLConnection connection, String payload, boolean identity) throws IOException {
                try {
                    connection = requestHandler.makeUrlRequest(connection, null, identity);
                }
                finally {
                    if (connection.getURL().toString().contains(path)) {
                        called = true;
                        final HttpURLConnection finalConnection = connection;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (finalConnection instanceof HttpsURLConnection) {
                                    HttpsURLConnection sslConnection = (HttpsURLConnection) finalConnection;
                                    assertEquals(shouldPin, isPinned(sslConnection));
                                } else {
                                    assertFalse(shouldPin);
                                }
                            }
                        });
                    }
                }
                return connection;
            }
        });
    }

    /**
     * The only way I have been able to find if we are pinning certificates or not, is to test
     * whether the HttpsURLConnection is using it's origin SSLSocketFactory or not. This does not
     * tell us if it actually has certificates pinned, but rather, if it has ever been set. Not the
     * best approach, but there is not easier way, without doing some Reflection, which we should
     * eventually do
     */
    private boolean isPinned(HttpsURLConnection connection) {
        return connection.getSSLSocketFactory() != SSLSocketFactory.getDefault();
    }
}
