package com.mparticle.testutils;

import android.util.Log;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.mparticle.MParticle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
        reset();
    }

    public void reset() {
        if (MParticle.getInstance() != null) {
            reset(MParticle.getInstance().getConfigManager().getMpid());
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
    public void waitForVerify(RequestPatternBuilder request, long millis) throws VerificationException {
        long endTime = System.currentTimeMillis() + millis;
        if (verify(request, millis <= 0)) {
            return;
        } else {
            while (endTime > System.currentTimeMillis()) {
                if (verify(request, false)) {
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e("Try","l");
            }
            verify(request, true);
        }
    }

    private boolean verify(RequestPatternBuilder request, boolean throwException) throws  VerificationException {
        try {
            mWireMockServer.verify(request);
            return true;
        }
        catch (VerificationException ex) {
            if (throwException) {
                throw ex;
            }
            return false;
        }
    }

    public void waitForVerify(UrlPathPattern pattern, JSONMatch jsonMatch, long millis) {
        long endTime = System.currentTimeMillis() + millis;
        if (verify(pattern, jsonMatch, millis == 0)) {
            return;
        } else {
            while (endTime > System.currentTimeMillis()) {
                if (verify(pattern, jsonMatch, false)) {
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e("Try","l");
            }
            verify(pattern, jsonMatch, true);
        }
    }

    private boolean verify(UrlPathPattern pattern, JSONMatch jsonMatch, boolean throwException) {
        List<ServeEvent> misses = new ArrayList<ServeEvent>();
        for (ServeEvent event: mWireMockServer.getAllServeEvents()) {
            if (pattern.match(event.getRequest().getUrl()).getDistance() < 1) {
                try {
                    if (jsonMatch.isMatch(new JSONObject(event.getRequest().getBodyAsString()))) {
                        return true;
                    } else if (throwException) {
                        misses.add(event);
                    }
                }
                catch (JSONException ignore) {

                }
            }
        }
        if (throwException) {
            StringBuilder builder = new StringBuilder();
            for(ServeEvent event: misses) {
                builder.append(event.getRequest().getBodyAsString() + ",\n");
            }
            fail("No matching Requests found. Matching URLs found that did not match JSON: " + misses.size() + ": " + builder.toString());
        }
        return false;
    }

    public ReceivedRequests getRequests() {
        return new ReceivedRequests(mWireMockServer.getAllServeEvents());
    }

    public interface JSONMatch {
        boolean isMatch(JSONObject jsonObject);
    }
}
