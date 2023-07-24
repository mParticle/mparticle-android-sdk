package com.mparticle.tooling

import org.json.JSONObject

data class Config(
    var credentialsFilePath: String? = null,
    var dataPlanId: String? = null,
    var workspaceId: String? = null,
    var dataPlanVersion: String? = null,
    var dataPlanVersionFile: String? = null,
    var resultsFile: String? = null,
    var verbose: Boolean? = null,
    var disabled: Boolean? = null,
    var debugReportServerMessage: Boolean? = null
) {

    var internalConfig: InternalConfig = InternalConfig()

    fun toJson(): JSONObject {
        val json = JSONObject()
        Config::class.java.declaredMethods
            .filter { it.name.startsWith("get") && !(it.name == "getInternalConfig") }
            .forEach {
                json.putOpt(it.name.removePrefix("get"), it.invoke(this))
            }
        json.put("internal-config", internalConfig.toJson())
        return json
    }

    companion object {
        fun from(json: JSONObject): Config {
            val config = Config()
            json.keys().forEach { key ->
                if (key == "internal-config") {
                    config.internalConfig =
                        InternalConfig.fromJson(json.optJSONObject("internal-config"))
                } else {
                    Config::class.java.declaredMethods
                        .firstOrNull {
                            it.name.removePrefix("set").toLowerCase() == key?.toString()
                                ?.toLowerCase()
                        }
                        ?.invoke(config, json.opt(key.toString()))
                }
            }
            return config
        }
    }

    data class InternalConfig(
        var nodePath: String? = null,
        var mpPath: String? = null
    ) {

        fun toJson(): JSONObject {
            return JSONObject()
                .putOpt("node-path", nodePath)
                .putOpt("mp-path", mpPath)
        }

        companion object {
            fun fromJson(json: JSONObject): InternalConfig {
                return InternalConfig(
                    nodePath = json.optString("node-path", null),
                    mpPath = json.optString("mp-path", null)
                )
            }
        }
    }
}
