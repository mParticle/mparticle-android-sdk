package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.networking.IdentityRequest.IdentityRequestBody
import com.mparticle.networking.Matcher
import com.mparticle.networking.MockServer.IdentityMatcher
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Test

class IdentityApiOutgoingTest : BaseCleanStartedEachTest() {
    @Test
    @Throws(Exception::class)
    fun testLogin() {
        MParticle.getInstance()?.Identity()?.login()
        mServer.waitForVerify(Matcher(mServer.Endpoints().loginUrl).bodyMatch(object :
            IdentityMatcher() {
            override fun isIdentityMatch(identityRequest: IdentityRequestBody): Boolean {
                return mStartingMpid == identityRequest.previousMpid
            }
        }))
    }

    @Test
    @Throws(Exception::class)
    fun testLoginNonEmpty() {
        MParticle.getInstance()?.Identity()?.login(IdentityApiRequest.withEmptyUser().build())
        mServer.waitForVerify(Matcher(mServer.Endpoints().loginUrl).bodyMatch(object :
            IdentityMatcher() {
            override fun isIdentityMatch(identityRequest: IdentityRequestBody): Boolean {
                return mStartingMpid == identityRequest.previousMpid
            }
        }))
    }

    @Test
    @Throws(Exception::class)
    fun testLogout() {
        MParticle.getInstance()?.Identity()?.logout()
        mServer.waitForVerify(Matcher(mServer.Endpoints().logoutUrl).bodyMatch(object :
            IdentityMatcher() {
            override fun isIdentityMatch(identityRequest: IdentityRequestBody): Boolean {
                return mStartingMpid == identityRequest.previousMpid
            }
        }))
    }

    @Test
    @Throws(Exception::class)
    fun testLogoutNonEmpty() {
        MParticle.getInstance()?.Identity()?.logout(IdentityApiRequest.withEmptyUser().build())
        mServer.waitForVerify(Matcher(mServer.Endpoints().logoutUrl).bodyMatch(object :
            IdentityMatcher() {
            override fun isIdentityMatch(identityRequest: IdentityRequestBody): Boolean {
                return mStartingMpid == identityRequest.previousMpid
            }
        }))
    }

    @Test
    @Throws(Exception::class)
    fun testModify() {
        MParticle.getInstance()
            ?.Identity()?.modify(
                IdentityApiRequest.withEmptyUser().customerId(ran.nextLong().toString() + "")
                    .build()
            )
        mServer.waitForVerify(Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)))
    }

    @Test
    @Throws(Exception::class)
    fun testIdentify() {
        MParticle.getInstance()?.Identity()?.identify(IdentityApiRequest.withEmptyUser().build())
        mServer.waitForVerify(Matcher(mServer.Endpoints().identifyUrl).bodyMatch(object :
            IdentityMatcher() {
            override fun isIdentityMatch(identityRequest: IdentityRequestBody): Boolean {
                return mStartingMpid == identityRequest.previousMpid
            }
        }))
    }
}