package com.mparticle

import com.mparticle.internal.Constants
import com.mparticle.internal.UploadHandler
import com.mparticle.internal.messages.MPEventMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import org.junit.Test
import java.util.TreeSet
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadMessageKotlinTest : BaseStartedTest() {
    val uploadInterval = 100

    override fun transformMParticleOptions(options: com.mparticle.api.MParticleOptions): com.mparticle.api.MParticleOptions = options.apply {
        this.uploadInterval = uploadInterval
    }

    @Test
    fun testUploadGreaterThan100() {
        try {
            // track these, since the way we have our HandlerThreads are setup, we get messages carried over from the last test
            val preexistingUploadQueueMessages = AccessUtils.uploadHandlerMessageQueue
            val numMessages = 10

            Constants.setMaxMessagePerBatch(2)

            (0 until numMessages).forEach {
                MParticle.getInstance()?.logEvent(MPEvent.Builder("$it").build())
            }

            val messages = TreeSet<Int>()
            MParticle.getInstance()?.upload()
            var uploadsCount = 0
            Server
                .endpoint(EndpointType.Events)
                .assertWillReceive {
                    uploadsCount += it.body.messages.filterIsInstance<MPEventMessage>().count()
                    uploadsCount >= numMessages
                }

            // Check the UploadHandler Message queue,
            val uploadQueueMessages = AccessUtils.uploadHandlerMessageQueue
                .filter { it.what == UploadHandler.UPLOAD_MESSAGES && it.target == com.mparticle.internal.AccessUtils.uploadHandler }

            val time = System.currentTimeMillis()
            // make sure there is just 1 upload message in the queue (for the upload loop)
            assertEquals(1, uploadQueueMessages.size, "current: " + uploadQueueMessages.joinToString() + "\npre:" + preexistingUploadQueueMessages.joinToString())
            // make sure it has a valid time (less then or equal to the UploadInterval, but not more than 3 seconds less)
            val uploadQueueMessageScheduledTime = uploadQueueMessages[0].`when`
            val uploadQString = uploadQueueMessages[0].toString()
            val uploadIntervalMillis = uploadInterval * 1000
            // make sure this is the actual upload message
            assertTrue(uploadQueueMessageScheduledTime < (time + uploadIntervalMillis))
        } finally {
            Constants.setMaxMessagePerBatch(100)
        }
    }
}

// fun testMessages() {
//    // logEvent
//    // logEvent
//    // logEvent
//    // logEvent
//

/**
 *
 * 2 views, "raw" (database, requests) and "By area" (Events, Sessions & Users)
 *
 * "raw"
 * - `Server` {
 *      - endpoints: listOf<Endpoint> {
 *          Alias,
 *          Events,
 *          Config,
 *          Identity_Identify,
 *          Identity_Login,
 *          Identity_Logout,
 *          Identity_Modify,
 *      }
 *
 *      `Endpoint` ->
 *          - nextRequest(request -> response)
 *          - assertWillReceiver(request) (onReceive(request -> Boolean).then((request) -> Unit)
 *          - assertHas/WillNot/HasNot/Receive(request -> Boolean)
 *          - requests: List<Request<Response>>
 *          - stream(Request -> Unit)
 *  }
 *
 * - `Database` {
 *      - tables: listOf<Table> {
 *          Attributes,
 *          Breadcrumbs,
 *          Messages,
 *          Reporting,
 *          Sessions,
 *          Uploads,
 *      }
 *
 *      `Table` ->
 *          - assertWillReceive(request) ---- onReceive(request -> Boolean).then((request) -> Unit)
 *          - assert{Has/HasNot/WillNot}Receive(request -> Boolean)
 *          - rows: List<TableMessage
 *  }
 *
 *
 * "by area"
 * -
 * Area {
 *      fun willReceive(Data)
 *      fun stream((Data) -> ())
 *      allReceivedData: List<Data>
 * }
 *
 * Events: Area
 *  - MPEvent, CommerceEvent, Screen...every thing related
 *
 * Users: Area
 *  - users: User
 *  - changes/IdentityRequests (network)
 *
 *      `User` {
 *          - attributes (sharedprefs),      //<- maybe each one of these properties is actually an `Area` and implements each of those methods
 *          - changes/UAC (database + network)
 *          - Identities (sharedprefs)
 *          - changes/UIC (database + network)
 *          - optin/optout (database + network)
 *          - events: List<Event>
 *      }
 *
 * Sessions: Area
 *  - sessions: Session
 *  - changes/SS/SE/UpdateSessioEvents etc (database + network)
 *
 *      `Session` {
 *          object (database)
 *          attributes (maaybe just a part of object? database/sharedprefs)
 *          events:  List<Event>
 *          users: List<User>
 *      }
 */
//    class Data {
//        val SharedPreferences {
//            Users //(identities, mpid, conesntstate, opt/settings,
//        }
//        val Database {
//            Messages,
//            Uploads,
//            Attributes,
//            Sessions,
//            Breadcrumbs,
//            Reporting
//        }
//        val Network {
//            Events,
//            Config
//            Identity_Identify,
//            Identity_Login,
//            Identity_Logout,
//            Identity_Modify,
//        }
//    }
//
//    class TestableDataCollection {
//        fun willReceive()
//    }
// }
