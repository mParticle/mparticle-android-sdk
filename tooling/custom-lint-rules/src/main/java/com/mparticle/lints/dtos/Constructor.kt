package com.mparticle.lints.dtos

import com.mparticle.lints.receiverClassName
import com.mparticle.lints.resolve
import org.jetbrains.uast.UCallExpression

data class Constructor(override val parent: Expression, val methodName: String?, override val node: UCallExpression) : ParameterizedExpression {
    override var arguments: List<Value> = listOf()

    override fun resolve(): Any? = ResolutionGuard.guarded {
        val qualifiedClassName =
            node.receiverClassName()?.replace(".Builder", "\$Builder")
        if (!AllowedTypes.isAllowed(qualifiedClassName)) {
            return@guarded null
        }
        val clazz = Class.forName(qualifiedClassName)
        val params: List<Any?> = arguments.resolve()
        val argumentClasses =
            params.map {
                if (it != null) {
                    it::class.java
                } else {
                    Nothing::class.java
                }
            }
        val constructor =
            try {
                clazz.getConstructor(*argumentClasses.toTypedArray())
            } catch (ex: Exception) {
                clazz.constructors.firstOrNull { it.parameterTypes.size == argumentClasses.size }
            }
        try {
            if (constructor != null) {
                if (params.size > 0) {
                    return@guarded constructor.newInstance(*params.toTypedArray())
                } else {
                    return@guarded constructor.newInstance()
                }
            }
        } catch (ex: Exception) {
            // fall through - no instance could be constructed
        }
        return@guarded null
    }

    override fun forEachExpression(predicate: (Expression) -> Unit) {
        predicate(this)
        arguments.forEach { it.forEachExpression(predicate) }
    }
}
