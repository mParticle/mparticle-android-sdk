package com.mparticle.kits

import android.graphics.Typeface
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.MParticleTask
import com.mparticle.TypedUserAttributeListener
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
import java.util.concurrent.atomic.AtomicLong

internal object RoktKitRequestHelper {
    private val placementSequence = AtomicLong()

    // Newest call that passed each canonical privacy key, per MPID. At most four entries per MPID
    // seen in this process, so it stays small and is never pruned.
    private val lastPrivacySequence = HashMap<Pair<Long, String>, Long>()

    // Records `sequence` for each of `keys` on `mpid` and returns the keys that no newer call has
    // passed for that user.
    private fun claimPrivacyKeys(mpid: Long, keys: Set<String>, sequence: Long): Set<String> =
        synchronized(lastPrivacySequence) {
            keys.filterTo(mutableSetOf()) { key ->
                val newest = lastPrivacySequence[mpid to key]
                (newest == null || newest <= sequence).also { claimed ->
                    if (claimed) lastPrivacySequence[mpid to key] = sequence
                }
            }
        }

    private fun isPrivacyKeySuperseded(mpid: Long, key: String, sequence: Long): Boolean =
        synchronized(lastPrivacySequence) {
            (lastPrivacySequence[mpid to key] ?: 0L) > sequence
        }

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

        confirmEmail(
            email = email,
            hashedEmail = hashedEmail,
            user = user,
            identityApi = instance.Identity(),
            kitConfiguration = kitConfig,
            callerAttributes = attributes,
        ) {
            prepareAttributes(mutableAttributes, user, kitConfig) { finalAttributes ->
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
    }

    fun prepareAttributesAsync(
        kitIntegration: KitIntegration,
        roktListener: RoktKitBridge,
        attributes: Map<String, String>,
        onEnriched: ((MutableMap<String, String>, FilteredMParticleUser?) -> Unit)? = null,
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

        confirmEmail(
            email = email,
            hashedEmail = hashedEmail,
            user = user,
            identityApi = instance.Identity(),
            kitConfiguration = kitConfig,
            callerAttributes = attributes,
        ) {
            prepareAttributes(mutableAttributes, user, kitConfig) { finalAttributes ->
                val filterUser = FilteredMParticleUser.getInstance(user?.id ?: 0L, kitIntegration)
                if (onEnriched != null) {
                    onEnriched(finalAttributes, filterUser)
                } else {
                    roktListener.enrichAttributes(finalAttributes, filterUser)
                }
            }
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

        confirmEmail(
            email = email,
            hashedEmail = hashedEmail,
            user = user,
            identityApi = instance.Identity(),
            kitConfiguration = kitConfig,
            callerAttributes = attributes,
        ) {
            prepareAttributes(mutableAttributes, user, kitConfig) { finalAttributes ->
                roktListener.selectShoppableAds(
                    viewName,
                    finalAttributes,
                    FilteredMParticleUser.getInstance(user?.id ?: 0L, kitIntegration),
                    config,
                )
            }
        }
    }

    // Case-insensitive to match the iOS and web kits; unrecognised values such as "Unknown" disable the mapping.
    fun parseIdentityType(value: String?): MParticle.IdentityType? =
        MParticle.IdentityType.values().firstOrNull { it.name.equals(value, ignoreCase = true) }

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
        onPrepared: (MutableMap<String, String>) -> Unit,
    ) {
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

        val callerPrivacy = extractPrivacyAttributesFromStrings(finalAttributes)
        val explicitSandbox = extractSandboxValue(finalAttributes)

        val iterator = finalAttributes.entries.iterator()
        while (iterator.hasNext()) {
            val (key, _) = iterator.next()
            if (isReservedAttributeKey(key)) {
                iterator.remove()
            }
        }

        val objectAttributes = mutableMapOf<String, Any>()
        for ((key, value) in finalAttributes) {
            if (value.isNotEmpty() && !isRoktOnlyAttribute(key)) {
                objectAttributes[key] = value
            }
        }
        if (objectAttributes.isNotEmpty()) {
            user?.setUserAttributes(objectAttributes)
        }

        resolveUserAttributes(user) { profileAttributes ->
            clearOverriddenPrivacyAttributes(user, callerPrivacy, profileAttributes)

            val profilePrivacy = extractPrivacyAttributesFromObjects(profileAttributes)
            val effectivePrivacy = mergePrivacyMapsWithOptOutPrecedence(profilePrivacy, callerPrivacy)
            finalAttributes.putAll(effectivePrivacy)

            syncNoTargetingToUser(
                user = user,
                noTargetingValue = finalAttributes[ROKT_ATTRIBUTE_NO_TARGETING],
                profileAttributes = profileAttributes,
            )

            finalAttributes[ROKT_ATTRIBUTE_SANDBOX_MODE] =
                explicitSandbox ?: Objects.toString(MPUtility.isDevEnv(), "false")
            onPrepared(finalAttributes)
        }
    }

    private fun resolveUserAttributes(user: MParticleUser?, onResolved: (Map<String, Any?>?) -> Unit) {
        if (user == null) {
            onResolved(null)
            return
        }
        var completed = false
        val listener = object : TypedUserAttributeListener {
            override fun onUserAttributesReceived(
                userAttributes: Map<String, Any?>,
                userAttributeLists: Map<String, List<String?>?>,
                mpid: Long,
            ) {
                if (!completed) {
                    completed = true
                    val merged = LinkedHashMap<String, Any?>()
                    merged.putAll(userAttributes)
                    merged.putAll(userAttributeLists)
                    onResolved(merged)
                }
            }
        }
        var readScheduled = true
        val immediate = try {
            user.getUserAttributes(listener)
        } catch (_: Exception) {
            readScheduled = false
            null
        }
        if (!completed) {
            val mainLooper = try {
                Looper.getMainLooper()
            } catch (_: Exception) {
                null
            }
            // On the main looper the core queues the read behind pending writes and returns null.
            val asyncScheduledOnMainLooper = readScheduled &&
                immediate == null &&
                mainLooper != null &&
                mainLooper === Looper.myLooper() &&
                MParticle.getInstance() != null
            if (!asyncScheduledOnMainLooper) {
                completed = true
                val fallback = try {
                    user.userAttributes
                } catch (_: Exception) {
                    immediate
                }
                onResolved(fallback ?: immediate)
            }
        }
    }

    private fun callerPrivacyAttributes(
        rawCallerAttributes: Map<String, String>,
        kitConfig: KitConfiguration?,
    ): Map<String, String> {
        val mappedCaller = rawCallerAttributes.toMutableMap()
        val jsonArray = try {
            kitConfig?.placementAttributesMapping ?: org.json.JSONArray()
        } catch (_: JSONException) {
            org.json.JSONArray()
        }
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val mapFrom = obj.optString("map")
            val mapTo = obj.optString("value")
            if (mappedCaller.containsKey(mapFrom)) {
                val value = mappedCaller.remove(mapFrom)
                if (value != null) {
                    mappedCaller[mapTo] = value
                }
            }
        }
        return extractPrivacyAttributesFromStrings(mappedCaller)
    }

    // Applies an identifying call's privacy attributes to the user that identify returned. Keys a
    // newer call has passed for that user are left to the newer call.
    private fun syncIdentifiedUserPrivacy(
        identifiedUser: MParticleUser,
        rawCallerPrivacy: Map<String, String>,
        sequence: Long,
    ) {
        val claimedKeys = claimPrivacyKeys(identifiedUser.id, rawCallerPrivacy.keys, sequence)
        val callerPrivacy = rawCallerPrivacy.filterKeys { it in claimedKeys }
        val identifiedProfileAttrs = identifiedUser.userAttributes
        clearOverriddenPrivacyAttributes(identifiedUser, callerPrivacy, identifiedProfileAttrs)
        if (isPrivacyKeySuperseded(identifiedUser.id, ROKT_ATTRIBUTE_NO_TARGETING, sequence)) return
        val identifiedProfilePrivacy = extractPrivacyAttributesFromObjects(identifiedProfileAttrs)
        val effectivePrivacy = mergePrivacyMapsWithOptOutPrecedence(identifiedProfilePrivacy, callerPrivacy)
        syncNoTargetingToUser(
            user = identifiedUser,
            noTargetingValue = effectivePrivacy[ROKT_ATTRIBUTE_NO_TARGETING],
            profileAttributes = identifiedProfileAttrs,
        )
    }

    // A per-call opt-out removes stored non-"true" copies so they cannot loosen a later call.
    private fun clearOverriddenPrivacyAttributes(
        user: MParticleUser?,
        callerPrivacy: Map<String, String>,
        profileAttributes: Map<String, Any?>?,
    ) {
        if (user == null || profileAttributes == null) return
        val optOutCallerKeys = callerPrivacy.filterValues { it.equals("true", ignoreCase = true) }.keys
        if (optOutCallerKeys.isEmpty()) return
        for ((existingKey, existingValue) in profileAttributes) {
            val canonical = canonicalPrivacyKey(existingKey) ?: continue
            if (canonical in optOutCallerKeys && !existingValue?.toString().equals("true", ignoreCase = true)) {
                MParticle.withoutRoktApiUsage {
                    user.removeUserAttribute(existingKey)
                }
            }
        }
    }

    private fun syncNoTargetingToUser(
        user: MParticleUser?,
        noTargetingValue: String?,
        profileAttributes: Map<String, Any?>?,
    ) {
        if (user == null) return
        when {
            noTargetingValue.equals("true", ignoreCase = true) -> {
                val alreadySet = profileAttributes?.entries?.any {
                    it.key.equals(NO_TARGETING_USER_ATTRIBUTE, ignoreCase = true) &&
                        it.value?.toString().equals("true", ignoreCase = true)
                } == true
                if (!alreadySet) {
                    MParticle.withoutRoktApiUsage {
                        user.setUserAttribute(NO_TARGETING_USER_ATTRIBUTE, true)
                    }
                }
            }
            noTargetingValue.equals("false", ignoreCase = true) -> {
                val existingKeys = profileAttributes?.keys?.filter {
                    it.equals(NO_TARGETING_USER_ATTRIBUTE, ignoreCase = true)
                }.orEmpty()
                for (existingKey in existingKeys) {
                    MParticle.withoutRoktApiUsage {
                        user.removeUserAttribute(existingKey)
                    }
                }
            }
        }
    }

    private fun confirmEmail(
        email: String?,
        hashedEmail: String?,
        user: MParticleUser?,
        identityApi: IdentityApi,
        kitConfiguration: KitConfiguration?,
        callerAttributes: Map<String, String>,
        runnable: Runnable,
    ) {
        val callerPrivacy = callerPrivacyAttributes(callerAttributes, kitConfiguration)
        val sequence = placementSequence.incrementAndGet()
        user?.let { claimPrivacyKeys(it.id, callerPrivacy.keys, sequence) }
        val hasEmail = !email.isNullOrEmpty()
        val hasHashedEmail = !hashedEmail.isNullOrEmpty()

        if ((hasEmail || hasHashedEmail) && user != null) {
            val selectedIdentityType = parseIdentityType(kitConfiguration?.hashedEmailUserIdentityType)

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
                identifyInBackground(identityApi, identityBuilder.build()) { identifiedUser ->
                    syncIdentifiedUserPrivacy(identifiedUser, callerPrivacy, sequence)
                }
            }
        }
        runnable.run()
    }

    private fun identifyInBackground(
        identityApi: IdentityApi,
        identityRequest: IdentityApiRequest,
        onIdentifiedUser: ((MParticleUser) -> Unit)? = null,
    ) {
        val task: MParticleTask<IdentityApiResult>?
        try {
            // Kit-internal identity sync (email carried on selectPlacement) — suppress so it
            // isn't reported as a partner IDENTIFY call.
            var identifyTask: MParticleTask<IdentityApiResult>? = null
            MParticle.withoutRoktApiUsage { identifyTask = identityApi.identify(identityRequest) }
            task = identifyTask
        } catch (e: Exception) {
            Logger.error("Failed to sync email from selectPlacement to user in the background: ${e.message}")
            return
        }
        task?.addFailureListener { result ->
            Logger.error("Failed to sync email from selectPlacement to user in the background: ${result?.errors}")
        }
        task?.addSuccessListener { result ->
            Logger.debug(
                "Updated email identity in the background based on selectPlacement's attributes: " +
                    result.user.userIdentities[MParticle.IdentityType.Email],
            )
            result?.user?.let { onIdentifiedUser?.invoke(it) }
        }
    }
}
