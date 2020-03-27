package com.mparticle.lints.basedetectors

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.mparticle.tooling.Config
import com.mparticle.tooling.Logger
import com.mparticle.tooling.Utils

open class BaseDetector: Detector() {

    companion object {
        var config: Config? = null
        var count = 0
    }

    override fun beforeCheckEachProject(context: Context) {
        super.beforeCheckRootProject(context)
        //there is only 1 config file that exists when running in the linting environment.
        //By adding this conditional, it lets us set it manually for testing, and not have it overridden by
        //the file system
        config = Utils.getConfigFile()
        Logger.verbose = config?.verbose ?: false
        count++
    }
}