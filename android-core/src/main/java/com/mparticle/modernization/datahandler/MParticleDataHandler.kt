package com.mparticle.modernization.datahandler

import com.mparticle.modernization.core.MParticleComponent
import org.jetbrains.annotations.NotNull

internal interface MParticleDataHandler : MParticleComponent {
    /**
     *Save set of data using provided strategies. Decision made base on [type]
     *
     * @param data any type of data. Each strategy is responsible of converting and handling the data
     * @param immediateUpload true or false depending if we want to force immediate data upload. By
     * default this is false
     */
    suspend fun saveData(
        @NotNull data: Any,
        @NotNull immediateUpload: Boolean = false
    )

    suspend fun configure() {}
}
