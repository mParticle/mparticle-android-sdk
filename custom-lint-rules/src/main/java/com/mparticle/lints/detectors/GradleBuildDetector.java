package com.mparticle.lints.detectors;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.GradleScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.Pair;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class GradleBuildDetector extends Detector implements GradleScanner {
    public static final String MESSAGE_DONT_MIX_PLUSES = "Additionally, it is highly recommended to avoid mixing explicit version numbers and version numbers containing \'+\'. They may be the same version today, but in the future could become mismatched.";
    public static final String MESSAGE_INCONSISTENCY_IN_VERSIONS_DETECTED = "Inconsistency in version numbers detected across MParticle build.gradle dependencies. packages:";

    private static final String MPARTICLE_PACKAGE = "com.mparticle";
    private List<MPDependency> mMPDependencies;

    public static final Issue ISSUE = Issue.create(
            "MParticle_Dependency_Version_Inconsistency",
            "MParticle module Gradle dependencies should not have different versions.",
            "There are multiple dependencies from 'com.mparticle', and they do not all have the same version code. In order for the MParticle SDK to function properly, all Gradle dependencies must be the same version. \n\n hint: If you are using kits, you do not need to include a dependency for 'com.mparticle:android-core', it will automatically be included with your kit dependency.",
            Category.USABILITY,
            9,
            Severity.ERROR,
            new Implementation(GradleBuildDetector.class, Scope.GRADLE_SCOPE));


    @Override
    public void beforeCheckProject(Context context) {
        mMPDependencies = new ArrayList<>();
    }

    @Override
    public void afterCheckProject(Context context) {
        // Check for inconsistent versions across MParticle dependencies (Core, Kits, etc).
        String currentVersion = null;
        for (MPDependency mpDependency: mMPDependencies) {
            if (currentVersion == null) {
                currentVersion = mpDependency.version;
            } else {
                if (!currentVersion.equals(mpDependency.version)) {
                    reportInconsistency(context);
                    break;
                }
            }
        }
    }

    @Override
    public void visitBuildScript (Context context, Map<String, Object> sharedData) {
        try {
            visitBuildScript(context);
        } catch (Exception e) {
            // do nothing
        }
    }

    private void checkDslPropertyAssignment(Context context, String property, String value, String parent, MethodCallExpression expression) {
        if (isDependencyLine(parent, property)) {
            String[] dependencySplit = value.replace("\'", "").replace("\"", "").split(":");
            if (dependencySplit.length == 3) {
                String name = dependencySplit[0];
                String compare = MPARTICLE_PACKAGE;
                if (compare.equalsIgnoreCase(name)) {
                    mMPDependencies.add(new MPDependency(context, dependencySplit[2], value, expression));
                }
            }
        }
    }

    // This method is meant to be added to, if we would like to investigate more block's child properties
    // in the isInterestingProperty() method.
    private boolean isInterestingBlock(String parent) {
        return "dependencies".equals(parent);
    }

    //This method is meant to be added to, if we want to add more properties we would like to inspect
    // in the checkDslPropertyAssignment() method.
    private boolean isInterestingProperty(String property, String parent) {
        return isDependencyLine(parent, property);
    }

    private boolean isDependencyLine(String parent, String property) {
        return "dependencies".equals(parent) && ("compile".equals(property) || "testCompile".equalsIgnoreCase(property));
    }

    private void reportInconsistency(Context context) {
        Context commonContext = null;
        MPDependency minLineNumber = null;
        MPDependency maxLineNumber = null;
        boolean hasCommonContext = false;


        int nonExplicitVersions = 0;
        StringBuilder message = new StringBuilder();
        message
                .append(MESSAGE_INCONSISTENCY_IN_VERSIONS_DETECTED)
                .append("\n\n");
        for(MPDependency mpDependency: mMPDependencies) {
            if (commonContext == null) {
                commonContext = mpDependency.context;
                hasCommonContext = true;
            } else {
                if (commonContext != mpDependency.context && hasCommonContext) {
                    commonContext = null;
                    hasCommonContext = false;
                }
            }
            if (maxLineNumber == null) {
                maxLineNumber = mpDependency;
            } else if (maxLineNumber.expression.getLastLineNumber() < mpDependency.expression.getLastLineNumber()){
                maxLineNumber = mpDependency;
            }
            if (minLineNumber == null) {
                minLineNumber = mpDependency;
            } else if (minLineNumber.expression.getLineNumber() > mpDependency.expression.getLineNumber()){
                minLineNumber = mpDependency;
            }

            message
                    .append("\t")
                    .append(mpDependency.originalString)
                    .append("\n");
            if (mpDependency.version.contains("+")) {
                nonExplicitVersions++;
            }
        }
        message
                .append("\nmust all have the same version.");

        if (nonExplicitVersions != 0 && nonExplicitVersions < mMPDependencies.size()) {
            message
                    .append(" ")
                    .append(MESSAGE_DONT_MIX_PLUSES);
        }
        if (hasCommonContext) {
            context.report(ISSUE, createLocation(maxLineNumber.context, maxLineNumber.expression.getArguments()), message.toString());
        } else {
            context.report(ISSUE, Location.create(context.file), message.toString());
        }
    }

    private void visitBuildScript(Context context) {
        CharSequence sequence = context.getContents();
        if (sequence != null) {
            final String source = sequence.toString();
            List<ASTNode> astNodes = (new AstBuilder()).buildFromString(source);
            CodeVisitorSupport visitor = new CodeVisitorSupport() {
                @Override
                public void visitMethodCallExpression(MethodCallExpression expression) {
                    super.visitMethodCallExpression(expression);
                    Expression arguments = expression.getArguments();
                    String parent = expression.getMethodAsString();
                    if (arguments instanceof ArgumentListExpression) {
                        ArgumentListExpression ale = (ArgumentListExpression) arguments;
                        List<Expression> expressions = ale.getExpressions();
                        if (expressions.size() == 1 &&
                                expressions.get(0) instanceof ClosureExpression) {
                            if (isInterestingBlock(parent)) {
                                ClosureExpression closureExpression =
                                        (ClosureExpression) expressions.get(0);
                                Statement block = closureExpression.getCode();
                                if (block instanceof BlockStatement) {
                                    BlockStatement bs = (BlockStatement) block;
                                    for (Statement statement : bs.getStatements()) {
                                        if (statement instanceof ExpressionStatement) {
                                            ExpressionStatement e = (ExpressionStatement) statement;
                                            if (e.getExpression() instanceof MethodCallExpression) {
                                                checkDslProperty(parent,
                                                        (MethodCallExpression) e.getExpression());
                                            }
                                        } else if (statement instanceof ReturnStatement) {
                                            ReturnStatement e = (ReturnStatement) statement;
                                            if (e.getExpression() instanceof MethodCallExpression) {
                                                checkDslProperty(parent,
                                                        (MethodCallExpression) e.getExpression());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                private void checkDslProperty(String parent, MethodCallExpression c) {
                    String property = c.getMethodAsString();
                    if (isInterestingProperty(property, parent)) {
                        String value = getText(c.getArguments());
                        checkDslPropertyAssignment(context, property, value, parent, c);
                    }
                }

                private String getText(ASTNode node) {
                    Pair<Integer, Integer> offsets = getOffsets(node, context);
                    return source.substring(offsets.getFirst(), offsets.getSecond());
                }

            };
            for (ASTNode node : astNodes) {
                node.visit(visitor);
            }
        }
    }

    //This method was inspired by the source code for GroovyGradleDetector in package 'com.android.build.gradle.tasks'.
    @NonNull
    private static Pair<Integer, Integer> getOffsets(ASTNode node, Context context) {
        if (node.getLastLineNumber() == -1 && node instanceof TupleExpression) {
            TupleExpression exp = (TupleExpression) node;
            List<Expression> expressions = exp.getExpressions();
            if (!expressions.isEmpty()) {
                return Pair.of(
                        getOffsets(expressions.get(0), context).getFirst(),
                        getOffsets(expressions.get(expressions.size() - 1), context).getSecond());
            }
        }
        CharSequence source = context.getContents();
        assert source != null;
        int start = 0;
        int end = source.length();
        int line = 1;
        int startLine = node.getLineNumber();
        int startColumn = node.getColumnNumber();
        int endLine = node.getLastLineNumber();
        int endColumn = node.getLastColumnNumber();
        int column = 1;
        for (int index = 0, len = end; index < len; index++) {
            if (line == startLine && column == startColumn) {
                start = index;
            }
            if (line == endLine && column == endColumn) {
                end = index;
                break;
            }

            char c = source.charAt(index);
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        return Pair.of(start, end);
    }

    private Location createLocation(@NonNull Context context, @NonNull Object cookie) {
        ASTNode node = (ASTNode) cookie;
        Pair<Integer, Integer> offsets = getOffsets(node, context);
        int fromLine = node.getLineNumber() - 1;
        int fromColumn = node.getColumnNumber() - 1;
        int toLine = node.getLastLineNumber() - 1;
        int toColumn = node.getLastColumnNumber() - 1;
        return Location.create(context.file,
                new DefaultPosition(fromLine, fromColumn, offsets.getFirst()),
                new DefaultPosition(toLine, toColumn, offsets.getSecond()));
    }

    class MPDependency {
        final Context context;
        final String version;
        final String originalString;
        final MethodCallExpression expression;

        MPDependency(Context context, String version, String originalString, MethodCallExpression expression) {
            this.context = context;
            this.version = version;
            this.originalString = originalString;
            this.expression = expression;
        }
    }
}
