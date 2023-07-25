package com.mparticle.lints.dtos

import com.mparticle.lints.receiverClassName
import com.mparticle.lints.resolve
import org.jetbrains.uast.UCallExpression

data class Constructor(
    override val parent: Expression,
    val methodName: String?,
    override val node: UCallExpression
) : ParameterizedExpression {
    override var arguments: List<Value> = listOf()

    override fun resolve(): Any? {
        val qualifiedClassName =
            node.receiverClassName()?.replace(".Builder", "\$Builder")
        val clazz = Class.forName(qualifiedClassName)
        val params: List<Any?> = arguments.resolve()
        val argumentClasses = params.map {
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
                clazz.constructors.firstOrNull() { it.parameterTypes.size == argumentClasses.size }
            }
        try {
            if (constructor != null) {
                if (params.size > 0) {
                    return constructor.newInstance(*params.toTypedArray())
                } else {
                    return constructor.newInstance()
                }
            }
        } catch (ex: Exception) {
            "no new Instance for $clazz.name, tried constructor: ${constructor?.name}"
        }
        return clazz
    }

    override fun forEachExpression(predicate: (Expression) -> Unit) {
        predicate(this)
        arguments.forEach { it.forEachExpression(predicate) }
    }
}
