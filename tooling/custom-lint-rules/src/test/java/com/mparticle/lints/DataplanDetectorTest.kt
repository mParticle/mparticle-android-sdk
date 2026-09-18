package com.mparticle.lints

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.mparticle.lints.Constants.mApplicationStubClass
import com.mparticle.lints.Constants.mParticleStubClass
import com.mparticle.lints.Constants.mpEventStubClass
import com.mparticle.lints.detectors.DataplanDetector
import com.mparticle.tooling.Config
import com.mparticle.tooling.Utils
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DataplanDetectorTest : LintDetectorTest() {
    @Test
    fun testCollection() {
        val sdkHome =
            System.getenv("ANDROID_HOME")
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

    // Regression test: a local variable feeding an MPEvent.Builder argument used to be resolved
    // by reflectively replaying every call made on it in the enclosing method, with no
    // restriction on what classes/methods those calls could touch. This reproduces that shape
    // (an unrelated File operation feeding the event name) and asserts lint never performs it.
    @Test
    fun testEnclosingMethodSideEffectsAreNeverExecuted() {
        val sdkHome =
            System.getenv("ANDROID_HOME")
                ?: "${System.getProperty("user.home")}/Library/Android/sdk"

        val marker = File.createTempFile("mparticle-lint-regression", ".txt")
        marker.writeText("must survive lint analysis")
        val markerPath = marker.absolutePath.replace("\\", "\\\\")

        try {
            withConfigFile(Config()) {
                @Language("JAVA")
                val source = """
                    package com.mparticle.lints;
                    import android.app.Application;
                    import com.mparticle.MPEvent;
                    import java.io.File;
                    public class HasUnrelatedFileCall extends Application {
                        @Override
                        public void onCreate() {
                            super.onCreate();
                            File f = new File("$markerPath");
                            f.delete();
                            String name = f.getName();
                            new MPEvent.Builder(name);
                        }
                    }
                    """
                lint()
                    .sdkHome(File(sdkHome))
                    .files(java(source), mParticleStubClass, mApplicationStubClass, mpEventStubClass)
                    .skipTestModes(TestMode.PARENTHESIZED)
                    .run()
            }
            assertTrue(
                "Data plan lint must not perform filesystem operations found in analyzed source",
                marker.exists(),
            )
        } finally {
            marker.delete()
        }
    }

    // A legitimate builder chain (the only thing this detector needs to resolve) must keep
    // resolving successfully - the allowlist should reject non-DTO reflection, not the feature.
    @Test
    fun testAllowlistedBuilderChainStillResolves() {
        val sdkHome =
            System.getenv("ANDROID_HOME")
                ?: "${System.getProperty("user.home")}/Library/Android/sdk"

        withConfigFile(Config()) {
            @Language("JAVA")
            val source = """
                package com.mparticle.lints;
                import android.app.Application;
                import com.mparticle.MPEvent;
                public class HasAllowlistedCall extends Application {
                    @Override
                    public void onCreate() {
                        super.onCreate();
                        new MPEvent.Builder("test").build();
                    }
                }
                """
            val result =
                lint()
                    .sdkHome(File(sdkHome))
                    .files(java(source), mParticleStubClass, mApplicationStubClass, mpEventStubClass)
                    .skipTestModes(TestMode.PARENTHESIZED)
                    .run()
            // With no local data plan configured, a successfully-resolved event is reported as
            // NO_DATA_PLAN; a failure to resolve at all would instead produce no report.
            result.expectContains(DataplanDetector.NO_DATA_PLAN.id)
        }
    }

    private fun withConfigFile(config: Config, block: () -> Unit) {
        Utils.setConfigFile(config)
        try {
            block()
        } finally {
            Utils.removeConfigFile()
        }
    }

    override fun requireCompileSdk() = false

    override fun getDetector() = DataplanDetector()

    override fun getIssues() = listOf(DataplanDetector.ISSUE, DataplanDetector.NODE_MISSING, DataplanDetector.NO_DATA_PLAN)
}
