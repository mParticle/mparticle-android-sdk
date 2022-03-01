package com.mparticle.lints.basedetectors

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.mparticle.tooling.Config
import com.mparticle.tooling.Logger
import com.mparticle.tooling.Utils

open class BaseDetector : Detector() {
    protected var disabled: Boolean = false
        get() = config?.disabled == true || field
        set
    private var configFile: Config? = null
    private var configLastModified = 0L
    protected var config: Config?
        get() {
            val lastModified = Utils.getConfigFileLastModified()
            if (configFile == null || lastModified != configLastModified) {
                configFile = Utils.getConfigFile()
            }
            return configFile
        }
        set(value) {
            configFile = value
        }

    override fun beforeCheckEachProject(context: Context) {
        super.beforeCheckRootProject(context)
        Logger.verbose = config?.verbose ?: false
    }
}
