package com.mparticle.lints.dtos

/**
 * The Data Plan detector needs to compute the message body of an event builder chain found in
 * analyzed source, which requires resolving constructor/static calls via reflection. Reflection
 * must never run against a class named by the analyzed source itself unless it's one of the
 * mParticle DTO/builder types (or a small set of collection helpers commonly used inline to build
 * attribute maps) - anything else is attacker-controlled input, not code we intend to execute.
 */
internal object AllowedTypes {
    private val ALLOWED_CLASS_NAMES =
        setOf(
            "com.mparticle.MPEvent",
            "com.mparticle.MPEvent\$Builder",
            "com.mparticle.commerce.CommerceEvent",
            "com.mparticle.commerce.CommerceEvent\$Builder",
            "com.mparticle.commerce.Product",
            "com.mparticle.commerce.Product\$Builder",
            "com.mparticle.commerce.Promotion",
            "com.mparticle.commerce.Impression",
            "com.mparticle.commerce.TransactionAttributes",
            "com.mparticle.MParticle\$EventType",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.ArrayList",
            "kotlin.collections.MapsKt",
            "kotlin.collections.CollectionsKt",
        )

    fun isAllowed(qualifiedClassName: String?): Boolean = qualifiedClassName != null && ALLOWED_CLASS_NAMES.contains(qualifiedClassName)
}
