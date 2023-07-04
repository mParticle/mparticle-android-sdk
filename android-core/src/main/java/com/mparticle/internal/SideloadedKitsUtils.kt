package com.mparticle.internal

import org.json.JSONArray

object SideloadedKitsUtils {

    fun combineConfig(kitConfig: JSONArray, kits: List<SideloadedKit>): JSONArray {
        kits.forEach { kit ->
            kit.getJsonConfig()?.let {
                kitConfig.put(it)
            } ?: kotlin.run { Logger.debug("Issue with kit ${kit.javaClass.simpleName}") }
        }
        return kitConfig
    }

    fun removeSideloadedKitsAndCombine(kitConfig: JSONArray, kits: List<SideloadedKit>): JSONArray {
        var results = JSONArray()
        if (!kits.isEmpty()) {
            val sideloadedIds = kits.map { it.getJsonConfig()?.optInt("id", -100) }
            for (i in 0 until kitConfig.length()) {
                val obj = kitConfig.getJSONObject(i)
                val id = obj.optInt("id", -1)
                if (id != -1 && !sideloadedIds.contains(id)) {
                    results.put(obj)
                }
            }
        } else {
            results = kitConfig
        }
        return combineConfig(results, kits)
    }
}
