package com.mparticle.lints.dtos

import com.intellij.psi.impl.compiled.ClsClassImpl
import com.mparticle.lints.resolve
import org.jetbrains.uast.UCallExpression
import java.lang.reflect.Method

class StaticFactory(val methodName: String?, override val node: UCallExpression) :
    ParameterizedExpression {
    override val parent = RootParent(node)
    override var arguments: List<Value> = listOf()

    override fun resolve(): Any? {
        val qualifiedClassName = (node.resolve()?.parent as? ClsClassImpl)?.stub?.qualifiedName
        val methods = HashSet<Method>()
        val clazz = Class.forName(qualifiedClassName)
        methods.addAll(clazz.declaredMethods)
        var matchingMethods = methods
            .filter { it.name == methodName }
            .filter { it.parameterCount == arguments.size }
        if (matchingMethods.size == 1) {
            val method = matchingMethods.first {
                it.parameterTypes.forEachIndexed { i, type ->
                    val value = if (arguments.size > i) arguments.get(i).value else null
                    if (type.name != "null" && value != null && type.name != value::class.java.name) {
                        false
                    }
                }
                true
            }
            val arguments = arguments.resolve()
            method.isAccessible = true
            return method.invoke(null, *arguments.toTypedArray())
        }
        return null
    }

    override fun forEachExpression(predicate: (Expression) -> Unit) {
        predicate(this)
        arguments.forEach { it.forEachExpression(predicate) }
    }
}
