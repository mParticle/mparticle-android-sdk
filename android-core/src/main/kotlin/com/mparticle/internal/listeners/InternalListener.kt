package com.mparticle.internal.listeners

import android.content.ContentValues
import android.os.Build
import android.os.Message
import androidx.annotation.RequiresApi
import com.mparticle.SdkListener
import com.mparticle.identity.AliasResponse
import com.mparticle.internal.InternalSession
import org.json.JSONObject

interface InternalListener {
    /**
     * This method should be called within the body of a public API method. Generally
     * we only want to instrument API methods which "do something", i.e., log an event or change
     * a User's state, not simple getters
     *
     * @param objects the arguments passed into the API method
     */
    fun onApiCalled(vararg objects: Any)

    /**
     * To be called when a Kit's API method is invoked. This overloaded variant should be used when
     * the name of the method containing this method's invocation (in KitManagerImpl) matches the name of the
     * Kit's method being invoked
     *
     * @param kitId   the Id of the kit
     * @param used    whether the Kit's method returned ReportingMessages, or null if return type is void
     * @param objects the arguments supplied to the Kit
     */
    fun onKitApiCalled(kitId: Int, used: Boolean, vararg objects: Any)

    /**
     * to be called when a Kit's API method is invoked, and the name of the Kit's method is different
     * then the method containing this method's invocation
     *
     * @param methodName the name of the Kit's method being called
     * @param kitId      the Id of the kit
     * @param used       whether the Kit's method returned ReportingMessages, or null if return type is void
     * @param objects    the arguments supplied to the Kit
     */
    fun onKitApiCalled(methodName: String, kitId: Int, used: Boolean, vararg objects: Any)

    /**
     * establishes a child-parent relationship between two objects. It is not necessary to call this
     * method for objects headed to the MessageHandler from the public API, or objects headed to kits
     *
     * @param child  the child object
     * @param parent the parent object
     */
    fun onCompositeObjects(child: Any?, parent: Any?)

    /**
     * denotes that an object is going to be passed to a new Thread, and is a candidate to be a "composite"
     * This method should be called twice per thread change, once before the shift to the new Thread,
     * and once as soon as it lands on the new Thread
     *
     * @param handlerName the Name of the Handler class, for example "com.mparticle.internal.MessageHandler"
     * @param msg         the Message object
     */
    fun onThreadMessage(handlerName: String, msg: Message, onNewThread: Boolean)

    /**
     * indicates that an entry has been stored in the Database
     *
     * @param rowId         the rowId denoted by the "_id" column value
     * @param tableName     the name of the database table
     * @param contentValues the ContentValues object to be inserted
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    fun onEntityStored(rowId: Long, tableName: String, contentValues: ContentValues?)

    /**
     * indicates that a Network Request has been started
     *
     * @param type    the type of Request, should either be "CONFIG", "EVENTS" or "Identity.Login/Logout/Identify/Modify"
     * @param url     the request url
     * @param body    the request body
     * @param objects any underlying objects that the request body is derived from, for example, an IdentityApiRequest instance
     */
    fun onNetworkRequestStarted(
        type: SdkListener.Endpoint,
        url: String,
        body: JSONObject?,
        vararg objects: Any
    )

    /**
     * indicates that a NetworkRequest has been finished
     *
     * @param url          the request url
     * @param response     the response body
     * @param responseCode the response code
     */
    fun onNetworkRequestFinished(
        type: SdkListener.Endpoint,
        url: String,
        response: JSONObject?,
        responseCode: Int
    )

    /**
     * this should be called when the current Session changes, for example, it starts, stops or the
     * event count changes
     *
     * @param internalSession
     */
    fun onSessionUpdated(internalSession: InternalSession)

    /**
     * indicates that a Kit dependency is present
     *
     * @param kitId
     */
    fun onKitDetected(kitId: Int)

    /**
     * indicates that we have received a configuration for a Kit
     *
     * @param kitId
     * @param configuration
     */
    fun onKitConfigReceived(kitId: Int, configuration: String?)

    /**
     * indicates that a Kit was present, and a configuration was received for it, but it was not started,
     * or it was stopped. This could be because it crashed, or because a User's logged in status required
     * that we shut it down
     *
     * @param kitId
     * @param reason
     */
    fun onKitExcluded(kitId: Int, reason: String?)

    /**
     * indicates that a Kit successfully executed it's onKitCreate() method
     *
     * @param kitId
     */
    fun onKitStarted(kitId: Int)

    fun onAliasRequestFinished(aliasResponse: AliasResponse?)

    companion object {
        @JvmField
        val EMPTY: InternalListener = object : InternalListener {
            override fun onApiCalled(vararg objects: Any) { /* stub */
            }

            override fun onKitApiCalled(
                kitId: Int,
                used: Boolean,
                vararg objects: Any
            ) { /* stub */
            }

            override fun onKitApiCalled(
                methodName: String,
                kitId: Int,
                used: Boolean,
                vararg objects: Any
            ) { /* stub */
            }

            override fun onEntityStored(
                rowId: Long,
                tableName: String,
                contentValues: ContentValues?
            ) { /* stub */
            }

            override fun onNetworkRequestStarted(
                type: SdkListener.Endpoint,
                url: String,
                body: JSONObject?,
                vararg objects: Any
            ) { /* stub */
            }

            override fun onNetworkRequestFinished(
                type: SdkListener.Endpoint,
                url: String,
                response: JSONObject?,
                responseCode: Int
            ) { /* stub */
            }

            override fun onSessionUpdated(internalSession: InternalSession) { /* stub */
            }

            override fun onKitDetected(kitId: Int) { /* stub */
            }

            override fun onKitConfigReceived(kitId: Int, configuration: String?) { /* stub */
            }

            override fun onKitExcluded(kitId: Int, reason: String?) { /* stub */
            }

            override fun onKitStarted(kitId: Int) { /* stub */
            }

            override fun onAliasRequestFinished(aliasResponse: AliasResponse?) { /* stub */
            }

            override fun onCompositeObjects(child: Any?, parent: Any?) { /* stub */
            }

            override fun onThreadMessage(
                handlerName: String,
                msg: Message,
                onNewThread: Boolean
            ) { /* stub */
            }
        }
    }
}
