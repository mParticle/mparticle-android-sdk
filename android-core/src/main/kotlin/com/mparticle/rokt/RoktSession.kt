package com.mparticle.rokt

/**
 * A Rokt session suitable for handoff between native and non-native integrations.
 *
 * Includes the session id plus an optional short-lived session token used to authorize offers and
 * events. Mirrors Web launcher options: `sessionId` is required for handoff; `sessionToken` is
 * optional (Bearer continuity when present).
 *
 * @param sessionId The Rokt session identifier. Must be non-empty when passed to [com.mparticle.kits.Rokt.setSession].
 * @param sessionToken Optional JWT session token used as a Bearer credential for offers and events.
 * When omitted or blank, [com.mparticle.kits.Rokt.setSession] applies the session id only (same as
 * Web `sessionId` without `sessionToken`).
 * @param expiresAt Optional Unix epoch milliseconds when [sessionToken] expires (matches server
 * `expires_at` when known). Ignored when [sessionToken] is absent.
 */
data class RoktSession @JvmOverloads constructor(
    val sessionId: String,
    val sessionToken: String? = null,
    val expiresAt: Long? = null,
)
