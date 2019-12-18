package com.mparticle.lints;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.mparticle.lints.detectors.MpApiDetectorKt;

import org.intellij.lang.annotations.Language;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.mparticle.lints.Constants.NO_WARNINGS;
import static org.fest.assertions.api.Assertions.assertThat;

public class MPApiDetectorKotlinTest extends LintDetectorTest {

    @Override
    protected Detector getDetector() {
        return new MpApiDetectorKt();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(MpApiDetectorKt.Companion.getISSUE());
    }

    @Override
    protected boolean allowMissingSdk() {
        return true;
    }

    public void testStartCalledProperly() throws Exception {
        @Language("KT") String source =
                "package com.mparticle.lints\n" +
                        "import android.app.Application\n" +
                        "import com.mparticle.MParticle\n" +
                        "class HasProperCall : Application() {\n" +
                        "   override fun onCreate() {\n" +
                        "       super.onCreate()\n" +
                        "       MParticle.start()\n" +
                        "   }\n" +
                        "}";
        assertEquals(NO_WARNINGS, lintProject(kotlin(source), mParticleStub, mApplicationStub));
    }

    @Test
    public void testStartCalledProperlyNestedInSeperateClassKotlin() throws Exception {
        @Language("KT") String source =
                "package com.mparticle.lints;\n" +
                        "import android.app.Application\n" +
                        "import com.mparticle.MParticle\n" +
                        "\n" +
                        "class HasProperCall : Application() {\n" +
                        "    val initializer = Initializer()\n" +
                        "    \n" +
                        "    override fun onCreate() {\n" +
                        "        super.onCreate()\n" +
                        "        initializer.init()\n" +
                        "    }\n" +
                        "}";
        assertEquals(NO_WARNINGS, lintProject(kotlin(source), mKotlinInitializerStub, mParticleStub, mApplicationStub));
    }

    @Test
    public void testStartCalledProperlyInBlock() throws Exception {
        @Language("KT") String source = "" +
                "package com.mparticle.lints\n" +
                "   import android.app.Application\n" +
                "   import com.mparticle.MParticle\n" +
                "class HasProperCall : Application(){\n" +
                "   override fun onCreate() {\n" +
                "        super.onCreate()\n" +
                "        doNothing()\n.also { \n" +
                "           doSomething()\n" +
                "           MParticle.start() }\n" +
                "    }\n" +
                "    \n" +
                "    fun doNothing() {\n" +
                "        \n" +
                "    }\n" +
                "   fun doSomething() : Int {" +
                "       1;" +
                "   }" +
                "}";
        assertEquals(NO_WARNINGS, lintProject(kotlin(source), mParticleStub, mApplicationStub));
    }

    @Test
    public void testStartCalledProperyInLocalFunction() throws Exception {
        @Language("KT") String source = "" +
                "package com.mparticle.lints\n" +
                "   import android.app.Application\n" +
                "   import com.mparticle.MParticle\n" +
                "class HasProperCall : Application(){\n" +
                "   override fun onCreate() {\n" +
                "        super.onCreate()\n" +
                "        fun inner() {\n"+
                "           MParticle.start()\n" +
                "       }\n" +
                "       inner()\n" +
                "    }\n" +
                "}";
        assertEquals(NO_WARNINGS, lintProject(kotlin(source), mParticleStub, mApplicationStub));
    }

    @Test
    public void testStartCalledProperlyInLocalLambdaFunction() throws Exception {
        @Language("KT") String source = "" +
                "package com.mparticle.lints\n" +
                "   import android.app.Application\n" +
                "   import com.mparticle.MParticle\n" +
                "class HasProperCall : Application(){\n" +
                "   override fun onCreate() {\n" +
                "        super.onCreate()\n" +
                "        fun innerLambda() = MParticle.start()\n" +
                "       innerLambda()\n" +
                "    }\n" +
                "}";
        assertEquals(NO_WARNINGS, lintProject(kotlin(source), mParticleStub, mApplicationStub));
    }

    @Test
    public void testStartNotCalledProperyInLocalFunction() throws Exception {
        @Language("KT") String source = "" +
                "package com.mparticle.lints\n" +
                "   import android.app.Application\n" +
                "   import com.mparticle.MParticle\n" +
                "class HasProperCall : Application(){\n" +
                "   override fun onCreate() {\n" +
                "        super.onCreate()\n" +
                "        var a = onResume()\n" +
                "        fun inner() {\n"+
                "           MParticle.start()\n" +
                "       }\n" +
                "       fun innerLambda() = MParticle.start()\n" +
                "    }\n" +
                "}";
        assertThat(lintProject(kotlin(source), mParticleStub, mApplicationStub))
                .contains(MpApiDetectorKt.Companion.getMESSAGE_NO_START_CALL_IN_ON_CREATE())
                .contains(Constants.getErrorWarningMessageString(0,1));    }

    private TestFile mKotlinInitializerStub = kotlin("" +
            "package com.mparticle.lints;\n" +
            "import com.mparticle.MParticle;\n" +
            "class Initializer {\n" +
            "    fun init() {\n" +
            "        MParticle.start();\n" +
            "    }\n" +
            "}");

    private TestFile mParticleStub = java("" +
            "package com.mparticle;\n" +
            "public class MParticle {\n" +
            "   public static void start() {}\n" +
            "}");

    private TestFile mApplicationStub = kotlin(
            "package android.app\n" +
                    "class Application {\n" +
                    "   fun onCreate() {}\n" +
                    "   fun onResume() {}\n" +
                    "}");
}
