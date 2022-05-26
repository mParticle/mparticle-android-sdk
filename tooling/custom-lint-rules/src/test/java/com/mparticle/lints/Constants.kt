package com.mparticle.lints

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import org.intellij.lang.annotations.Language

object Constants {
    const val NO_WARNINGS = "No warnings."

    const val ERROR_WARNING_FORMAT = "%d errors, %d warnings"

    fun getErrorWarningMessageString(errors: Int, warnings: Int): String {
        return String.format(ERROR_WARNING_FORMAT, errors, warnings)
    }

    @Language("JAVA")
    const val mparticleStub =
        """package com.mparticle;
    public class MParticle {
       public static void start() {}
    }"""

    @Language("JAVA")
    const val applicationStub = """
    package android.app; 
    public class Application {
       public void onCreate() {}
       public void onResume() {}
    }"""

    val mParticleStubClass = java(mparticleStub)
    val mApplicationStubClass = java(applicationStub)
}
