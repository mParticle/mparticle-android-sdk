package com.mparticle.internal.messages

import android.location.Location
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Promotion
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import com.mparticle.internal.MPUtility
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

open class MPCommerceMessage : BaseMPMessage {
    @Throws(JSONException::class)
    protected constructor(
        builder: Builder?,
        session: InternalSession?,
        location: Location?,
        mpId: Long,
    ) : super(builder, session, location, mpId)

    open class Builder(commerceEvent: CommerceEvent?) : BaseMPMessageBuilder(Constants.MessageType.COMMERCE_EVENT) {
        init {
            addCommerceEventInfo(this, commerceEvent)
        }

        override fun timestamp(timestamp: Long): BaseMPMessageBuilder = super.timestamp(timestamp)

        open val productAction: String?
            get() {
                val productActionObj = optJSONObject(Constants.Commerce.PRODUCT_ACTION_OBJECT)
                if (productActionObj != null) {
                    return productActionObj.optString(Constants.Commerce.PRODUCT_ACTION)
                }
                return null
            }

        @Throws(JSONException::class)
        override fun build(session: InternalSession?, location: Location?, mpId: Long): BaseMPMessage =
            MPCommerceMessage(this, session, location, mpId)

        private companion object {
            private fun addCommerceEventInfo(message: JSONObject, event: CommerceEvent?) {
                try {
                    if (event!!.screen != null) {
                        message.put(Constants.Commerce.SCREEN_NAME, event.screen)
                    }
                    if (event.nonInteraction != null) {
                        message.put(Constants.Commerce.NON_INTERACTION, event.nonInteraction!!)
                    }
                    if (event.currency != null) {
                        message.put(Constants.Commerce.CURRENCY, event.currency)
                    }
                    if (event.customAttributeStrings != null) {
                        message.put(Constants.Commerce.ATTRIBUTES, MPUtility.mapToJson(event.customAttributeStrings))
                    }
                    if (event.productAction != null) {
                        val productAction = JSONObject()
                        message.put(Constants.Commerce.PRODUCT_ACTION_OBJECT, productAction)
                        productAction.put(Constants.Commerce.PRODUCT_ACTION, event.productAction)
                        if (event.checkoutStep != null) {
                            productAction.put(Constants.Commerce.CHECKOUT_STEP, event.checkoutStep)
                        }
                        if (event.checkoutOptions != null) {
                            productAction.put(Constants.Commerce.CHECKOUT_OPTIONS, event.checkoutOptions)
                        }
                        if (event.productListName != null) {
                            productAction.put(Constants.Commerce.PRODUCT_LIST_NAME, event.productListName)
                        }
                        if (event.productListSource != null) {
                            productAction.put(Constants.Commerce.PRODUCT_LIST_SOURCE, event.productListSource)
                        }
                        val transactionAttributes = event.transactionAttributes
                        if (transactionAttributes != null) {
                            if (transactionAttributes.id != null) {
                                productAction.put(Constants.Commerce.TRANSACTION_ID, transactionAttributes.id)
                            }
                            if (transactionAttributes.affiliation != null) {
                                productAction.put(Constants.Commerce.TRANSACTION_AFFILIATION, transactionAttributes.affiliation)
                            }
                            if (transactionAttributes.revenue != null) {
                                productAction.put(Constants.Commerce.TRANSACTION_REVENUE, transactionAttributes.revenue)
                            }
                            if (transactionAttributes.tax != null) {
                                productAction.put(Constants.Commerce.TRANSACTION_TAX, transactionAttributes.tax)
                            }
                            if (transactionAttributes.shipping != null) {
                                productAction.put(Constants.Commerce.TRANSACTION_SHIPPING, transactionAttributes.shipping)
                            }
                            if (transactionAttributes.couponCode != null) {
                                productAction.put(Constants.Commerce.TRANSACTION_COUPON_CODE, transactionAttributes.couponCode)
                            }
                        }
                        if (event.products != null && event.products!!.size > 0) {
                            val products = JSONArray()
                            for (i in 0 until event.products!!.size) {
                                products.put(JSONObject(event.products!![i].toString()))
                            }
                            productAction.put(Constants.Commerce.PRODUCT_LIST, products)
                        }
                    }
                    if (event.promotionAction != null) {
                        val promotionAction = JSONObject()
                        message.put(Constants.Commerce.PROMOTION_ACTION_OBJECT, promotionAction)
                        promotionAction.put(Constants.Commerce.PROMOTION_ACTION, event.promotionAction)
                        if (event.promotions != null && event.promotions!!.size > 0) {
                            val promotions = JSONArray()
                            for (i in 0 until event.promotions!!.size) {
                                promotions.put(getPromotionJson(event.promotions!![i]))
                            }
                            promotionAction.put(Constants.Commerce.PROMOTION_LIST, promotions)
                        }
                    }
                    if (event.impressions != null && event.impressions!!.size > 0) {
                        val impressions = JSONArray()
                        for (impression in event.impressions!!) {
                            val impressionJson = JSONObject()
                            if (impression.listName != null) {
                                impressionJson.put(Constants.Commerce.IMPRESSION_LOCATION, impression.listName)
                            }
                            if (impression.products != null && impression.products.size > 0) {
                                val productsJson = JSONArray()
                                impressionJson.put(Constants.Commerce.IMPRESSION_PRODUCT_LIST, productsJson)
                                for (product in impression.products) {
                                    productsJson.put(JSONObject(product.toString()))
                                }
                            }
                            if (impressionJson.length() > 0) {
                                impressions.put(impressionJson)
                            }
                        }
                        if (impressions.length() > 0) {
                            message.put(Constants.Commerce.IMPRESSION_OBJECT, impressions)
                        }
                    }
                } catch (jse: Exception) {
                }
            }

            private fun getPromotionJson(promotion: Promotion): JSONObject {
                val json = JSONObject()
                try {
                    if (!MPUtility.isEmpty(promotion.id)) {
                        json.put("id", promotion.id)
                    }
                    if (!MPUtility.isEmpty(promotion.name)) {
                        json.put("nm", promotion.name)
                    }
                    if (!MPUtility.isEmpty(promotion.creative)) {
                        json.put("cr", promotion.creative)
                    }
                    if (!MPUtility.isEmpty(promotion.position)) {
                        json.put("ps", promotion.position)
                    }
                } catch (jse: JSONException) {
                }
                return json
            }
        }
    }
}
