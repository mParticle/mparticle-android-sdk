package com.mparticle.lints;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.mparticle.lints.detectors.MpApiDetector;

import org.intellij.lang.annotations.Language;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class MpApiDetectorTest extends LintDetectorTest {
    private static final String NO_WARNINGS = "No warnings.";

    @Override
    protected Detector getDetector() {
        return new MpApiDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(MpApiDetector.ISSUE);
    }

    @Test
    public void testStartCalledProperly() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import android.util.Log;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onCreate() {\n" +
                        "       super.onCreate();\n" +
                        "       MParticle.start();\n" +
                        "   }\n" +
                        "}";
        assertEquals(lintProject(java(source), mParticleStub, mApplicationStub), NO_WARNINGS);
    }

    @Test
    public void testStartCalledProperlyNested() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import android.util.Log;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onCreate() {\n" +
                        "       super.onCreate();\n" +
                        "       int a = random(4);\n" +
                        "       init();\n" +
                        "    }\n" +
                        "    public void init() {\n" +
                        "       MParticle.start();\n" +
                        "    }\n" +
                        "    public int random(int number) {\n" +
                        "       return 5 + number;\n" +
                        "    }\n" +
                        "    public void anotherMethod() {}\n" +
                        "}";
        assertEquals(NO_WARNINGS, lintProject(java(source), mParticleStub, mApplicationStub));
    }

    @Test
    public void testStartCalledProperlyDeepNested() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import android.util.Log;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onCreate() {\n" +
                        "       super.onCreate();\n" +
                        "       int a = random(4);\n" +
                        "       init();\n" +
                        "    }\n" +
                        "    public void init() {\n" +
                        "       init1();" +
                        "       init2();" +
                        "    }\n" +
                        "    public void init1() {\n" +
                        "       int b = random(4);" +
                        "    }\n" +
                        "    public void init2() {\n" +
                        "       init3();" +
                        "    }\n" +
                        "    public void init3() {\n" +
                        "       MParticle.start();" +
                        "    }\n" +
                        "    public int random(int number) {\n" +
                        "       return 5 + number;\n" +
                        "    }\n" +
                        "    public void anotherMethod() {}\n" +
                        "}";
        assertEquals(NO_WARNINGS, lintProject(java(source), mParticleStub, mApplicationStub));
    }

    /**
     * We want to make sure that we are have a limit on how deep down the AST we will look for the start
     * call, otherwise, we might have cases where this method could cause a hang if the AST has backlinks.
     *
     * currently, this test that we will not try to go beyond 4 levels of nested calls. This test has
     * MParticle.start() in the 5th level, and should fail, as "Not Found".
     * @throws Exception
     */
    @Test
    public void testStartToDeeplyNestedToBeFound() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import android.util.Log;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onCreate() {\n" +
                        "       super.onCreate();\n" +
                        "       int a = random(4);\n" +
                        "       init();\n" +
                        "    }\n" +
                        "    public void init() {\n" +
                        "       init1();" +
                        "       init2();" +
                        "    }\n" +
                        "    public void init1() {\n" +
                        "       int b = random(4);" +
                        "    }\n" +
                        "    public void init2() {\n" +
                        "       init3();" +
                        "    }\n" +
                        "    public void init3() {\n" +
                        "       init4();" +
                        "    }\n" +
                        "    public void init4() {\n" +
                        "       MParticle.start();" +
                        "    }\n" +
                        "    public int random(int number) {\n" +
                        "       return 5 + number;\n" +
                        "    }\n" +
                        "    public void anotherMethod() {}\n" +
                        "}";
        assertThat(lintProject(java(source), mParticleStub, mApplicationStub))
                .contains(MpApiDetector.MESSAGE_START_CALLED_IN_WRONG_PLACE)
                .contains(MpApiDetector.MESSAGE_NO_START_CALL_IN_ON_CREATE)
                .contains(Constants.getErrorWarningMessageString(0,2));
    }

    @Test
    public void testStartCalledNestedMultipleCalls() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import android.util.Log;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onCreate() {\n" +
                        "       super.onCreate();\n" +
                        "       int a = random(4);\n" +
                        "       init();\n" +
                        "    }\n" +
                        "    public void init() {\n" +
                        "       MParticle.start();\n" +
                        "    }\n" +
                        "    public int random(int number) {\n" +
                        "       return 5 + number;\n" +
                        "    }\n" +
                        "    public void anotherMethod() {\n" +
                        "       MParticle.start();\n" +
                        "    }\n" +
                        "}";
        assertThat(lintProject(java(source), mParticleStub, mApplicationStub))
                .contains(MpApiDetector.MESSAGE_START_CALLED_IN_WRONG_PLACE)
                .contains(Constants.getErrorWarningMessageString(0,1));
    }

    @Test
    public void testStartNotCalled() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import android.util.Log;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "}";
        assertThat(lintProject(java(source), mParticleStub, mApplicationStub))
                .contains(MpApiDetector.MESSAGE_NO_START_CALL_AT_ALL)
                .contains(Constants.getErrorWarningMessageString(0,1));
    }

    @Test
    public void testStartCalledMultipleTimes() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onResume() {\n" +
                        "       super.onResume();\n" +
                        "   }\n" +
                        "    public void onCreate() {\n" +
                        "       super.onCreate();\n" +
                        "       MParticle.start();\n" +
                        "       MParticle.start();\n" +
                        "   }\n" +
                        "}";
        assertThat(lintProject(java(source), mParticleStub, mApplicationStub))
                .contains(MpApiDetector.MESSAGE_MULTIPLE_START_CALLS)
                .contains(Constants.getErrorWarningMessageString(0,1));
    }


    /**
     * This test guarantees that we are searching for start() calls in the correct order within onCreate(),
     * which should the order in which they will be executed. To attain this we need to keep a DFS.
     * This does not account for multiple children of android.app.Application and chained constructors.
     * In that case, order cannot be guaranteed.
     * @throws Exception
     */
    @Test
    public void testStartCallFoundInProperOrder() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import android.util.Log;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onCreate() {\n" +
                        "       super.onCreate();\n" +
                        "       int a = random(4);\n" +
                        "       init();\n" +
                        "       MParticle.start();\n" +
                        "    }\n" +
                        "    public void init() {\n" +
                        "       init1();" +
                        "       init2();" +
                        "    }\n" +
                        "    public void init1() {\n" +
                        "       int b = random(4);" +
                        "    }\n" +
                        "    public void init2() {\n" +
                        "       init3();" +
                        "    }\n" +
                        "    public void init3() {\n" +
                        "       MParticle.start();" +
                        "    }\n" +
                        "    public int random(int number) {\n" +
                        "       return 5 + number;\n" +
                        "    }\n" +
                        "    public void anotherMethod() {}\n" +
                        "}";
        assertThat(lintProject(java(source), mParticleStub, mApplicationStub))
                .contains(MpApiDetector.MESSAGE_MULTIPLE_START_CALLS)
                .contains("HasProperCall.java:10")
                .contains(Constants.getErrorWarningMessageString(0,1));
    }


    @Test
    public void testStartErrorCombos() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onResume() {\n" +
                        "       super.onResume();\n" +
                        "       MParticle.start();\n" +
                        "   }\n" +
                        "    public void onCreate() {\n" +
                        "       super.onCreate();\n" +
                        "       MParticle.start();\n" +
                        "   }\n" +
                        "}";
        @Language("JAVA") String source2 =
                "package com.mparticle.lints;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "public class ClassTwo {\n" +
                        "   public void methodName() {\n" +
                        "       MParticle.start();\n" +
                        "   }\n" +
                        "}";
        assertThat(lintProject(java(source), java(source2), mParticleStub, mApplicationStub))
                .contains(MpApiDetector.MESSAGE_START_CALLED_IN_WRONG_PLACE)
                .contains(Constants.getErrorWarningMessageString(0,2));
    }

    @Test
    public void testCalledOutsideApplicationClass() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "public class ClassTwo {\n" +
                        "   public void methodName() {\n" +
                        "       MParticle.start();\n" +
                        "   }\n" +
                        "}";
        assertThat(lintProject(java(source), mParticleStub, mApplicationStub))
                .contains(MpApiDetector.MESSAGE_START_CALLED_IN_WRONG_PLACE)
                .contains(MpApiDetector.MESSAGE_NO_START_CALL_AT_ALL)
                .contains(Constants.getErrorWarningMessageString(0,2));
    }

    @Test
    public void testStartCalledInApplicationOutsideOnCreate() throws Exception {
        @Language("JAVA") String source =
                "package com.mparticle.lints;\n" +
                        "import android.util.Log;\n" +
                        "import com.mparticle.MParticle;\n" +
                        "import android.app.Application;\n" +
                        "public class HasProperCall extends Application {\n" +
                        "    public void onResume() {\n" +
                        "       super.start();\n" +
                        "       MParticle.start();\n" +
                        "   }\n" +
                        "}";
        assertThat(lintProject(java(source), mParticleStub, mApplicationStub))
                .contains(MpApiDetector.MESSAGE_START_CALLED_IN_WRONG_PLACE)
                .contains(MpApiDetector.MESSAGE_NO_START_CALL_AT_ALL)
                .contains(Constants.getErrorWarningMessageString(0,2));
    }

    private TestFile mParticleStub = java("" +
            "package com.mparticle;\n" +
            "public class MParticle {\n" +
            "   public static void start() {}\n" +
            "}");

    private TestFile mApplicationStub = java("" +
            "package android.app; \n" +
            "public class Application {\n" +
            "   public void onCreate() {}\n" +
            "   public void onResume() {}\n" +
            "}");

    private TestFile mInitializerClass = java("" +
            "package com.mparticle.lints;\n" +
            "import com.mparticle.MParticle;\n" +
            "public class Initializer {\n" +
            "   public void init() {\n" +
            "       MParticle.start();\n" +
            "   }\n" +
            "}");

    private TestFile mInitializerSubClass = java("" +
            "package com.mparticle.lints;\n" +
            "import com.mparticle.MParticle;\n" +
            "public class InitializerChild extends Initializer {\n" +
            "}");

    @Override
    protected boolean allowCompilationErrors() {
        return true;
    }

}
