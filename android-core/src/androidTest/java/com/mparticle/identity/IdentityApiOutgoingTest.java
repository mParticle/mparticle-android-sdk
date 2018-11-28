package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.networking.IdentityRequest;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.networking.MockServer.IdentityMatcher;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.junit.Test;

public final class IdentityApiOutgoingTest extends BaseCleanStartedEachTest {

    @Test
    public void testLogin() throws Exception {
        MParticle.getInstance().Identity().login();
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getLoginUrl()).bodyMatch(new IdentityMatcher() {
            @Override
            protected boolean isIdentityMatch(IdentityRequest.IdentityRequestBody identityRequest) {
                return mStartingMpid.equals(identityRequest.previousMpid);
            }
        }));
    }

    @Test
    public void testLoginNonEmpty() throws Exception {
        MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getLoginUrl()).bodyMatch(new IdentityMatcher() {
            @Override
            protected boolean isIdentityMatch(IdentityRequest.IdentityRequestBody identityRequest) {
                return mStartingMpid.equals(identityRequest.previousMpid);
            }
        }));
    }

    @Test
    public void testLogout() throws Exception {
        MParticle.getInstance().Identity().logout();
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getLogoutUrl()).bodyMatch(new IdentityMatcher() {
            @Override
            protected boolean isIdentityMatch(IdentityRequest.IdentityRequestBody identityRequest) {
                return mStartingMpid.equals(identityRequest.previousMpid);
            }
        }));
    }

    @Test
    public void testLogoutNonEmpty() throws Exception {
        MParticle.getInstance().Identity().logout(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getLogoutUrl()).bodyMatch(new IdentityMatcher() {
            @Override
            protected boolean isIdentityMatch(IdentityRequest.IdentityRequestBody identityRequest) {
                return mStartingMpid.equals(identityRequest.previousMpid);
            }
        }));
    }

    @Test
    public void testModify() throws Exception {
        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().customerId(ran.nextLong() + "").build());
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)));
    }

    @Test
    public void testIdentify() throws Exception {
        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getIdentifyUrl()).bodyMatch(new IdentityMatcher() {
            @Override
            protected boolean isIdentityMatch(IdentityRequest.IdentityRequestBody identityRequest) {
                return mStartingMpid.equals(identityRequest.previousMpid);
            }
        }));
    }
}
