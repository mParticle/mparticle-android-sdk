package com.mparticle.internal

import org.json.JSONArray

object SideloadedKitsUtils {

    fun combineConfig(kitConfig: JSONArray?, kits: List<SideloadedKit>): JSONArray {
        var results = JSONArray()
        var addedIds = mutableSetOf<Int>()
        kitConfig?.let { kitConfig ->
            for (i in 0 until kitConfig.length()) {
                val kit = kitConfig.getJSONObject(i)
                val id = kit.optInt("id", -1)
                if (id != -1 && id < 1000000 && !addedIds.contains(id)) {
                    results.put(kit)
                    addedIds.add(id)
                }
            }
        }
        for (i in kits.indices) {
            val kit = kits.get(i)
            if (!addedIds.contains(kit.kitId())) {
                kit.getJsonConfig()?.let {
                    results.put(it)
                    addedIds.add(kit.kitId())
                }
            }
        }
        return results
    }
}
