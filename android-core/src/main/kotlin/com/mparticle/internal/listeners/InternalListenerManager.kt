package com.mparticle.internal.listeners

import android.content.ContentValues
import android.content.Context
import android.os.Message
import com.mparticle.SdkListener
import com.mparticle.identity.AliasResponse
import com.mparticle.internal.InternalSession
import com.mparticle.internal.KitFrameworkWrapper
import com.mparticle.internal.MPUtility
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Locale

class InternalListenerManager private constructor(private val context: Context) : InternalListener {
    val sdkListeners: MutableList<WeakReference<SdkListener>> = ArrayList()
    val graphListeners: MutableList<WeakReference<GraphListener>> = ArrayList()
    private val thrown = false

    fun addListener(sdkListener: SdkListener) {
        for (listener in sdkListeners) {
            if (listener.get() === sdkListener) {
                return
            }
        }
        sdkListeners.add(WeakReference(sdkListener))
        if (sdkListener is GraphListener) {
            graphListeners.add(WeakReference(sdkListener as GraphListener))
        }
    }

    fun removeListener(sdkListener: SdkListener) {
        for (listener in ArrayList(sdkListeners)) {
            if (listener.get() === sdkListener) {
                sdkListeners.remove(listener)
            }
        }
        for (listener in ArrayList(graphListeners)) {
            if (listener.get() === sdkListener) {
                graphListeners.remove(listener)
            }
        }
    }

    override fun onApiCalled(vararg objects: Any) {
    }

    override fun onKitApiCalled(kitId: Int, used: Boolean, vararg objects: Any) {
        val stackTrace = Thread.currentThread().stackTrace
        var methodName: String? = null
        for (i in stackTrace.indices) {
            val element = stackTrace[i]
            if (element.className == KitFrameworkWrapper::class.java.name) {
                methodName = element.methodName + "()"
            }
        }
        methodName?.let { onKitApiCalled(stackTrace, it, kitId, used, *objects) }
    }

    override fun onKitApiCalled(
        methodName: String, kitId: Int, used: Boolean, vararg objects: Any
    ) {
        onKitApiCalled(Thread.currentThread().stackTrace, methodName, kitId, used, *objects)
    }

    private fun onKitApiCalled(
        stackTrace: Array<StackTraceElement>,
        methodName: String,
        kitId: Int,
        used: Boolean,
        vararg objects: Any
    ) {
        var invokingApiMethodName: String? = null
        var kitManagerMethodName: String? = null
        var foundInternal = false
        var foundExternal = false
        val objectList: MutableList<Any> = ArrayList()
        for (obj in objects) {
            objectList.add(obj)
        }
        for (i in stackTrace.indices) {
            if (!isExternalApiInvocation(stackTrace[i])) {
                foundInternal = true
            }
            if (foundInternal && !foundExternal) {
                if (isExternalApiInvocation(stackTrace[i])) {
                    invokingApiMethodName = getApiName(stackTrace[i - 1])
                    foundExternal = true
                }
            }
            if (stackTrace[i].className == "com.mparticle.kits.KitManagerImpl") {
                kitManagerMethodName = getApiName(stackTrace[i])
            }
        }
        val finalInvokingApiMethodName = invokingApiMethodName
        val finalKitManagerMethodName = kitManagerMethodName
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onKitApiCalled(
                    kitId,
                    methodName,
                    finalInvokingApiMethodName,
                    finalKitManagerMethodName,
                    objectList,
                    used
                )
            }
        })
    }

    override fun onCompositeObjects(child: Any?, parent: Any?) {
        broadcast(object : SdkGraphListenerRunnable {
            override fun run(listener: GraphListener) {
                listener.onCompositeObjects(child, parent)
            }
        })
    }

    override fun onThreadMessage(handlerName: String, msg: Message, onNewThread: Boolean) {
        var stackTrace: Array<StackTraceElement?>? = null
        if (!onNewThread) {
            stackTrace = Thread.currentThread().stackTrace
        }
        val finalStackTrace = stackTrace
        broadcast(object : SdkGraphListenerRunnable {
            override fun run(listener: GraphListener) {
                listener.onThreadMessage(handlerName, msg, onNewThread, finalStackTrace)
            }
        })
    }

    override fun onEntityStored(
        primaryKey: Long, tableName: String, contentValues: ContentValues?
    ) {
        onCompositeObjects(contentValues, tableName + primaryKey)
        val jsonObject = JSONObject()
        var table: SdkListener.DatabaseTable? = null
        table = try {
            tableName.uppercase(Locale.getDefault()).let { SdkListener.DatabaseTable.valueOf(it) }
        } catch (ex: IllegalArgumentException) {
            SdkListener.DatabaseTable.UNKNOWN
        }
        if (contentValues != null) {
            for ((key, value) in contentValues.valueSet()) {
                try {
                    if (value == null) {
                        jsonObject.put(key, JSONObject.NULL)
                    } else {
                        jsonObject.put(key, value)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
        val finalTable = table
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                finalTable?.let { listener.onEntityStored(it, primaryKey, jsonObject) }
            }
        })
    }

    override fun onNetworkRequestStarted(
        type: SdkListener.Endpoint, url: String, body: JSONObject?, vararg objects: Any
    ) {
        for (obj in objects) {
            onCompositeObjects(obj, body)
        }
        val objectList: MutableList<Any> = ArrayList()
        for (obj in objects) {
            objectList.add(obj)
        }
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onNetworkRequestStarted(type, url, body ?: JSONObject())
            }
        })
    }

    override fun onNetworkRequestFinished(
        type: SdkListener.Endpoint, url: String, response: JSONObject?, responseCode: Int
    ) {

        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onNetworkRequestFinished(type, url, response, responseCode)
            }
        })
    }

    override fun onSessionUpdated(internalSession: InternalSession) {
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onSessionUpdated(InternalSession(internalSession))
            }
        })
    }

    override fun onKitDetected(kitId: Int) {
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onKitDetected(kitId)
            }
        })
    }

    override fun onKitConfigReceived(kitId: Int, configuration: String?) {
        var jsonObject = JSONObject()
        try {
            jsonObject = JSONObject(configuration)
        } catch (e: JSONException) {
        }
        val jsonConfig = jsonObject
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onKitConfigReceived(kitId, jsonConfig)
            }
        })
    }

    override fun onKitExcluded(kitId: Int, reason: String?) {
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onKitExcluded(kitId, reason)
            }
        })
    }

    override fun onKitStarted(kitId: Int) {
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onKitStarted(kitId)
            }
        })
    }

    override fun onAliasRequestFinished(aliasResponse: AliasResponse?) {
        broadcast(object : SdkListenerRunnable {
            override fun run(listener: SdkListener) {
                listener.onAliasRequestFinished(aliasResponse)
            }
        })
    }

    private fun broadcast(runnable: SdkListenerRunnable) {
        for (listenerRef in ArrayList(sdkListeners)) {
            val listener = listenerRef.get()
            if (listener == null) {
                sdkListeners.remove(listenerRef)
            } else {
                runnable.run(listener)
            }
        }
    }

    private fun broadcast(runnable: SdkGraphListenerRunnable) {
        for (listenerRef in ArrayList(graphListeners)) {
            val listener = listenerRef.get()
            if (listener == null) {
                graphListeners.remove(listenerRef)
            } else {
                runnable.run(listener)
            }
        }
    }

    internal interface SdkListenerRunnable {
        fun run(listener: SdkListener)
    }

    internal interface SdkGraphListenerRunnable {
        fun run(listener: GraphListener)
    }

    private fun getApiName(element: StackTraceElement): String {
        val classNameString = getClassName(element.className, element.methodName)
        return getApiFormattedName(classNameString, element.methodName)
    }

    private fun getClassName(className: String, methodName: String): String {
        val packageNames =
            className.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val simpleClassName = packageNames[packageNames.size - 1]
        if (isObfuscated(simpleClassName)) {
            try {
                val superClasses: MutableList<Class<*>> = ArrayList()
                val clazz = Class.forName(className) // nosemgrep
                superClasses.add(clazz.superclass)
                for (interfce in clazz.interfaces) {
                    superClasses.add(interfce)
                }
                for (superClass in superClasses) {
                    for (method in superClass.methods) {
                        if (method.name == methodName) {
                            val superClassName = getClassName(superClass.name, methodName)
                            if (!isObfuscated(superClassName)) {
                                return superClassName
                            }
                        }
                    }
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        return simpleClassName
    }

    private fun isObfuscated(className: String): Boolean {
        return Character.isLowerCase(className.toCharArray()[0]) && className.length <= 3
    }

    private fun isExternalApiInvocation(element: StackTraceElement): Boolean {
        return !element.className.startsWith("com.mparticle") || (element.className.startsWith(
            context.applicationContext.packageName
        ) && context.applicationContext.packageName.length > 1)
    }

    private fun hasListeners(): Boolean {
        return (instance?.sdkListeners?.size ?: 0) > 0 || (instance?.graphListeners?.size ?: 0) > 0
    }

    companion object {
        private var instance: InternalListenerManager? = null
        private const val INTERNAL_LISTENER_PROP = "debug.mparticle.listener"

        @JvmStatic
        fun start(context: Context?): InternalListenerManager? {
            val canRun = MPUtility.isAppDebuggable(context) || context?.packageName == MPUtility.getProp(INTERNAL_LISTENER_PROP)
            if (instance == null && context != null && canRun) {
                instance = InternalListenerManager(context.applicationContext)
            }
            return instance
        }

        @JvmStatic
        val isEnabled: Boolean
            get() = instance != null && instance?.hasListeners() == true

        @JvmStatic
        val listener: InternalListener
            get() = instance?.let {
                if (isEnabled) it else InternalListener.EMPTY
            } ?: InternalListener.EMPTY

        fun getApiFormattedName(className: String?, methodName: String?): String {
            return StringBuilder().append(className).append(".").append(methodName).append("()")
                .toString()
        }
    }
}
