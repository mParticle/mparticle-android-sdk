package com.adobe.marketing.mobile

import android.app.Application

class MobileCore {
    companion object {
        var configKey: String? = null

        @JvmStatic
        fun start(callback: AdobeCallback<Any>) {
            callback.call("")
        }

        @JvmStatic
        fun configureWithAppID(key: String) {
            configKey = key
        }

        @JvmStatic
        fun registerExtensions(
            extensions: List<Class<out Extension>>,
            completionCallback: AdobeCallback<*>?,
        ) {
            // Method implementation here
        }

        @JvmStatic
        fun setApplication(application: Application) {}
    }
}

interface AdobeCallback<T> {
    fun call(t: T)
}

open class BaseAdobeExtension {
    internal abstract class AnalyticsExtension(
        extensionApi: ExtensionApi,
    ) : com.adobe.marketing.mobile.Extension(extensionApi) {
        companion object {
            private val ANALYTICS_HARD_DEPENDENCIES: List<String> =
                listOf(
                    // Add actual hard dependencies here
                )

            private val ANALYTICS_SOFT_DEPENDENCIES: List<String> =
                listOf(
                    // Add actual soft dependencies here
                )

            private const val CLASS_NAME: String = "YourClassName" // Replace "YourClassName" with the actual class name
        }
    }

    companion object {
        @JvmField
        val EXTENSION: Class<out Extension> = AnalyticsExtension::class.java

        @JvmStatic
        fun registerExtension() {}
    }
}

class MobileServices : BaseAdobeExtension()

class Analytics : BaseAdobeExtension()

class UserProfile : BaseAdobeExtension()

class Lifecycle : BaseAdobeExtension()

class Signal : BaseAdobeExtension()

object Identity : BaseAdobeExtension() {
    @JvmStatic
    fun getExperienceCloudId(callback: AdobeCallback<String>) {}
}

object Media : BaseAdobeExtension() {
    @JvmStatic
    fun createTracker(): MediaTracker = MediaTracker()

    enum class MediaType {
        Video,
        Audio,
    }
}

open class MediaTracker
