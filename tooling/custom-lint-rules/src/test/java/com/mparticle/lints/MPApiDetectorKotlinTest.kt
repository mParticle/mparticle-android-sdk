package com.mparticle.lints

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.mparticle.lints.Constants.mApplicationStubClass
import com.mparticle.lints.Constants.mParticleStubClass
import com.mparticle.lints.detectors.MpApiDetectorKt
import com.mparticle.lints.detectors.MpApiDetectorKt.Companion.ISSUE
import com.mparticle.lints.detectors.MpApiDetectorKt.Companion.MESSAGE_NO_START_CALL_IN_ON_CREATE
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.junit.Test

class MPApiDetectorKotlinTest : LintDetectorTest() {
    private val mKotlinInitializerStub = kotlin(
        """package com.mparticle.lints;
import com.mparticle.MParticle;
class Initializer {
    fun init() {
        MParticle.start();
    }
}"""
    )

    override fun getDetector(): Detector {
        return MpApiDetectorKt()
    }

    override fun getIssues(): List<Issue> {
        return listOf(ISSUE)
    }

    override fun allowMissingSdk(): Boolean {
        return true
    }

    @Throws(Exception::class)
    fun testStartCalledProperly() {
        @Language("KT") val source = """package com.mparticle.lints
import android.app.Application
import com.mparticle.MParticle
class HasProperCall : Application() {
   override fun onCreate() {
       super.onCreate()
       MParticle.start()
   }
}"""
        lint()
            .files(kotlin(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectErrorCount(0)
            .expectWarningCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperlyNestedInSeperateClassKotlin() {
        @Language("KT") val source = """package com.mparticle.lints;
import android.app.Application
import com.mparticle.MParticle

class HasProperCall : Application() {
    val initializer = Initializer()
    
    override fun onCreate() {
        super.onCreate()
        initializer.init()
    }
}"""
        lint()
            .files(kotlin(source), mKotlinInitializerStub, mParticleStubClass, mApplicationStubClass)
            .run()
            .expectWarningCount(0)
            .expectErrorCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperlyInBlock() {
        @Language("KT") val source = """package com.mparticle.lints
   import android.app.Application
   import com.mparticle.MParticle
class HasProperCall : Application(){
   override fun onCreate() {
        super.onCreate()
        doNothing()
.also { 
           doSomething()
           MParticle.start() }
    }
    
    fun doNothing() {
        
    }
   fun doSomething() : Int {       1;   }}"""
        lint()
            .files(kotlin(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectWarningCount(0)
            .expectErrorCount(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperyInLocalFunction() {
        @Language("KT") val source = """package com.mparticle.lints
   import android.app.Application
   import com.mparticle.MParticle
class HasProperCall : Application(){
   override fun onCreate() {
        super.onCreate()
        fun inner() {
           MParticle.start()
       }
       inner()
    }
}"""
        TestCase.assertEquals(
            Constants.NO_WARNINGS,
            lintProject(kotlin(source), mParticleStubClass, mApplicationStubClass)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testStartCalledProperlyInLocalLambdaFunction() {
        @Language("KT") val source = """package com.mparticle.lints
   import android.app.Application
   import com.mparticle.MParticle
class HasProperCall : Application(){
   override fun onCreate() {
        super.onCreate()
        fun innerLambda() = MParticle.start()
       innerLambda()
    }
}"""
        TestCase.assertEquals(
            Constants.NO_WARNINGS,
            lintProject(kotlin(source), mParticleStubClass, mApplicationStubClass)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testStartNotCalledProperyInLocalFunction() {
        @Language("KT") val source = """package com.mparticle.lints
   import android.app.Application
   import com.mparticle.MParticle
class HasProperCall : Application(){
   override fun onCreate() {
        super.onCreate()
        var a = onResume()
        fun inner() {
           MParticle.start()
       }
       fun innerLambda() = MParticle.start()
    }
}"""
        lint()
            .files(kotlin(source), mParticleStubClass, mApplicationStubClass)
            .run()
            .expectContains(MESSAGE_NO_START_CALL_IN_ON_CREATE)
            .expectContains(Constants.getErrorWarningMessageString(0, 1))
    }
}
