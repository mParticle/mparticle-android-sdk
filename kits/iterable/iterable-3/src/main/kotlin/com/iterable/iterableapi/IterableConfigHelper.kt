package com.iterable.iterableapi

object IterableConfigHelper {
    fun createConfigBuilderFromIterableConfig(config: IterableConfig?): IterableConfig.Builder {
        val builder = IterableConfig.Builder()
        if (config != null) {
            builder.apply {
                setPushIntegrationName(config.pushIntegrationName)
                setUrlHandler(config.urlHandler)
                setCustomActionHandler(config.customActionHandler)
                setAutoPushRegistration(config.autoPushRegistration)
                setCheckForDeferredDeeplink(config.checkForDeferredDeeplink)
                setLogLevel(config.logLevel)
                setInAppHandler(config.inAppHandler)
                setInAppDisplayInterval(config.inAppDisplayInterval)
                setAllowedProtocols(config.allowedProtocols)
            }
        }
        return builder
    }
}
