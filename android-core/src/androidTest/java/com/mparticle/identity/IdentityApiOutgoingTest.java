package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.junit.Test;

import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public final class IdentityApiOutgoingTest extends BaseCleanStartedEachTest {

    @Test
    public void testLogin() throws Exception {
        MParticle.getInstance().Identity().login();
        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/login")).withRequestBody(matchingJsonPath(String.format("$.[?(@.previous_mpid == '%s')]", String.valueOf(mStartingMpid)))));
    }

    @Test
    public void testLoginNonEmpty() throws Exception {
        MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/login")).withRequestBody(matchingJsonPath(String.format("$.[?(@.previous_mpid == '%s')]", String.valueOf(mStartingMpid)))));
    }

    @Test
    public void testLogout() throws Exception {
        MParticle.getInstance().Identity().logout();
        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/logout")).withRequestBody(matchingJsonPath(String.format("$.[?(@.previous_mpid == '%s')]", String.valueOf(mStartingMpid)))));
    }

    @Test
    public void testLogoutNonEmpty() throws Exception {
        MParticle.getInstance().Identity().logout(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/logout")).withRequestBody(matchingJsonPath(String.format("$.[?(@.previous_mpid == '%s')]", String.valueOf(mStartingMpid)))));
    }

    @Test
    public void testModify() throws Exception {
        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().customerId(ran.nextLong() + "").build());
        mServer.waitForVerify(postRequestedFor(urlPathMatching(String.format("/v([0-9]*)/%s/modify", mStartingMpid))));
    }

    @Test
    public void testIdentify() throws Exception {
        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/identify")).withRequestBody(matchingJsonPath(String.format("$.[?(@.previous_mpid == '%s')]", String.valueOf(mStartingMpid)))));
    }
}
