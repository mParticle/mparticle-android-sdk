package com.mparticle.lints.basedetectors

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.JavaContext
import com.mparticle.lints.VariableCollector
import com.mparticle.lints.dtos.Expression
import com.mparticle.lints.dtos.RootParent
import com.mparticle.lints.getVariableElement
import com.mparticle.lints.receiverClassName
import com.mparticle.lints.resolveChainedCalls
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.util.isConstructorCall

/**
 * Visit all instances of the classes denoted in the {@link com.mparticle.lints.treetraversal.CallScanner#getApplicableClasses()}
 * implementation. To visit instances, implement the {@link com.mparticle.lints.treetraversal.CallScanner#onInstanceCollected()}
 * callback, and you will have access to the {@link com.mparticle.lints.dtos.UnresolveObject} describing the instance.
 *
 * @see com.mparticle.lints.dtos.UnresolvedObject
 */
abstract class CallScanner : BaseDetector(), Detector.UastScanner {

    abstract fun onInstanceCollected(
        context: JavaContext,
        unresolvedExpression: Expression,
        reportingNode: UExpression
    )

    abstract fun getApplicableClasses(): List<Class<*>>

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                try {
                    if (!disabled && ofInterest(node)) {
                        var expression = node.resolveChainedCalls(true, RootParent(node))

                        val variable = node.getVariableElement(true, true)
                        val method = node.getParentOfType<UMethod>(UMethod::class.java)

                        if (variable != null && method != null) {
                            VariableCollector(variable, method, expression).getUnresolvedObject(
                                false
                            )
                        }
                        if (expression != null) {
                            node.receiverClassName()?.let { receiverName ->
                                onInstanceCollected(context, expression, node)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (config?.verbose == true) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UCallExpression::class.java)
    }

    private fun ofInterest(node: UCallExpression): Boolean {
        val receiverClassName = node.receiverClassName() ?: return false

        return node.isConstructorCall() && getApplicableClasses().any {
            it.canonicalName == receiverClassName
        }
    }
}
