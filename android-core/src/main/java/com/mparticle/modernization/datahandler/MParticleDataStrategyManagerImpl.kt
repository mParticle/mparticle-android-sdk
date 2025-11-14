package com.mparticle.modernization.datahandler

internal class MParticleDataStrategyManagerImpl(
    private val strategies: List<MParticleDataHandlerStrategy<*, *>>,
) : MParticleDataHandler {

    /**
     * Based on the data type we will choose the corresponding strategy provided at config type, and
     * execute an action on it.
     */
    override suspend fun saveData(data: Any, immediateUpload: Boolean) {
        data.javaClass.getStrategy()?.saveData(data, immediateUpload)
    }

    private fun Class<*>.getStrategy(): MParticleDataHandlerStrategy<*, *>? =
        strategies.firstOrNull { it.type() == DataHandlerType.getType(this) }

}
