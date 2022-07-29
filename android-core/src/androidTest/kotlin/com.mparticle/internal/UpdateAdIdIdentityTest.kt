package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.identity.BaseIdentityTask
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.messages.IdentityResponseMessage
import com.mparticle.testing.BaseTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateAdIdIdentityTest : BaseTest() {

    @Test
    fun testAdIdModifyNoUser() {
        // setup mock server so initial identity request will not set mpid
        Server
            .endpoint(EndpointType.Identity_Identify)
            .addResponseLogic {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(1)
                }
            }
        val latch = FailureLatch()
        MParticle.start(
            MParticleOptions.builder(context)
                .credentials("key", "secret")
                .identifyTask(BaseIdentityTask().addSuccessListener { latch.countDown() })
                .build()
        )

        // execute CheckAdIdRunnable without a current user
        AppStateManager.CheckAdIdRunnable("newAdId", "oldAdId").run()
        assertNull(MParticle.getInstance()!!.Identity().currentUser)

        // set a current user
        Server
            .endpoint(EndpointType.Identity_Identify)
            .addResponseLogic({ it.body.previousMpid == 0L }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(mStartingMpid)
                }
            }

        latch.await()

        // force a modify request to ensure that the modify request from the CheckAdIdRunnable is completed
        val latch2 = FailureLatch()
        MParticle.getInstance()!!.Identity().modify(IdentityApiRequest.withEmptyUser().customerId("someId").build())
            .addSuccessListener { latch2.countDown() }
        latch2.await()

        // check that modify request from CheckAdIdRunnable executed when current user was set
        Server
            .endpoint(EndpointType.Identity_Modify)
            .requests
            .count {

                it.request.body.identityChanges.let { identityChanges ->
                    identityChanges?.size == 1 &&
                        identityChanges[0].let { identityChange ->
                            identityChange.newValue == "newAdId" &&
                                identityChange.oldValue == "oldAdId"
                        }
                }
            }.let {
                assertEquals(1, it)
            }
    }
}
