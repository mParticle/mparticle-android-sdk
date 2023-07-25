package com.mparticle.lints

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiVariable
import com.mparticle.lints.dtos.Expression
import com.mparticle.lints.dtos.MethodCall
import com.mparticle.lints.dtos.RootParent
import com.mparticle.lints.dtos.Value
import org.jetbrains.uast.UArrayAccessExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.getOutermostQualified
import org.jetbrains.uast.getQualifiedChain
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * class that will gather all {@link com.mparticle.ling.dtos.Call}s made to the provided PsiVariable instance.
 * After
 */
internal class VariableCollector(
    val variable: PsiVariable,
    private val method: UMethod,
    val parent: Expression,
    val includeInitialization: Boolean = false
) : AbstractUastVisitor() {
    private var expression: Expression? = null

    fun getUnresolvedObject(returnValue: Boolean? = null): Expression? {
        method.accept(this)
        return expression?.apply {
            if (this is MethodCall && returnValue != null) {
                this.returnValue = returnValue
            }
        }
    }

    override fun visitArrayAccessExpression(node: UArrayAccessExpression) = visitExpression(node)

    override fun visitCallExpression(node: UCallExpression) = visitExpression(node)

    override fun visitVariable(node: UVariable): Boolean {
        if (node is ULocalVariable) {
            if (variable.name == node.name && variable.type == node.type) {
                expression = null
                val uastInitializer = node.uastInitializer

                // check target variable initialization
                if (includeInitialization) {
                    val value = uastInitializer?.evaluate()
                    if (value != null) {
                        expression = Value(parent, value, uastInitializer)
                    }
                    when (uastInitializer) {
                        // product = productTemp
                        is USimpleNameReferenceExpression -> {
                            val reference = uastInitializer.resolve()
                            if (reference is PsiVariable) {
                                expression = VariableCollector(
                                    reference,
                                    method,
                                    parent,
                                    true
                                ).getUnresolvedObject(true)
                            }
                        }

                        is UQualifiedReferenceExpression -> {
                            // get the first call in a potential chain of calls
                            val reference = uastInitializer.getOutermostQualified()
                                .getQualifiedChain()[0].tryResolve()
                            when (reference) {
                                // product = productBuilder.build()
                                is PsiVariable -> {
                                    expression = VariableCollector(
                                        reference,
                                        method,
                                        parent,
                                        true
                                    ).getUnresolvedObject(true)
                                }
                                // product = Product.Builder()   (static method)
                                is PsiClass -> {
                                    val selector = uastInitializer.selector
                                    if (selector is UCallExpression) {
                                        expression = selector.resolveChainedCalls(true, parent)
                                    }
                                }
                            }
                        }
                        // product = new Product()
                        // map = mapOf()
                        is UCallExpression -> {
                            expression = uastInitializer.resolveChainedCalls(
                                true,
                                RootParent(uastInitializer)
                            )
                        }
                    }
                }
            }
        }
        return super.visitVariable(node)
    }

    private fun visitExpression(node: UCallExpression): Boolean {
        if (node.kind === UastCallKind.METHOD_CALL) {
            val receiver = node.receiver
            when (receiver) {
                is USimpleNameReferenceExpression -> {
                    if (receiver.resolvedName == variable.name && expression != null) {
                        expression = node.resolveChainedCalls(false, expression!!)
                    }
                }
                is UQualifiedReferenceExpression -> {
                }
            }
        }
        return super.visitCallExpression(node)
    }
}
