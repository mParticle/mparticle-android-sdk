package com.mparticle.lints

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.mparticle.lints.Constants.mApplicationStubClass
import com.mparticle.lints.Constants.mParticleStubClass
import com.mparticle.lints.detectors.MpApiDetectorKt
import com.mparticle.lints.detectors.MpApiDetectorKt.Companion.ISSUE
import com.mparticle.lints.detectors.MpApiDetectorKt.Companion.MESSAGE_MULTIPLE_START_CALLS
import com.mparticle.lints.detectors.MpApiDetectorKt.Companion.MESSAGE_NO_START_CALL_AT_ALL
import com.mparticle.lints.detectors.MpApiDetectorKt.Companion.MESSAGE_START_CALLED_IN_WRONG_PLACE
import org.intellij.lang.annotations.Language
import org.junit.Test

class MpApiDetectorJavaTest : LintDetectorTest() {
    override fun getDetector(): Detector {
        return MpApiDetectorKt()
    }

    override fun getIssues(): List<Issue> {
        return listOf(ISSUE)
    }

    override fun allowMissingSdk(): Boolean {
        return true
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperly() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onCreate() {
       super.onCreate();
       MParticle.start();
   }
}"""
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperlyNested() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onCreate() {
       super.onCreate();
       int a = random(4);
       init();
    }
    public void init() {
       MParticle.start();
    }
    public int random(int number) {
       return 5 + number;
    }
    public void anotherMethod() {}
}"""
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .skipTestModes(TestMode.PARENTHESIZED)
            .run()
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperlyDeepNested() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onCreate() {
       super.onCreate();
       int a = random(4);
       init();
    }
    public void init() {
       init1();       
       init2();    
       }
    public void init1() {
       int b = random(4);    
    }
    public void init2() {
       init3();    
    }
    public void init3() {
       MParticle.start();    
    }
    public int random(int number) {
       return 5 + number;
    }
    public void anotherMethod() {}
}
"""
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .skipTestModes(TestMode.PARENTHESIZED)
            .run()
            .expectWarningCount(0)
    }

    /**
     * We want to make sure that we are have a limit on how deep down the AST we will look for the start
     * call, otherwise, we might have cases where this method could cause a hang if the AST has backlinks
     *
     * currently, this test that we will not try to go beyond 4 levels of nested calls. This test has
     * MParticle.start() in the 5th level, and should fail, as "Not Found"
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testStartDeeplyNested() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onCreate() {
       super.onCreate();
       int a = random(4);
       init();
    }
    public void init() {
       init1();       
       init2();    
    }
    public void init1() {
       int b = random(4);    
    }
    public void init2() {
       init3();    
    }
    public void init3() {
       init4();    
    }
    public void init4() {
       MParticle.start();    
    }
    public int random(int number) {
       return 5 + number;    
    }
    public void anotherMethod() {}
}
"""
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .skipTestModes(TestMode.PARENTHESIZED)
            .run()
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperyNestedInSeperateClass() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
import com.mparticle.lints.Initializer;
public class HasProperCall extends Application {
    Initializer initializer = new Initializer();
    public void onCreate() {
       super.onCreate();
       initializer.init();
       int a = random(4);
    }
    public int random(int number) {
       return 5 + number;
    }
    public void anotherMethod() {}
}"""
        lint()
            .files(java(source), mInitializerClass, mParticleStubClass, mApplicationStubClass)
            .run()
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperyNestedInSeperateSubClass() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
import com.mparticle.lints.Initializer;
import com.mparticle.lints.InitializerChild;
public class HasProperCall extends Application {
    InitializerChild initializer = new InitializerChild();
    public void onCreate() {
       super.onCreate();
       initializer.init();
       int a = random(4);
    }
    public int random(int number) {
       return 5 + number;
    }
    public void anotherMethod() {}
}"""
        lint()
            .files(
                java(source),
                mInitializerClass,
                mInitializerSubClass,
                mParticleStubClass,
                mApplicationStubClass
            )
            .run()
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledNestedMultipleCalls() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onCreate() {
       super.onCreate();
       int a = random(4);
       init();
    }
    public void init() {
       MParticle.start();
    }
    public int random(int number) {
       return 5 + number;
    }
    public void anotherMethod() {
       MParticle.start();
    }
}"""
        lint().files(java(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectContains(MESSAGE_START_CALLED_IN_WRONG_PLACE)
            .expectContains(Constants.getErrorWarningMessageString(0, 1))
    }

    @Test
    @Throws(Exception::class)
    fun testStartNotCalled() {
        @Language("JAVA") val source = """
            package com.mparticle.lints;
            import android.util.Log;
            import com.mparticle.MParticle;
            import android.app.Application;
            public class HasProperCall extends Application {
            }
        """.trimIndent()
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectContains(MESSAGE_NO_START_CALL_AT_ALL)
            .expectContains(Constants.getErrorWarningMessageString(0, 1))
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledMultipleTimes() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onResume() {
       super.onResume();
   }
    public void onCreate() {
       super.onCreate();
       MParticle.start();
       MParticle.start();
   }
}"""
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectContains(MESSAGE_MULTIPLE_START_CALLS)
            .expectContains(Constants.getErrorWarningMessageString(0, 1))
    }

    /**
     * This test guarantees that we are searching for start() calls in the correct order within onCreate(),
     * which should the order in which they will be executed. To attain this we need to keep a DFS.
     * This does not account for multiple children of android.app.Application and chained constructors.
     * In that case, order cannot be guaranteed
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testStartCallFoundInProperOrder() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onCreate() {
       super.onCreate();
       int a = random(4);
       init();
       MParticle.start();
    }
    public void init() {
       init1();       
       init2();    
    }
    public void init1() {
       int b = random(4);    
    }
    public void init2() {
       init3();    
    }
    public void init3() {
       MParticle.start();    
    }
    public int random(int number) {
       return 5 + number;
    }
    public void anotherMethod() {}
}"""
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectContains(MESSAGE_MULTIPLE_START_CALLS)
            .expectContains("HasProperCall.java:10")
            .expectContains(Constants.getErrorWarningMessageString(0, 1))
    }

    @Test
    @Throws(Exception::class)
    fun testStartErrorCombos() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onResume() {
       super.onResume();
       MParticle.start();
   }
    public void onCreate() {
       super.onCreate();
       MParticle.start();
   }
}"""
        @Language("JAVA") val source2 = """package com.mparticle.lints;
import com.mparticle.MParticle;
public class ClassTwo {
   public void methodName() {
       MParticle.start();
   }
}"""
        lint()
            .files(java(source), java(source2), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectContains(MESSAGE_START_CALLED_IN_WRONG_PLACE)
            .expectContains(Constants.getErrorWarningMessageString(0, 2))
    }

    @Test
    @Throws(Exception::class)
    fun testCalledOutsideApplicationClass() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import com.mparticle.MParticle;
public class ClassTwo {
   public void methodName() {
       MParticle.start();
   }
}"""
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectContains(MESSAGE_START_CALLED_IN_WRONG_PLACE)
            .expectContains(MESSAGE_NO_START_CALL_AT_ALL)
            .expectContains(Constants.getErrorWarningMessageString(0, 2))
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledInApplicationOutsideOnCreate() {
        @Language("JAVA") val source = """package com.mparticle.lints;
import android.util.Log;
import com.mparticle.MParticle;
import android.app.Application;
public class HasProperCall extends Application {
    public void onResume() {
       super.start();
       MParticle.start();
   }
}"""
        lint()
            .files(java(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectContains(MESSAGE_START_CALLED_IN_WRONG_PLACE)
            .expectContains(MESSAGE_NO_START_CALL_AT_ALL)
            .expectContains(Constants.getErrorWarningMessageString(0, 2))
    }

    private val mInitializerClass = java(
        """package com.mparticle.lints;
import com.mparticle.MParticle;
public class Initializer {
   public void init() {
       MParticle.start();
   }
}"""
    )
    private val mInitializerSubClass = java(
        """
    package com.mparticle.lints;
    import com.mparticle.MParticle;
    public class InitializerChild extends Initializer {
    }
        """.trimIndent()
    )
}
