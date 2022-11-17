package com.mparticle.kits

fun getKitsLoadedListener(
    action: ( 
        kits: Map<Int, KitIntegration?>, 
        previousKits: Map<Int, KitIntegration?>, 
        kitConfigs: List<KitConfiguration> 
    ) -> Unit
): KitManagerImpl.KitsLoadedListener {
    return object : KitManagerImpl.KitsLoadedListener {
        override fun onKitsLoaded(
            kits: Map<Int, KitIntegration?>,
            previousKits: Map<Int, KitIntegration?>,
            kitConfigs: List<KitConfiguration>
        ) {
            action.invoke(kits, previousKits, kitConfigs)
        }
    }
}
