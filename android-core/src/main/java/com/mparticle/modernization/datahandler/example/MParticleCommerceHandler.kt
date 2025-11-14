package com.mparticle.modernization.datahandler.example

import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.modernization.BatchManager
import com.mparticle.modernization.data.MParticleDataRepository
import com.mparticle.modernization.datahandler.BaseMParticleDataHandlerStrategy
import com.mparticle.modernization.datahandler.DataHandlerType

/**
 * As an example, each data specific type would have a handler (using the strategy pattern),
 * and inherit from a baseHandler with cross type capabilities (batch operations, etc)
 *
 * As an example I wrote a couple of operations to save/retrieve data and convert data from and to
 * businessModel/DTO.
 * Each type will create a concrete strategy implementing the defined operations and specifying its type
 */
internal class MParticleCommerceHandler(
    private val dataRepository: MParticleDataRepository,
      batchManager: BatchManager
) : BaseMParticleDataHandlerStrategy<CommerceEvent, BaseMPMessage>(batchManager) {

    override suspend fun saveData(
        data: Any,
        immediateUpload: Boolean
    ) {
        (data as CommerceEvent).toDto()?.let {
            with(dataRepository) { insertCommerceDTO(it) }
        }
        if (immediateUpload) {
            createAndUploadBatch()
        }
    }

    override suspend fun retrieveData(): List<BaseMPMessage> {
        return with(dataRepository) { getEventsByType() ?: emptyList() }
    }

    override fun type(): DataHandlerType = DataHandlerType.COMMERCE_EVENT

    override fun BaseMPMessage.toModel(): CommerceEvent? {
        //Grab a String and convert it into a business model object
        var strProduct = Product.fromString("")
        var productAction = Product.ADD_TO_CART
        return CommerceEvent.Builder(productAction, strProduct).build()
    }

    override fun CommerceEvent.toDto(): BaseMPMessage? {
        //        val message = BaseMPMessage.Builder(Constants.MessageType.BREADCRUMB).
//            .timestamp(mAppStateManager.getSession().mLastEventTime)
//            .build(mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
//
//        message.put(MessageKey.EVENT_START_TIME, mAppStateManager.getSession().mLastEventTime)
//        message.put(
//            MessageKey.BREADCRUMB_SESSION_COUNTER,
//            mConfigManager.getUserStorage().getCurrentSessionCounter()
//        )
//        message.put(MessageKey.BREADCRUMB_LABEL, breadcrumb)
        return null
    }
}
