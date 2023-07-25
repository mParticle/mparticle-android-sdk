package com.mparticle.lints.dtos

import com.mparticle.lints.resolveToEnum
import org.jetbrains.uast.UExpression

data class Value(override var parent: Expression, val value: Any?, override val node: UExpression) :
    Expression {

    override fun toString(): String {
        return "$value"
    }

    override fun resolve(): Any? {
        return when (value) {
            is Expression -> {
                value.resolve()
            }
            is Pair<*, *> -> value.resolveToEnum()
            else -> value
        }
    }

    override fun forEachExpression(predicate: (Expression) -> Unit) {
        predicate(this)
        if (value is Expression) {
            value.forEachExpression(predicate)
        }
    }
}
