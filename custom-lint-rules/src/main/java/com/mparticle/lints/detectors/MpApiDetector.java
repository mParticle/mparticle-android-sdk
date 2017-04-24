package com.mparticle.lints.detectors;

import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MpApiDetector extends Detector implements Detector.JavaPsiScanner {
    private PsiMethodCallExpression properMethodCall;
    private List<PsiMethodCallExpression> properMethodCalls;
    private List<PsiMethodCallExpression> reportedDuplicateMethodCalls;
    private boolean hasApplicationOnCreateMethod;
    public static final String MESSAGE_MULTIPLE_START_CALLS = "Duplicate call to MParticle.start";
    public static final String MESSAGE_NO_START_CALL_IN_ON_CREATE = "This Method should call MParticle.start()";
    public static final String MESSAGE_START_CALLED_IN_WRONG_PLACE = "MParticle.start() should be called in Application.onCreate(), not here";
    public static final String MESSAGE_NO_START_CALL_AT_ALL = "In order to Initialize MParticle, you need to extend android.app.Application, and call MParticle.start() in it's onCreate() method";

    public static final Issue ISSUE = Issue.create(
            "MParticleInitialization",
            "MParticle is being started improperly",
            "MParticle.start() is not called on mParticle in onCreate method of Application",
            Category.MESSAGES,
            10,
            Severity.WARNING,
            new Implementation(MpApiDetector.class, Scope.JAVA_FILE_SCOPE));

    private static final String TARGET_METHOD_QUALIFIED_NAME = "com.mparticle.MParticle.start";
    private static final String TARGET_METHOD_NAME = "start";
    private static final String TARGET_ORIGINATION_METHOD_QUALIFIED_NAME = "android.app.Application.onCreate";
    private static final String TARGET_ORIGINATION_METHOD_NAME = "onCreate";

    // the deeps level of the AST we want to search to.
    private static final int MAX_AST_DEPTH = 4;

    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        return Collections.singletonList(PsiMethod.class);
    }

    public JavaElementVisitor createPsiVisitor(final JavaContext context) {
        return new JavaElementVisitor() {
            public void visitMethod(PsiMethod method) {
                visitMethodDefinition(context, method);
            }
        };
    }

    private void visitMethodCall(JavaContext context, PsiMethodCallExpression call, PsiMethod method) {
        String fullMethodName = call.getMethodExpression().getQualifiedName();
        //test if the "start(~)" call is a call to the MParticle.start(Context) method
        if (TARGET_METHOD_QUALIFIED_NAME.equals(fullMethodName) || "MParticle.start".equals(fullMethodName)) {
            //we have found the correct method call
            context.report(ISSUE, context.getLocation(call), MESSAGE_START_CALLED_IN_WRONG_PLACE);
        }
    }

    private void visitMethodDefinition(JavaContext context, PsiMethod method) {
        if (method == null) { return; }
        //make sure we are not looking in the onCreate() method of a subclass of android.app.Application,
        // and not the actual android.app.Application class ..sometimes the search will take you into
        // the sdk's Application class, particularly in testing
        if (TARGET_ORIGINATION_METHOD_NAME.equals(method.getName()) && isApplicationClassCall(context, method) && !method.getContainingClass().getQualifiedName().contains("android.app.Application")) {
            hasApplicationOnCreateMethod = true;
                properMethodCalls = findMethodCalledFrom(TARGET_METHOD_QUALIFIED_NAME, method, MAX_AST_DEPTH);
                if (properMethodCalls.size() >= 1) {
                    if (properMethodCall == null) {
                        properMethodCall = properMethodCalls.get(0);
                    }
                    for (int i = 1; i < properMethodCalls.size(); i++) {
                        PsiMethodCallExpression call = properMethodCalls.get(i);
                        if (!reportedDuplicateMethodCalls.contains(call)) {
                            context.report(ISSUE, context.getLocation(call), MESSAGE_MULTIPLE_START_CALLS);
                            reportedDuplicateMethodCalls.add(call);
                        }
                    }
                } else {
                    context.report(ISSUE, context.getNameLocation(method), MESSAGE_NO_START_CALL_IN_ON_CREATE);
                }
        }
        if (method.getBody() != null) {
            for (PsiStatement statement : method.getBody().getStatements()) {
                PsiMethodCallExpression methodCall = findMethodCall(statement);
                if (methodCall != null && !properMethodCalls.contains(methodCall)) {
                    visitMethodCall(context, methodCall, method);
                }
            }
        }
    }

    @Override
    public void beforeCheckProject(Context context) {
        super.beforeCheckProject(context);
        hasApplicationOnCreateMethod = false;
        properMethodCalls = new ArrayList<>();
        reportedDuplicateMethodCalls = new ArrayList<>();
    }

    @Override
    public void afterCheckProject(Context context) {
        if (!hasApplicationOnCreateMethod) {
            context.report(ISSUE, Location.create(context.file), MESSAGE_NO_START_CALL_AT_ALL);
        }
    }

    private boolean isApplicationClassCall(JavaContext context, PsiElement call) {
        JavaEvaluator evaluator = context.getEvaluator();
        PsiClass cls = PsiTreeUtil.getParentOfType(call, PsiClass.class, true);
        return cls != null && evaluator.extendsClass(cls, "android.app.Application", false);
    }

    private List<PsiMethodCallExpression> findMethodCalledFrom(String qualifiedTargetMethodName, PsiMethod method, int maxDepth) {
        return findMethodCalledFrom(qualifiedTargetMethodName, method, maxDepth, new ArrayList<>());
    }

    //DFS for the target method
    private List<PsiMethodCallExpression> findMethodCalledFrom(String qualifiedTargetMethodName, PsiMethod method, int maxDepth, List<PsiMethodCallExpression> foundMethodCalls) {
        if (maxDepth <= 0) {
            return foundMethodCalls;
        }
        if (qualifiedTargetMethodName == null || qualifiedTargetMethodName.equals("")) {
            return null;
        }
        if (method == null) {
            return null;
        }

        PsiCodeBlock body = method.getBody();
        if (body == null) {
            return foundMethodCalls;
        }
        for (PsiElement element : body.getChildren()) {
            PsiMethodCallExpression methodFound = findMethodCall(element);

            //check the method we found, if it passes, return true, else find the file it is declared in,
            //find the PsiMethod object in that file, and add it to the list to check!
            if (methodFound == null) {
                continue;
            }
            //TODO
            // pull Arguments (parameters) and check to make sure we have the correct method, rather than an overloaded alternative
            String foundMethodName = methodFound.getMethodExpression().getQualifiedName();
            if (qualifiedTargetMethodName.endsWith(foundMethodName)) {
                foundMethodCalls.add(methodFound);
            } else {
                //chase the method down and see if the target method is called in that method body
                PsiMethod psiMethod = methodFound.resolveMethod();
                PsiClass clazz = psiMethod.getContainingClass();
                for (PsiMethod classMethod : clazz.getMethods()) {
                    if (foundMethodName.endsWith(classMethod.getName())) {
                        foundMethodCalls.addAll(findMethodCalledFrom(qualifiedTargetMethodName, classMethod, maxDepth - 1));
                    }
                }
            }
        }
        return foundMethodCalls;
    }

    private PsiMethodCallExpression findMethodCall(PsiElement element) {
        // This covers the case if there is a method being used to initialize a variable..
        // i.e int a = random();
        if (element instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) element;
            for (PsiElement declarationElement : declarationStatement.getDeclaredElements()) {
                if (declarationElement instanceof PsiVariable) {
                    PsiVariable variable = (PsiVariable) declarationElement;
                    PsiExpression initializer = variable.getInitializer();
                    if (initializer instanceof PsiMethodCallExpression) {
                        return (PsiMethodCallExpression) initializer;
                    }
                }
            }
        }
        if (element instanceof PsiExpressionStatement) {
            PsiExpression expression = ((PsiExpressionStatement) element).getExpression();
            if (expression instanceof PsiMethodCallExpression) {
                return (PsiMethodCallExpression) expression;
            }
        }
        return null;
    }
}
