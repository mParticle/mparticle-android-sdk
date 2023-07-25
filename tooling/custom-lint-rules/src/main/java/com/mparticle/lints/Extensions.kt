package com.mparticle.lints

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiImmediateClassType
import com.mparticle.lints.dtos.Constructor
import com.mparticle.lints.dtos.Expression
import com.mparticle.lints.dtos.MethodCall
import com.mparticle.lints.dtos.RootParent
import com.mparticle.lints.dtos.StaticFactory
import com.mparticle.lints.dtos.Value
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UDeclarationsExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UExpressionList
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getOutermostQualified
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.getQualifiedChain
import org.jetbrains.uast.getQualifiedParentOrThis
import org.jetbrains.uast.skipParenthesizedExprUp
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.util.isAssignment
import org.jetbrains.uast.util.isConstructorCall
import org.json.JSONArray
import org.json.JSONObject

internal fun UCallExpression.resolveExpression(
    instance: Expression,
    returnValue: Boolean
): Expression {
    val expression = if (isConstructorCall()) {
        Constructor(instance, methodName, this)
    } else {
        if (instance is RootParent) {
            StaticFactory(methodName, this)
        } else {
            MethodCall(instance, methodName, this, returnValue)
        }
    }
    expression.arguments = valueArguments.map { it.resolveValue(expression) }
    return expression
}

internal fun UExpression.resolveValue(initializer: Expression): Value {
    val eval = evaluate()
    if (eval != null) {
        return Value(initializer, eval, this)
    }
    if (this is UReferenceExpression) {
        val reference = resolve()
        if (reference is PsiEnumConstant) {
            (this as? UQualifiedReferenceExpression)?.run {
                val className = (receiver.tryResolve() as? PsiClass)?.getQualifiedName(true)
                val enumName = (selector as? UReferenceExpression)?.resolvedName
                if (className != null && enumName != null) {
                    return Value(initializer, Pair(className, enumName), this)
                }
            }
        }
        if (reference is PsiVariable) {
            val method = getParentOfType(UMethod::class.java)
            if (method != null) {
                val collectorVisitor = VariableCollector(reference, method, initializer, true)
                val obj = collectorVisitor.getUnresolvedObject()
                return Value(initializer, obj, this)
            }
        }
    }
    return Value(initializer, null, this)
}

internal fun PsiVariable.getClassName(): String? {
    var name = type.getClassName()
    if (name != null && name.contains("<")) {
        name = name.substring(0, name.indexOf("<"))
    }
    return name
}

internal fun UExpression.getUltimateReceiverVariable(): PsiVariable? {
    val receiver = when (this) {
        is UCallExpression -> receiver
        is UQualifiedReferenceExpression -> receiver
        else -> null
    }
    if (receiver == null) {
        return null
    }

    return when (receiver.tryResolve()) {
        is PsiVariable -> return receiver.tryResolve() as PsiVariable
        else -> receiver.getUltimateReceiverVariable()
    }
}

internal fun UExpression.getVariableElement(
    allowChainedCalls: Boolean,
    allowFields: Boolean
): PsiVariable? {
    var parent = skipParenthesizedExprUp(getQualifiedParentOrThis().uastParent)

    // Handle some types of chained calls; e.g. you might have
    //    var = prefs.edit().put(key,value)
    // and here we want to skip past the put call
    if (allowChainedCalls) {
        while (true) {
            if (parent is UQualifiedReferenceExpression) {
                val parentParent = skipParenthesizedExprUp(parent.uastParent)
                if (parentParent is UQualifiedReferenceExpression) {
                    parent = skipParenthesizedExprUp(parentParent.uastParent)
                } else if (parentParent is UVariable || parentParent is UPolyadicExpression) {
                    parent = parentParent
                    break
                } else {
                    break
                }
            } else {
                break
            }
        }
    }

    if (parent != null && parent.isAssignment()) {
        val assignment = parent as UBinaryExpression
        val lhs = assignment.leftOperand
        if (lhs is UReferenceExpression) {
            val resolved = lhs.resolve()
            if (resolved is PsiVariable && (allowFields || resolved !is PsiField)) {
                // e.g. local variable, parameter - but not a field
                return resolved
            }
        }
    } else if (parent is UVariable && (allowFields || parent !is UField)) {
        // Handle elvis operators in Kotlin. A statement like this:
        //   val transaction = f.beginTransaction() ?: return
        // is turned into
        //   var transaction: android.app.FragmentTransaction = elvis {
        //       @org.jetbrains.annotations.NotNull var var8633f9d5: android.app.FragmentTransaction = f.beginTransaction()
        //       if (var8633f9d5 != null) var8633f9d5 else return
        //   }
        // and here we want to record "transaction", not "var8633f9d5", as the variable
        // to track.
        if (parent.uastParent is UDeclarationsExpression &&
            parent.uastParent!!.uastParent is UExpressionList
        ) {
            val exp = parent.uastParent!!.uastParent as UExpressionList
            val kind = exp.kind
            if (kind.name == "elvis" && exp.uastParent is UVariable) {
                parent = exp.uastParent
            }
        }

        return (parent as UVariable).psi
    }
    return null
}

internal fun UCallExpression.receiverClassName(stripGenerics: Boolean = true): String? {
    var className =
        when {
            isConstructorCall() -> returnType?.getClassName()
            receiver == null -> {
                (this.resolve() as? ClsMethodImpl)?.run {
                    stub.parentStub.psi.containingFile.name.split("__")[0]
                }
            }
            else -> (receiverType as? PsiClassReferenceType)?.reference?.qualifiedName
        }

    fun stripGenerics() {
        val start = className?.indexOf("<") ?: 0
        val end = className?.indexOf(">") ?: 0
        if (start > 0 && end > 0 && end < (className?.length ?: (0 - 2))) {
            className = className?.removeRange(start, end + 1)
        }
        if (className?.contains(">") == true) {
            stripGenerics()
        }
    }
    if (stripGenerics) {
        stripGenerics()
    }
    return className
}

internal fun PsiType.getClassName(): String? {
    return when (this) {
        is PsiClassReferenceType -> this.reference.qualifiedName
        is PsiImmediateClassType -> this.resolve()?.qualifiedName
        else -> null
    }
}

internal fun PsiClass.getQualifiedName(reflectable: Boolean): String? {
    if (!reflectable) {
        return qualifiedName
    }
    var qualifiedName: String = getQualifiedName() ?: return null

    fun isParentTopLevel(child: PsiElement) {
        if (!child.isTopLevelKtOrJavaMember()) {
            val index = qualifiedName.indexOfLast { it == '.' }
            qualifiedName = qualifiedName.substring(0, index) + "$" + qualifiedName.substring(
                index + 1,
                qualifiedName.length
            )
            isParentTopLevel(child.parent)
        }
    }
    isParentTopLevel(this)
    return qualifiedName
}

internal fun UExpression.resolveChainedCalls(
    returnValue: Boolean,
    instance: Expression
): Expression {
    val initialInstance = instance
    var calls = (getOutermostQualified() ?: this).getQualifiedChain().toMutableList()
    calls = calls.filter { it is UCallExpression }.toMutableList()
    // check if first call is a constructor - Product.Builder().brand("") otherwise, the instance
    // argument should be the object the chain is being called on
    return calls.fold(initialInstance) { acc, item ->
        (item as UCallExpression).resolveExpression(acc, true)
    }.apply {
        if (this is MethodCall) {
            this.returnValue = returnValue
        }
    }
}

internal fun Pair<*, *>.resolveToEnum(): Enum<*> {
    val className = when (first) {
        is ClassId -> "${(first as ClassId).packageFqName}.${
        (first as ClassId).relativeClassName.asString().replace(".", "$")
        }"
        is String -> first as String
        else -> null
    }
    return className?.let { className ->
        val enumName = second.toString()
        val constructor = Class.forName(className)
            .methods.first { it.name == "valueOf" }
        constructor.invoke(null, enumName)
    } as Enum<*>
}

internal fun List<Value>.resolve(): List<Any?> {
    return map { it.resolve() }
}

internal fun JSONObject.stringify(): JSONObject {
    val newJSONObject = JSONObject()
    keys().forEach {
        val value = get(it as String)
        when (value) {
            is JSONObject -> newJSONObject.put(it, value.stringify())
            is JSONArray -> newJSONObject.put(it, value.stringify())
            else -> newJSONObject.put(it, value.toString())
        }
    }
    return this
}

internal fun JSONArray.stringify(): JSONArray {
    val newArray = JSONArray()
    for (i in 0..this.length() - 1) {
        val value = get(i)
        when (value) {
            is JSONObject -> newArray.put(value.stringify())
            is JSONArray -> newArray.put(value.stringify())
            else -> newArray.put(value.toString())
        }
    }
    return newArray
}
