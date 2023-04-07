package com.mparticle.modernization.uploading

import com.mparticle.modernization.core.MParticleComponent
import org.jetbrains.annotations.NotNull

internal interface MParticleDataUploader : MParticleComponent {
    /**
     * Upload set of data using provided strategies. Decision made base on [type]
     *
     * @param data any type of data. Each strategy is responsible of converting and handling the data
     * @param type an int (we recommend creating an enum class defining the types, each with and Int value
     * @param immediateUpload true or false depending if we want to force immediate data upload. By
     * default this is false
     */
    suspend fun upload(
        @NotNull data: Any,
        @NotNull type: Int,
        @NotNull immediateUpload: Boolean = false
    )

    /**
     * Trigger data uploader configuration. Called on DataUploader initialization
     */
    suspend fun configure() {}
}
