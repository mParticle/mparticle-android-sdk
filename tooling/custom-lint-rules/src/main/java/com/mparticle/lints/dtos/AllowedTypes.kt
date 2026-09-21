package com.mparticle.lints.dtos

/**
 * The Data Plan detector needs to compute the message body of an event builder chain found in
 * analyzed source, which requires resolving constructor/static calls via reflection. Reflection
 * must never run against a class named by the analyzed source itself unless it's one of the
 * mParticle DTO/builder types (or a small set of collection helpers commonly used inline to build
 * attribute maps) - anything else is attacker-controlled input, not code we intend to execute.
 *
 * Every call site that produces or receives a value in the resolution chain must be checked, not
 * just the ones that name a class directly - a value of an allowed type still exposes inherited
 * methods (e.g. Any.getClass()) whose return value (e.g. java.lang.Class) is not itself an allowed
 * type, so a method-invocation gate is what actually stops the chain, not the constructor gate.
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
        )

    // Kotlin top-level functions compile to static methods on a synthetic facade class, but a
    // compiled-PSI reference to one resolves to the *part* class backing that facade (e.g.
    // "kotlin.collections.MapsKt__MapsKt", not "kotlin.collections.MapsKt") - normalized below.
    // Gating specific method names, rather than every declared method of the whole facade, keeps
    // this to exactly the inline map/list literal builders consumer code actually needs.
    private val ALLOWED_STATIC_FACTORY_METHODS: Map<String, Set<String>> =
        mapOf(
            "kotlin.collections.MapsKt" to setOf("mapOf", "mutableMapOf", "hashMapOf", "linkedMapOf"),
            "kotlin.collections.CollectionsKt" to setOf("listOf", "mutableListOf", "arrayListOf"),
        )

    fun isAllowed(qualifiedClassName: String?): Boolean = qualifiedClassName != null && ALLOWED_CLASS_NAMES.contains(qualifiedClassName)

    fun isAllowedStaticFactory(qualifiedClassName: String?, methodName: String?): Boolean {
        if (qualifiedClassName == null || methodName == null) {
            return false
        }
        val facadeName = qualifiedClassName.substringBefore("__")
        return ALLOWED_STATIC_FACTORY_METHODS[facadeName]?.contains(methodName) == true
    }

    // Gates what a MethodCall may be invoked on. Values legitimately produced by allowed
    // constructors/static factories - strings, boxed primitives, maps, lists - are accepted by
    // type rather than by exact runtime class, since their concrete implementation class isn't
    // guaranteed (e.g. what mapOf() returns varies by size and Kotlin version). Anything else
    // (java.lang.Class included) must match the same class-name allowlist used for construction.
    fun isInstanceAllowed(instance: Any?): Boolean = when (instance) {
        null -> false
        is String, is Boolean, is Char,
        is Byte, is Short, is Int, is Long, is Float, is Double,
        is Map<*, *>, is List<*>,
        -> true
        else -> isAllowed(instance::class.java.name)
    }
}
