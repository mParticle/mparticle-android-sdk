package com.mparticle.lints

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import org.intellij.lang.annotations.Language

object Constants {
    const val NO_WARNINGS = "No warnings."

    const val ERROR_WARNING_FORMAT = "%d errors, %d warnings"

    fun getErrorWarningMessageString(
        errors: Int,
        warnings: Int,
    ): String = String.format(ERROR_WARNING_FORMAT, errors, warnings)

    @Language("JAVA")
    const val MPARTICLE_STUB =
        """package com.mparticle;
    public class MParticle {
       public static void start() {}
    }"""

    @Language("JAVA")
    const val APPLICATION_STUB = """
    package android.app;
    public class Application {
       public void onCreate() {}
       public void onResume() {}
    }"""

    @Language("JAVA")
    const val MPEVENT_STUB = """package com.mparticle;
    public class MPEvent {
       public static class Builder {
           public Builder(String eventName) {}
           public Builder customAttributes(java.util.Map<String, ?> customAttributes) { return this; }
           public MPEvent build() { return new MPEvent(); }
       }
    }"""

    val mParticleStubClass = java(MPARTICLE_STUB)
    val mApplicationStubClass = java(APPLICATION_STUB)
    val mpEventStubClass = java(MPEVENT_STUB)
}
