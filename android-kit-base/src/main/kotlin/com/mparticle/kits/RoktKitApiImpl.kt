package com.mparticle.kits

import android.graphics.Typeface
import com.mparticle.MParticle
import com.mparticle.MpRoktEventCallback
import com.mparticle.RoktEvent
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.internal.RoktKitApi
import com.mparticle.rokt.PlacementOptions
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.json.JSONException
import java.lang.ref.WeakReference
import java.util.Objects

/**
 * Implementation of [RoktKitApi] that wraps a [KitIntegration.RoktListener].
 *
 * This class handles user resolution and attribute preparation before delegating
 * to the underlying Rokt Kit implementation.
 */
internal class RoktKitApiImpl(
    private val roktListener: KitIntegration.RoktListener,
    private val kitIntegration: KitIntegration,
) : RoktKitApi {

    override fun execute(
        viewName: String,
        attributes: Map<String, String>,
        mpRoktEventCallback: MpRoktEventCallback?,
        placeHolders: Map<String, WeakReference<RoktEmbeddedView>>?,
        fontTypefaces: Map<String, WeakReference<Typeface>>?,
        config: RoktConfig?,
        options: PlacementOptions?,
    ) {
        try {
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
                val finalAttributes = prepareAttributes(mutableAttributes, user)
                roktListener.execute(
                    viewName,
                    finalAttributes,
                    mpRoktEventCallback,
                    placeHolders,
                    fontTypefaces,
                    FilteredMParticleUser.getInstance(user?.id ?: 0L, kitIntegration),
                    config,
                    options,
                )
            }
        } catch (e: Exception) {
            Logger.warning("Failed to call execute for Rokt Kit: ${e.message}")
        }
    }

    override fun events(identifier: String): Flow<RoktEvent> {
        return try {
            Logger.verbose("Calling events for Rokt Kit with identifier: $identifier")
            roktListener.events(identifier)
        } catch (e: Exception) {
            Logger.warning("Failed to call events for Rokt Kit: ${e.message}")
            flowOf()
        }
    }

    override fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean) {
        try {
            roktListener.purchaseFinalized(placementId, catalogItemId, status)
        } catch (e: Exception) {
            Logger.warning("Failed to call purchaseFinalized for Rokt Kit: ${e.message}")
        }
    }

    override fun close() {
        try {
            roktListener.close()
        } catch (e: Exception) {
            Logger.warning("Failed to call close for Rokt Kit: ${e.message}")
        }
    }

    override fun setSessionId(sessionId: String) {
        try {
            roktListener.setSessionId(sessionId)
        } catch (e: Exception) {
            Logger.warning("Failed to call setSessionId for Rokt Kit: ${e.message}")
        }
    }

    override fun getSessionId(): String? {
        return try {
            roktListener.sessionId
        } catch (e: Exception) {
            Logger.warning("Failed to call getSessionId for Rokt Kit: ${e.message}")
            null
        }
    }

    override fun prepareAttributesAsync(attributes: Map<String, String>) {
        try {
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
                val finalAttributes = prepareAttributes(mutableAttributes, user)
                roktListener.enrichAttributes(
                    finalAttributes,
                    FilteredMParticleUser.getInstance(user?.id ?: 0L, kitIntegration),
                )
            }
        } catch (e: Exception) {
            Logger.warning("Failed to call prepareAttributesAsync for Rokt Kit: ${e.message}")
        }
    }

    // Helper methods

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
    ): MutableMap<String, String> {
        val kitConfig = kitIntegration.configuration
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
            if (key != Constants.MessageKey.SANDBOX_MODE_ROKT) {
                objectAttributes[key] = value
            }
        }
        user?.setUserAttributes(objectAttributes)

        if (!finalAttributes.containsKey(Constants.MessageKey.SANDBOX_MODE_ROKT)) {
            finalAttributes[Constants.MessageKey.SANDBOX_MODE_ROKT] =
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
                // If there's an existing email but it doesn't match the passed-in email, log a warning
                if (emailMismatch && existingEmail != null) {
                    Logger.warning(
                        "The existing email on the user ($existingEmail) does not match the email passed to selectPlacements ($email). " +
                            "Please make sure to sync the email identity to mParticle as soon as it's available. " +
                            "Identifying user with the provided email before continuing to selectPlacements.",
                    )
                } else if (hashedEmailMismatch && existingHashedEmail != null) {
                    // If there's an existing other but it doesn't match the passed-in hashed email, log a warning
                    Logger.warning(
                        "The existing hashed email on the user ($existingHashedEmail) does not match the hashed email passed to selectPlacements ($hashedEmail). " +
                            "Please make sure to sync the hashed email identity to mParticle as soon as it's available. " +
                            "Identifying user with the provided hashed email before continuing to selectPlacements.",
                    )
                }

                val identityBuilder = IdentityApiRequest.withUser(user)
                if (emailMismatch) {
                    identityBuilder.email(email)
                }
                if (hashedEmailMismatch && selectedIdentityType != null) {
                    identityBuilder.userIdentity(selectedIdentityType, hashedEmail)
                }

                val identityRequest = identityBuilder.build()
                val task = identityApi.identify(identityRequest)

                task.addFailureListener { result ->
                    Logger.error("Failed to sync email from selectPlacement to user: ${result?.errors}")
                    runnable.run()
                }

                task.addSuccessListener { result ->
                    Logger.debug(
                        "Updated email identity based on selectPlacement's attributes: " +
                            result.user.userIdentities[MParticle.IdentityType.Email],
                    )
                    runnable.run()
                }
            } else {
                runnable.run()
            }
        } else {
            runnable.run()
        }
    }
}
