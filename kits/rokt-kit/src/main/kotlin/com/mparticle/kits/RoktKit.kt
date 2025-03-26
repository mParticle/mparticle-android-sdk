package com.mparticle.kits

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.mparticle.commerce.CommerceEvent
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration.*
import com.rokt.roktsdk.Rokt
import java.math.BigDecimal


/**
 * MParticle embedded implementation of the Rokt Library.
 *
 * Learn more at our [Developer Docs](https://docs.rokt.com/developers/integration-guides/android)
 */
class RoktKit : KitIntegration(), CommerceListener, IdentityListener {
    private var applicationContext: Context? = null

    override fun getName(): String = NAME

    override fun getInstance(): RoktKit = this

    public override fun onKitCreate(
        settings: Map<String, String>,
        ctx: Context
    ): List<ReportingMessage> {
        applicationContext = ctx.applicationContext
        val roktTagId = settings[ROKT_ACCOUNT_ID]
        if (KitUtils.isEmpty(roktTagId)) {
            throwOnKitCreateError(NO_ROKT_ACCOUNT_ID)
        }
        applicationContext?.let {
            val manager = context.packageManager
            if (roktTagId != null) {
                try {
                    val info = manager.getPackageInfoForApp(context.packageName, 0)
                    val application = context.applicationContext as Application
                    Rokt.init(roktTagId, info.versionName , application)
                } catch (e: PackageManager.NameNotFoundException) {
                    throwOnKitCreateError(NO_APP_VERSION_FOUND)
                } catch (e: Exception) {
                    logError("Error initializing Rokt", e)
                }
            }
        }
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    public override fun reset() {
        super.reset()
    }

    /*
     * Overrides for CommerceListener
     */
    override fun logLtvIncrease(
        bigDecimal: BigDecimal, bigDecimal1: BigDecimal,
        s: String, map: Map<String, String>
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> {
        return emptyList()
    }

    /*
     * Overrides for IdentityListener
     */
    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {}

    private fun logError(message: String, t: Throwable) {
        Logger.error(t, "RoktKit: $message")
    }

    private fun throwOnKitCreateError(message: String) {
        throw IllegalArgumentException(message)
    }

    companion object {
        const val NAME = "Rokt"
        const val ROKT_ACCOUNT_ID = "accountId"
        const val NO_ROKT_ACCOUNT_ID = "No Rokt account ID provided, can't initialize kit."
        const val NO_APP_VERSION_FOUND = "No App version found, can't initialize kit."
    }
}

fun PackageManager.getPackageInfoForApp(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }