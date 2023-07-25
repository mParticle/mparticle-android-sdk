package com.mparticle.lints.dtos

import com.mparticle.lints.resolve
import org.jetbrains.uast.UCallExpression

data class MethodCall(
    override val parent: Expression,
    val methodName: String?,
    override val node: UCallExpression,
    var returnValue: Boolean
) : ParameterizedExpression {

    override var arguments: List<Value> = listOf()

    init {
        arguments.forEach { it.parent = this }
    }

    override fun resolve(): Any? {
        val instance = parent.resolve()
        if (instance == null) {
            return null
        }
        var matchingMethods = instance::class.java.methods
            .filter { it.name == methodName }
            .filter { it.parameterCount == arguments.size }
        if (matchingMethods.size == 1) {
            val method = matchingMethods.first {
                it.parameterTypes.forEachIndexed { i, type ->
                    val value = arguments[i].value
                    if (type.name != "null" && value != null && type.name != value::class.java.name) {
                        false
                    }
                }
                true
            }
            val arguments = arguments.resolve()
            val value = method.invoke(instance, *arguments.toTypedArray())
            if (returnValue) {
                return value
            } else {
                return instance
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        return node.equals((other as? MethodCall)?.node)
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }

    override fun toString(): String {
        return "$methodName (${arguments.joinToString("\n")})"
    }

    override fun forEachExpression(predicate: (Expression) -> Unit) {
        parent.forEachExpression(predicate)
        predicate(this)
        arguments.forEach {
            it.forEachExpression(predicate)
        }
    }
}
