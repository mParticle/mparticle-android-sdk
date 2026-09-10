package com.mparticle.rokt

/**
 * A Rokt session suitable for handoff between native and non-native integrations.
 *
 * Includes the session id and short-lived session token used to authorize offers and events.
 *
 * @param sessionId The Rokt session identifier. Must be non-empty when passed to [com.mparticle.kits.Rokt.setSession].
 * @param sessionToken The Rokt session token. Must be non-empty when passed to [com.mparticle.kits.Rokt.setSession].
 * @param expiresAt Optional Unix epoch milliseconds when [sessionToken] expires. The underlying Rokt SDK applies its default expiry when
 * this value is omitted or already in the past.
 */
data class RoktSession @JvmOverloads constructor(val sessionId: String, val sessionToken: String, val expiresAt: Long? = null)
