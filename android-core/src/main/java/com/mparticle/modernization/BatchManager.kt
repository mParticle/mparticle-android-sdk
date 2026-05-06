package com.mparticle.modernization

class BatchManager(private val api : MpApiClientImpl) {

    suspend fun createBatch() : MpBatch? = MpBatch("", Math.random().toLong())

    suspend fun uploadBatch(batch : MpBatch) {
        api.uploadBatch(batch)
    }

}

class MpBatch(data : String, batchId : Long){}