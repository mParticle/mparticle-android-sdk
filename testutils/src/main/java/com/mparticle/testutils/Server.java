package com.mparticle.testutils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.mparticle.MParticle;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static junit.framework.Assert.fail;

public class Server {
    private WireMockServer mWireMockServer;
    StubMapping mConfigMapping;
    List<RequestListener> requestListeners = new ArrayList<>();

    public Server() {
        mWireMockServer = new WireMockServer(wireMockConfig().port(8080).notifier(new Notifier() {
            @Override
            public void info(String s) {
                Log.i("WIREMOCK", s);
            }

            @Override
            public void error(String s) {
                Log.e("WIREMOCK", s);
            }

            @Override
            public void error(String s, Throwable throwable) {
                Log.e("WIREMOCK", s, throwable);
            }
        }));
        mWireMockServer.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                //need to copy the Request to a logged request, since Request has some weird, mutable properties
                //which seem to change unexpectedly when you hang onto the reference
                for (RequestListener requestListener : requestListeners) {
                    if (requestListener != null) {
                        requestListener.requestReceived(request, response);
                    }
                }
            }
        });
        mWireMockServer.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                request.getUrl();
            }
        });
        reset();
    }

    public void reset() {
        if (MParticle.getInstance() != null) {
            reset(MParticle.getInstance().Internal().getConfigManager().getMpid());
        } else {
            reset(0);
        }
    }

    public void reset(long currentMpid) {
        mWireMockServer.resetAll();
        setUpHappyConfig();
        setupHappyEvents();
        setupHappyIdentify(currentMpid);
        setupHappyLogin(currentMpid);
        setupHappyLogout(currentMpid);
        setupHappyModify();
        mWireMockServer.start();
        requestListeners = new ArrayList<>();
    }

    public void stop() {
        mWireMockServer.stop();
    }

    public WireMockServer getServer() {
        return mWireMockServer;
    }

    public Server setUpHappyConfig() {
        return setupConfigResponse("{response:\"hello\"}");
    }

    public Server setupConfigResponse(String response) {
        return setupConfigResponse(response, 0);
    }


    public Server setupConfigResponse(String response, int delay) {
        if (mConfigMapping != null) {
            mWireMockServer.removeStub(mConfigMapping);
        }
        mConfigMapping = mWireMockServer.stubFor(get(urlPathMatching("/v([0-9]*)/([0-9a-zA-Z]*)/config"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(response)));
        return this;
    }

    public Server setupHappyEvents() {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/([0-9a-zA-Z]*)/events"))
                .willReturn(aResponse()
                        .withStatus(202)));
        return this;
    }

    public Server setupHappyIdentify() {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/identify"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Access-Control-Allow-Origin", "*")
                ));
        return this;
    }

    public Server setupHappyIdentify(long mpid) {
        return setupHappyIdentify(mpid, 0);
    }

    public Server setupHappyIdentify(long mpid, int delay) {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/identify"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(delay)
                        .withHeader("Access-Control-Allow-Origin", "*")
                        .withBody(getIdentityResponse(mpid))
                ));
        return this;
    }

    public Server setupHappyLogin(long mpid) {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getIdentityResponse(mpid))
                        .withHeader("Access-Control-Allow-Origin", "*")
                ));
        return this;
    }

    public Server setupHappyLogout(long mpid) {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/logout"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getIdentityResponse(mpid))
                        .withHeader("Access-Control-Allow-Origin", "*")
                ));
        return this;
    }

    public Server setupHappyModify() {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/([0-9]*)/modify"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Access-Control-Allow-Origin", "*")
                ));
        return this;
    }

    public Server addConditionalIdentityResponse(long ifMpid, long thenMpid) {
        return addConditionalIdentityResponse(ifMpid, thenMpid, 0);
    }

    public Server addConditionalIdentityResponse(long ifMpid, long thenMpid, int delay) {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/identify")).withRequestBody(matchingJsonPath(String.format("$.[?(@.previous_mpid == '%s')]", String.valueOf(ifMpid))))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(delay)
                        .withBody(getIdentityResponse(thenMpid))));
        return this;
    }

    public Server addConditionalLoginResponse(long ifMpid, long thenMpid) {
        return addConditionalLoginResponse(ifMpid, thenMpid, 0);
    }

    public Server addConditionalLoginResponse(long ifMpid, long thenMpid, int delay) {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/login")).withRequestBody(matchingJsonPath(String.format("$.[?(@.previous_mpid == '%s')]", String.valueOf(ifMpid))))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(delay)
                        .withBody(getIdentityResponse(thenMpid))));
        return this;
    }

    public Server addConditionalLogoutResponse(long ifMpid, long thenMpid) {
        return addConditionalLogoutResponse(ifMpid, thenMpid, 0);
    }

    public Server addConditionalLogoutResponse(long ifMpid, long thenMpid, int delay) {
        mWireMockServer.stubFor(post(urlPathMatching("/v([0-9]*)/logout")).withRequestBody(matchingJsonPath(String.format("$.[?(@.previous_mpid == '%s')]", String.valueOf(ifMpid))))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(delay)
                        .withBody(getIdentityResponse(thenMpid))));
        return this;
    }

    private String getIdentityResponse(long mpid) {
        try {
            return new JSONObject().put("mpid", String.valueOf(mpid))
                    .put("context", "randomContext")
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * an implementation of WireMockServer.verify(), except this method will wait until either the call
     * is verified, or the timeout is reached
     */
    public void waitForVerify(RequestPatternBuilder requestPattern) {
        waitForVerify(requestPattern, null);
    }

    public void waitForVerify(final RequestPatternBuilder requestPattern, final RequestReceivedCallback callback) {
        final CountDownLatch latch = new MPLatch(1);
        final Handler handler = new Handler(Looper.getMainLooper());
        requestListeners.add(new RequestListener() {
            @Override
            public void requestReceived(final Request request, Response response) {
                final Request loggedRequest = LoggedRequest.createFrom(request);
                if (requestPattern.build().match(loggedRequest).isExactMatch()) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            latch.countDown();
                            if (callback != null) {
                                callback.onRequestReceived(loggedRequest);
                            }
                        }
                    });
                }
            }
        });
        for (final ServeEvent serveEvent : mWireMockServer.getAllServeEvents()) {
            if (requestPattern.build().match(serveEvent.getRequest()).isExactMatch()) {
                latch.countDown();
                if (callback != null) {
                    callback.onRequestReceived(serveEvent.getRequest());
                }
            }
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            fail(e.toString());
        }
    }

    public void waitForVerify(UrlPathPattern requestPattern, JSONMatch jsonMatch) throws InterruptedException {
        waitForVerify(requestPattern, jsonMatch, null);
    }

    public void waitForVerify(final UrlPathPattern pattern, final JSONMatch jsonMatch, final RequestReceivedCallback callback) throws InterruptedException {
        final CountDownLatch latch = new MPLatch(1);
        final Handler handler = new Handler(Looper.getMainLooper());
        requestListeners.add(new RequestListener() {
            @Override
            public void requestReceived(final Request request, Response response) {
                final LoggedRequest loggedRequest = LoggedRequest.createFrom(request);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isMatch(loggedRequest, pattern, jsonMatch)) {
                            if (loggedRequest != null) {
                                latch.countDown();
                                if (callback != null) {
                                    callback.onRequestReceived(loggedRequest);
                                }
                            }
                        }
                    }
                });
            }
        });
        Request request = verify(pattern, jsonMatch, false);
        if (request != null) {
            if (callback != null) {
                callback.onRequestReceived(request);
            }
            latch.countDown();
            return;
        }
        latch.await();
    }

    private Request verify(UrlPathPattern pattern, JSONMatch jsonMatch, boolean throwException) {
        List<ServeEvent> misses = new ArrayList<ServeEvent>();
        for (ServeEvent event : mWireMockServer.getAllServeEvents()) {
            if (isMatch(event.getRequest(), pattern, jsonMatch)) {
                return event.getRequest();
            }
        }
        if (throwException) {
            StringBuilder builder = new StringBuilder();
            for (ServeEvent event : misses) {
                builder.append(event.getRequest().getBodyAsString() + ",\n");
            }
            fail("No matching Requests found. Matching URLs found that did not match JSON: " + misses.size() + ": " + builder.toString());
        }
        return null;
    }


    //IMPORTANT: this only returns FINISHED requests. If you call this just after a "waitForVerify()" call
    //finishes, the request you were waiting for will not reliably be included in the ReceivedRequests, since
    //the call has likely been received, but not finish. 
    public ReceivedRequests getRequests() {
        return new ReceivedRequests(mWireMockServer.getAllServeEvents());
    }

    private boolean isMatch(Request request, UrlPathPattern pattern, JSONMatch jsonMatch) {
        if (pattern.match(request.getUrl()).getDistance() < 1) {
            try {
                return jsonMatch.isMatch(new JSONObject(request.getBodyAsString()));
            } catch (JSONException ignore) {

            }
        }
        return false;
    }

    public interface JSONMatch {
        boolean isMatch(JSONObject jsonObject);
    }

    public interface RequestReceivedCallback {
        void onRequestReceived(Request request);
    }

}
