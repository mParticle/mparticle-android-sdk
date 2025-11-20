package com.mparticle.modernization.uploading

import com.mparticle.modernization.config.UploadingConfiguration
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Data uploading strategies for different data types
 */
internal interface MParticleUploadingStrategy {
    /** Upload set of data
     *
     * @param data any type of data to upload
     * @param immediateUpload true or false depending if we want to force immediate data upload. By
     * default this is false
     * @param uploadingConfiguration to handle auto-data uploads or other custom implementation based
     * on a configuration.
     */
    suspend fun upload(
        @NotNull data: Any,
        @NotNull immediateUpload: Boolean,
        @Nullable uploadingConfiguration: UploadingConfiguration?
    )

    /**
     * @return strategy id
     */
    fun type(): Int
}
