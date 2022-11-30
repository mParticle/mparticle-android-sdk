package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.internal.Logger
import org.json.JSONException
import java.lang.reflect.Constructor
import java.util.HashMap
import kotlin.Exception

open class KitIntegrationFactory {
    val supportedKits: MutableMap<Int, Class<*>> = HashMap()

    init {
        loadIntegrations()
    }

    /**
     * This is the canonical method mapping all known Kit/Module IDs to Kit class names.
     *
     * @return a mapping of module Ids to kit classes
     */
    protected val knownIntegrations: Map<Int, String>
        protected get() {
            val kits: MutableMap<Int, String> = HashMap()
            kits[MParticle.ServiceProviders.ADJUST] = "com.mparticle.kits.AdjustKit"
            kits[MParticle.ServiceProviders.APPBOY] = "com.mparticle.kits.AppboyKit"
            kits[MParticle.ServiceProviders.BRANCH_METRICS] = "com.mparticle.kits.BranchMetricsKit"
            kits[MParticle.ServiceProviders.COMSCORE] = "com.mparticle.kits.ComscoreKit"
            kits[MParticle.ServiceProviders.KOCHAVA] = "com.mparticle.kits.KochavaKit"
            kits[MParticle.ServiceProviders.FORESEE_ID] = "com.mparticle.kits.ForeseeKit"
            kits[MParticle.ServiceProviders.LOCALYTICS] = "com.mparticle.kits.LocalyticsKit"
            kits[MParticle.ServiceProviders.FLURRY] = "com.mparticle.kits.FlurryKit"
            kits[MParticle.ServiceProviders.WOOTRIC] = "com.mparticle.kits.WootricKit"
            kits[MParticle.ServiceProviders.CRITTERCISM] = "com.mparticle.kits.CrittercismKit"
            kits[MParticle.ServiceProviders.TUNE] = "com.mparticle.kits.TuneKit"
            kits[MParticle.ServiceProviders.APPSFLYER] = "com.mparticle.kits.AppsFlyerKit"
            kits[MParticle.ServiceProviders.APPTENTIVE] = "com.mparticle.kits.ApptentiveKit"
            kits[MParticle.ServiceProviders.BUTTON] = "com.mparticle.kits.ButtonKit"
            kits[MParticle.ServiceProviders.URBAN_AIRSHIP] = "com.mparticle.kits.UrbanAirshipKit"
            kits[MParticle.ServiceProviders.LEANPLUM] = "com.mparticle.kits.LeanplumKit"
            kits[MParticle.ServiceProviders.APPTIMIZE] = "com.mparticle.kits.ApptimizeKit"
            kits[MParticle.ServiceProviders.REVEAL_MOBILE] = "com.mparticle.kits.RevealMobileKit"
            kits[MParticle.ServiceProviders.RADAR] = "com.mparticle.kits.RadarKit"
            kits[MParticle.ServiceProviders.ITERABLE] = "com.mparticle.kits.IterableKit"
            kits[MParticle.ServiceProviders.SKYHOOK] = "com.mparticle.kits.SkyhookKit"
            kits[MParticle.ServiceProviders.SINGULAR] = "com.mparticle.kits.SingularKit"
            kits[MParticle.ServiceProviders.ADOBE] = "com.mparticle.kits.AdobeKit"
            kits[MParticle.ServiceProviders.TAPLYTICS] = "com.mparticle.kits.TaplyticsKit"
            kits[MParticle.ServiceProviders.OPTIMIZELY] = "com.mparticle.kits.OptimizelyKit"
            kits[MParticle.ServiceProviders.RESPONSYS] = "com.mparticle.kits.ResponsysKit"
            kits[MParticle.ServiceProviders.CLEVERTAP] = "com.mparticle.kits.CleverTapKit"
            kits[MParticle.ServiceProviders.GOOGLE_ANALYTICS_FIREBASE] =
                "com.mparticle.kits.GoogleAnalyticsFirebaseKit"
            kits[MParticle.ServiceProviders.GOOGLE_ANALYTICS_FIREBASE_GA4] =
                "com.mparticle.kits.GoogleAnalyticsFirebaseGA4Kit"
            kits[MParticle.ServiceProviders.PILGRIM] = "com.mparticle.kits.PilgrimKit"
            kits[MParticle.ServiceProviders.ONETRUST] = "com.mparticle.kits.OneTrustKit"
            kits[MParticle.ServiceProviders.SWRVE] = "com.mparticle.kits.SwrveKit"
            kits[MParticle.ServiceProviders.BLUESHIFT] = "com.mparticle.kits.BlueshiftKit"
            kits[MParticle.ServiceProviders.NEURA] = "com.mparticle.kits.NeuraKit"
            return kits
        }

    @Throws(JSONException::class, ClassNotFoundException::class)
    fun createInstance(manager: KitManagerImpl?, configuration: KitConfiguration): KitIntegration? {
        val kit = createInstance(manager, configuration.kitId)
        return kit?.apply { setConfiguration(configuration) }
    }

    @Throws(JSONException::class, ClassNotFoundException::class)
    open fun createInstance(manager: KitManagerImpl?, moduleId: Int): KitIntegration? {
        if (!supportedKits.isEmpty()) {
            try {
                val constructor: Constructor<KitIntegration> = supportedKits[moduleId]!!
                    .getConstructor() as Constructor<KitIntegration>
                constructor.isAccessible = true
                return constructor.newInstance()
                    .setKitManager(manager)
            } catch (e: Exception) {
                Logger.debug(e, "Failed to create Kit with ID: $moduleId")
            }
        }
        return null
    }

    private fun loadIntegrations() {
        val knownIntegrations = knownIntegrations
        for ((key, value) in knownIntegrations) {
            val kitClass = loadKit(value)
            if (kitClass != null) {
                supportedKits[key] = kitClass
                Logger.debug(value.substring(value.lastIndexOf(".") + 1) + " detected.")
            }
        }
    }

    private fun loadKit(className: String): Class<*>? {
        try {
            return Class.forName(className)
        } catch (e: Exception) {
            Logger.verbose("Kit not bundled: ", className)
        }
        return null
    }

    fun addSupportedKit(kitId: Int, kitIntegration: Class<out KitIntegration?>) {
        val previousKit = supportedKits[kitId]
        if (previousKit != null) {
            Logger.warning("Overriding kitId " + kitId + ". KitIntegration: " + kitIntegration.name + " will replace existing KitIntegration: " + previousKit.name)
        }
        supportedKits[kitId] = kitIntegration
    }

    /**
     * Get the module IDs of the Kits that have been detected.
     *
     * @return
     */
    fun getSupportedKits(): Set<Int> {
        return supportedKits.keys
    }

    open fun isSupported(kitModuleId: Int): Boolean {
        return supportedKits.containsKey(kitModuleId)
    }
}
