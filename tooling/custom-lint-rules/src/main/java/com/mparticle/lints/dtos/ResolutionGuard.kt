package com.mparticle.lints.dtos

/**
 * Bounds the depth of expression resolution. Some qualified call chains cause a resolved
 * expression's parent to keep resolving into itself; without a bound that recurses until the
 * stack overflows, which callers' try/catch(Exception) blocks don't catch. Throwing a regular
 * exception once a generous depth is exceeded keeps that failure catchable.
 */
internal object ResolutionGuard {
    private const val MAX_DEPTH = 50
    private val depth = ThreadLocal.withInitial { 0 }

    fun <T> guarded(block: () -> T): T {
        val current = depth.get()
        if (current >= MAX_DEPTH) {
            throw IllegalStateException("Expression resolution exceeded max depth of $MAX_DEPTH")
        }
        depth.set(current + 1)
        try {
            return block()
        } finally {
            depth.set(current)
        }
    }
}
