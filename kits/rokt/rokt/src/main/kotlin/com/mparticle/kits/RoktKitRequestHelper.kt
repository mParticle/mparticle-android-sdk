package com.mparticle.kits

import android.graphics.Typeface
import com.mparticle.MParticle
import com.mparticle.MParticleTask
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityApiResult
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.rokt.roktsdk.PlacementOptions
import com.rokt.roktsdk.RoktConfig
import org.json.JSONException
import java.lang.ref.WeakReference
import java.util.Objects

internal object RoktKitRequestHelper {
    fun selectPlacements(
        kitIntegration: KitIntegration,
        roktListener: RoktKitBridge,
        viewName: String,
        attributes: Map<String, String>,
        placeHolders: Map<String, WeakReference<RoktEmbeddedView>>?,
        fontTypefaces: Map<String, WeakReference<Typeface>>?,
        config: RoktConfig?,
        options: PlacementOptions?,
    ) {
        val mutableAttributes = attributes.toMutableMap()
        val instance = MParticle.getInstance()
        if (instance == null) {
            Logger.warning("MParticle instance is null, cannot execute Rokt placement")
            return
        }
        val user = instance.Identity().currentUser
        val email = getValueIgnoreCase(mutableAttributes, "email")
        val hashedEmail = getValueIgnoreCase(mutableAttributes, "emailsha256")
        val kitConfig = kitIntegration.configuration

        confirmEmail(email, hashedEmail, user, instance.Identity(), kitConfig) {
            val finalAttributes = prepareAttributes(mutableAttributes, user, kitConfig)
            roktListener.selectPlacements(
                viewName,
                finalAttributes,
                placeHolders?.toMutableMap(),
                fontTypefaces?.toMutableMap(),
                FilteredMParticleUser.getInstance(user?.id ?: 0L, kitIntegration),
                config,
                options,
            )
        }
    }

    fun prepareAttributesAsync(
        kitIntegration: KitIntegration,
        roktListener: RoktKitBridge,
        attributes: Map<String, String>,
    ) {
        val mutableAttributes = attributes.toMutableMap()
        val instance = MParticle.getInstance()
        if (instance == null) {
            Logger.warning("MParticle instance is null, cannot prepare attributes")
            return
        }
        val user = instance.Identity().currentUser
        val email = mutableAttributes["email"]
        val hashedEmail = getValueIgnoreCase(mutableAttributes, "emailsha256")
        val kitConfig = kitIntegration.configuration

        confirmEmail(email, hashedEmail, user, instance.Identity(), kitConfig) {
            val finalAttributes = prepareAttributes(mutableAttributes, user, kitConfig)
            roktListener.enrichAttributes(
                finalAttributes,
                FilteredMParticleUser.getInstance(user?.id ?: 0L, kitIntegration),
            )
        }
    }

    fun selectShoppableAds(
        kitIntegration: KitIntegration,
        roktListener: RoktKitBridge,
        viewName: String,
        attributes: Map<String, String>,
        config: RoktConfig?,
    ) {
        val mutableAttributes = attributes.toMutableMap()
        val instance = MParticle.getInstance()
        if (instance == null) {
            Logger.warning("MParticle instance is null, cannot execute Rokt Shoppable Ads placement")
            return
        }
        val user = instance.Identity().currentUser
        val email = getValueIgnoreCase(mutableAttributes, "email")
        val hashedEmail = getValueIgnoreCase(mutableAttributes, "emailsha256")
        val kitConfig = kitIntegration.configuration

        confirmEmail(email, hashedEmail, user, instance.Identity(), kitConfig) {
            val finalAttributes = prepareAttributes(mutableAttributes, user, kitConfig)
            roktListener.selectShoppableAds(
                viewName,
                finalAttributes,
                FilteredMParticleUser.getInstance(user?.id ?: 0L, kitIntegration),
                config,
            )
        }
    }

    private fun getValueIgnoreCase(map: Map<String, String>, searchKey: String): String? {
        for ((key, value) in map) {
            if (key.equals(searchKey, ignoreCase = true)) {
                return value
            }
        }
        return null
    }

    private fun prepareAttributes(
        finalAttributes: MutableMap<String, String>,
        user: MParticleUser?,
        kitConfig: KitConfiguration?,
    ): MutableMap<String, String> {
        val jsonArray = try {
            kitConfig?.placementAttributesMapping ?: org.json.JSONArray()
        } catch (e: JSONException) {
            Logger.warning("Invalid placementAttributes for Rokt Kit JSON: ${e.message}")
            org.json.JSONArray()
        }

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val mapFrom = obj.optString("map")
            val mapTo = obj.optString("value")
            if (finalAttributes.containsKey(mapFrom)) {
                val value = finalAttributes.remove(mapFrom)
                if (value != null) {
                    finalAttributes[mapTo] = value
                }
            }
        }

        val objectAttributes = mutableMapOf<String, Any>()
        for ((key, value) in finalAttributes) {
            if (key != ROKT_ATTRIBUTE_SANDBOX_MODE) {
                objectAttributes[key] = value
            }
        }
        user?.setUserAttributes(objectAttributes)

        if (!finalAttributes.containsKey(ROKT_ATTRIBUTE_SANDBOX_MODE)) {
            finalAttributes[ROKT_ATTRIBUTE_SANDBOX_MODE] =
                Objects.toString(MPUtility.isDevEnv(), "false")
        }
        return finalAttributes
    }

    private fun confirmEmail(
        email: String?,
        hashedEmail: String?,
        user: MParticleUser?,
        identityApi: IdentityApi,
        kitConfiguration: KitConfiguration?,
        runnable: Runnable,
    ) {
        val hasEmail = !email.isNullOrEmpty()
        val hasHashedEmail = !hashedEmail.isNullOrEmpty()

        if ((hasEmail || hasHashedEmail) && user != null) {
            var selectedIdentityType: MParticle.IdentityType? = null
            try {
                val identityTypeStr = kitConfiguration?.hashedEmailUserIdentityType
                if (identityTypeStr != null) {
                    selectedIdentityType = MParticle.IdentityType.valueOf(identityTypeStr)
                }
            } catch (e: IllegalArgumentException) {
                Logger.error("Invalid identity type ${e.message}")
            }

            val existingEmail = user.userIdentities[MParticle.IdentityType.Email]
            val existingHashedEmail = selectedIdentityType?.let { user.userIdentities[it] }
            val emailMismatch = hasEmail && !email.equals(existingEmail, ignoreCase = true)
            val hashedEmailMismatch =
                hasHashedEmail && !hashedEmail.equals(existingHashedEmail, ignoreCase = true)

            if (emailMismatch || (hashedEmailMismatch && selectedIdentityType != null)) {
                if (emailMismatch && existingEmail != null) {
                    val emailMismatchMessage =
                        "The existing email on the user ($existingEmail) does not match " +
                            "the email passed to selectPlacements ($email). " +
                            "Please make sure to sync the email identity to mParticle " +
                            "as soon as it's available. " +
                            "Identifying user with the provided email in the background; " +
                            "selectPlacements continues without waiting."
                    Logger.warning(
                        emailMismatchMessage,
                    )
                } else if (hashedEmailMismatch && existingHashedEmail != null) {
                    val hashedEmailMismatchMessage =
                        "The existing hashed email on the user ($existingHashedEmail) does not match " +
                            "the hashed email passed to selectPlacements ($hashedEmail). " +
                            "Please make sure to sync the hashed email identity to mParticle " +
                            "as soon as it's available. " +
                            "Identifying user with the provided hashed email in the background; " +
                            "selectPlacements continues without waiting."
                    Logger.warning(
                        hashedEmailMismatchMessage,
                    )
                }

                val identityBuilder = IdentityApiRequest.withUser(user)
                if (emailMismatch) {
                    identityBuilder.email(email)
                }
                if (hashedEmailMismatch && selectedIdentityType != null) {
                    identityBuilder.userIdentity(selectedIdentityType, hashedEmail)
                }
                val identifyKey = listOf(
                    user.id,
                    email.takeIf { emailMismatch },
                    selectedIdentityType.takeIf { hashedEmailMismatch },
                    hashedEmail.takeIf { hashedEmailMismatch },
                ).joinToString("|")
                identifyInBackground(identityApi, identityBuilder.build(), identifyKey)
            }
        }
        runnable.run()
    }

    // Identities of the identify currently in flight, so repeated placements don't stack duplicates.
    // Expires so a task that never completes cannot block that identity for the rest of the session.
    private const val IN_FLIGHT_IDENTIFY_TIMEOUT_MS = 30_000L
    private val inFlightIdentifyLock = Any()
    private var inFlightIdentifyKey: String? = null
    private var inFlightIdentifyStartedAt = 0L
    internal var clock: () -> Long = System::currentTimeMillis

    private fun startIdentify(identifyKey: String): Boolean = synchronized(inFlightIdentifyLock) {
        val now = clock()
        if (inFlightIdentifyKey == identifyKey && now - inFlightIdentifyStartedAt < IN_FLIGHT_IDENTIFY_TIMEOUT_MS) {
            false
        } else {
            inFlightIdentifyKey = identifyKey
            inFlightIdentifyStartedAt = now
            true
        }
    }

    private fun finishIdentify(identifyKey: String) = synchronized(inFlightIdentifyLock) {
        if (inFlightIdentifyKey == identifyKey) {
            inFlightIdentifyKey = null
        }
    }

    private fun identifyInBackground(
        identityApi: IdentityApi,
        identityRequest: IdentityApiRequest,
        identifyKey: String,
    ) {
        if (!startIdentify(identifyKey)) {
            return
        }
        val task: MParticleTask<IdentityApiResult>?
        try {
            // Kit-internal identity sync (email carried on selectPlacement) — suppress so it
            // isn't reported as a partner IDENTIFY call.
            var identifyTask: MParticleTask<IdentityApiResult>? = null
            MParticle.withoutRoktApiUsage { identifyTask = identityApi.identify(identityRequest) }
            task = identifyTask
        } catch (e: Exception) {
            finishIdentify(identifyKey)
            Logger.error("Failed to sync email from selectPlacement to user in the background: ${e.message}")
            return
        }
        if (task == null) {
            finishIdentify(identifyKey)
            return
        }
        task.addFailureListener { result ->
            finishIdentify(identifyKey)
            Logger.error("Failed to sync email from selectPlacement to user in the background: ${result?.errors}")
        }
        task.addSuccessListener { result ->
            finishIdentify(identifyKey)
            Logger.debug(
                "Updated email identity in the background based on selectPlacement's attributes: " +
                    result.user.userIdentities[MParticle.IdentityType.Email],
            )
        }
        // Listeners are not replayed, so a task that completed before they were added never calls them.
        if (task.isComplete) {
            finishIdentify(identifyKey)
        }
    }

    internal fun resetInFlightIdentifyForTesting() {
        synchronized(inFlightIdentifyLock) { inFlightIdentifyKey = null }
        clock = System::currentTimeMillis
    }
}
