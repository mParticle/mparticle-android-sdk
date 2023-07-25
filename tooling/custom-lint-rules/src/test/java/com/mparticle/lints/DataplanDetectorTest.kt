package com.mparticle.lints

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.mparticle.lints.Constants.mParticleStubClass
import com.mparticle.lints.detectors.DataplanDetector
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.io.File

class DataplanDetectorTest : LintDetectorTest() {

    @Test
    fun testCollection() {
        val sdkHome = System.getenv("ANDROID_HOME")
            ?: "${System.getProperty("user.home")}/Library/Android/sdk"

        @Language("KT")
        val source = """
                package com.mparticle.lints
                import android.app.Application
                import com.mparticle.MParticle
                class HasProperCall : Application() {
                   override fun onCreate() {
                       super.onCreate()
                       var attributes = mapOf("this" to "that")
                   }
                }
                """
        lint()
            .sdkHome(File(sdkHome))
            .files(kotlin(source), mParticleStubClass)
            .skipTestModes(TestMode.PARENTHESIZED)
            .run()
            .expectErrorCount(0)
    }

    override fun requireCompileSdk() = false
    override fun getDetector() = DataplanDetector()
    override fun getIssues() =
        listOf(DataplanDetector.ISSUE, DataplanDetector.NODE_MISSING, DataplanDetector.NO_DATA_PLAN)
}
