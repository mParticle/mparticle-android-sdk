package com.mparticle.modernization.datahandler


import com.mparticle.modernization.BatchManager
import org.jetbrains.annotations.NotNull

/**
 * Data uploading strategies for different data types
 */
internal interface MParticleDataHandlerStrategy<I, O> {
    /** Upload set of data
     *
     * @param data any type of data to upload
     * @param immediateUpload true or false depending if we want to force immediate data upload. By
     * default this is false
     * @param uploadingConfiguration to handle auto-data uploads or other custom implementation based
     * on a configuration.
     */
    suspend fun saveData(
        @NotNull data: Any,
        @NotNull immediateUpload: Boolean
    )

    suspend fun retrieveData(): List<O>

    fun I.toDto(): O?

    fun O.toModel(): I?

    /**
     * @return strategy id
     */
    fun type(): DataHandlerType

}

abstract class BaseMParticleDataHandlerStrategy<I, O>(protected val batchManager: BatchManager) :
    MParticleDataHandlerStrategy<I, O> {
    protected open suspend fun createAndUploadBatch() {
        with(batchManager) {
            createBatch()?.let { this.uploadBatch(it) }
        }
    }
}
