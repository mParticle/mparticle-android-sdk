package com.mparticle.lints.detectors;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.GradleContext;
import com.android.tools.lint.detector.api.GradleScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.Pair;
import com.mparticle.lints.basedetectors.BaseDetector;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;

import java.util.ArrayList;
import java.util.List;


public class GradleBuildDetector extends BaseDetector implements GradleScanner {
    public static final String MESSAGE_DONT_MIX_PLUSES = "Additionally, it is highly recommended to avoid mixing explicit version numbers and version numbers containing \'+\'. They may be the same version today, but in the future could become mismatched";
    public static final String MESSAGE_INCONSISTENCY_IN_VERSIONS_DETECTED = "Inconsistency in version numbers detected across MParticle build.gradle dependencies. packages:";

    private static final String MPARTICLE_PACKAGE = "com.mparticle";
    private List<MPDependency> mMPDependencies;
    private String[] dependencyKeys = new String[]{
            "compile",
            "implementation",
            "api"
    };

    private String[] includedArtifacts = new String[]{
            "android-core",
            "-kit",
    };

    public static final Issue ISSUE = Issue.create(
            "MParticleVersionInconsistency",
            "mParticle module Gradle dependencies should not have different versions",
            "There are multiple dependencies from 'com.mparticle', and they do not all have the same version code. In order for the mParticle SDK to function properly, all Gradle dependencies must be the same version. \n\n hint: If you are using kits, you do not need to include a dependency for 'com.mparticle:android-core', it will automatically be included with your kit dependency",
            Category.USABILITY,
            9,
            Severity.ERROR,
            new Implementation(GradleBuildDetector.class, Scope.GRADLE_SCOPE));


    @Override
    public void beforeCheckRootProject(@NonNull Context context) {
        mMPDependencies = new ArrayList<>();
    }

    @Override
    public void afterCheckRootProject(Context context) {
        // Check for inconsistent versions across MParticle dependencies (Core, Kits, etc)
        String currentVersion = null;
        for (MPDependency mpDependency : mMPDependencies) {
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
    public void checkDslPropertyAssignment(@NonNull GradleContext context, @NonNull String property, @NonNull String value, @NonNull String parent, @Nullable String parentParent, @NonNull Object propertyCookie, @NonNull Object valueCookie, @NonNull Object statementCookie) {
        if (isDependencyLine(parent, property)) {
            String[] dependencySplit = value.replace("\'", "").replace("\"", "").split(":");
            if (dependencySplit.length == 3) {
                String pkg = dependencySplit[0];
                String artifact = dependencySplit[1];
                if (MPARTICLE_PACKAGE.equalsIgnoreCase(pkg) && artifact != null) {
                    boolean isIncludedArtifact = false;
                    for (String includedArtifact : includedArtifacts) {
                        if (artifact.contains(includedArtifact)) {
                            isIncludedArtifact = true;
                        }
                    }
                    if (isIncludedArtifact) {
                        mMPDependencies.add(new MPDependency(context, dependencySplit[2], value, (MethodCallExpression) statementCookie));
                    }
                }
            }
        }
    }

    private boolean isDependencyLine(String parent, String property) {
        if ("dependencies".equals(parent) && property != null) {
            for (String dependencyKey : dependencyKeys) {
                if (property.equals(dependencyKey)) {
                    return true;
                }
            }
        }
        return false;
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
        for (MPDependency mpDependency : mMPDependencies) {
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
            } else if (maxLineNumber.expression.getLastLineNumber() < mpDependency.expression.getLastLineNumber()) {
                maxLineNumber = mpDependency;
            }
            if (minLineNumber == null) {
                minLineNumber = mpDependency;
            } else if (minLineNumber.expression.getLineNumber() > mpDependency.expression.getLineNumber()) {
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

    //This method was inspired by the source code for GroovyGradleDetector in package 'com.android.build.gradle.tasks'
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
