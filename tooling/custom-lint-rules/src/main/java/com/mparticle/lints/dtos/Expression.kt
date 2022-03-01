package com.mparticle.lints.dtos

import org.jetbrains.uast.UExpression

interface Expression {
    val node: UExpression
    val parent: Expression
    fun resolve(): Any?
    fun forEachExpression(predicate: (Expression) -> Unit)
}

interface ParameterizedExpression : Expression {
    var arguments: List<Value>
}

class RootParent(override val node: UExpression) : Expression {
    override val parent: Expression = this

    override fun resolve() = null

    override fun forEachExpression(predicate: (Expression) -> Unit) {}
}
