package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.identity.BaseIdentityTask
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateAdIdIdentityTest : BaseCleanInstallEachTest() {

    @Test
    fun testAdIdModifyNoUser() {
        // setup mock server so initial identity request will not set mpid
        mServer.setupHappyIdentify(0)
        val latch = MPLatch(1)
        MParticle.start(
            MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .identifyTask(BaseIdentityTask().addSuccessListener { latch.countDown() })
                .build()
        )

        // execute CheckAdIdRunnable without a current user
        MParticle.getInstance()!!.Internal().configManager
        AppStateManager.CheckAdIdRunnable(MParticle.getInstance()!!.Internal().configManager).run()
        assertNull(MParticle.getInstance()!!.Identity().currentUser)

        // set a current user
        mServer.addConditionalIdentityResponse(0, mStartingMpid)
        latch.await()

        // force a modify request to ensure that the modify request from the CheckAdIdRunnable is completed
        val latch2 = MPLatch(1)
        MParticle.getInstance()!!.Identity()
            .modify(IdentityApiRequest.withEmptyUser().customerId("someId").build())
            .addSuccessListener { latch2.countDown() }
        latch2.await()

        // check that modify request from CheckAdIdRunnable executed when current user was set
        mServer.Requests().modify.count { request ->
            request.asIdentityRequest().body.identity_changes.let {
                it.size == 1 &&
                    it[0].let { identityChange ->
                        identityChange["new_value"] == "someId"
                    }
            }
        }.let {
            assertEquals(1, it)
        }
    }
}
