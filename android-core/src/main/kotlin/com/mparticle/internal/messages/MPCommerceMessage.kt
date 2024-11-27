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

class MPCommerceMessage protected constructor(builder: Builder, session: InternalSession, location: Location?, mpId: Long) :
    BaseMPMessage(builder, session, location, mpId) {

    class Builder(commerceEvent: CommerceEvent) : BaseMPMessageBuilder(Constants.MessageType.COMMERCE_EVENT) {
        init {
            addCommerceEventInfo(this, commerceEvent)
        }

        val productAction: String?
            get() {
                val productActionObj = optJSONObject(Constants.Commerce.PRODUCT_ACTION_OBJECT)
                if (productActionObj != null) {
                    return productActionObj.optString(Constants.Commerce.PRODUCT_ACTION)
                }
                return null
            }

        @Throws(JSONException::class)
        override fun build(session: InternalSession, location: Location?, mpId: Long): BaseMPMessage {
            return MPCommerceMessage(this, session, location, mpId)
        }

        companion object {
            private fun addCommerceEventInfo(message: JSONObject, event: CommerceEvent) {
                try {
                    event.screen?.let {
                        message.put(Constants.Commerce.SCREEN_NAME, it)
                    }
                    event.nonInteraction?.let {
                        message.put(Constants.Commerce.NON_INTERACTION, it)
                    }
                    event.currency?.let {
                        message.put(Constants.Commerce.CURRENCY, it)
                    }
                    event.customAttributeStrings?.let {
                        message.put(Constants.Commerce.ATTRIBUTES, MPUtility.mapToJson(it))
                    }
                    event.productAction?.let {
                        val productAction = JSONObject()
                        message.put(Constants.Commerce.PRODUCT_ACTION_OBJECT, productAction)
                        productAction.put(Constants.Commerce.PRODUCT_ACTION, event.productAction)
                        event.checkoutStep?.let {
                            productAction.put(Constants.Commerce.CHECKOUT_STEP, it)
                        }
                        event.checkoutOptions?.let {
                            productAction.put(Constants.Commerce.CHECKOUT_OPTIONS, it)
                        }
                        event.productListName?.let {
                            productAction.put(Constants.Commerce.PRODUCT_LIST_NAME, it)
                        }
                        event.productListSource?.let {
                            productAction.put(Constants.Commerce.PRODUCT_LIST_SOURCE, it)
                        }
                        event.transactionAttributes?.let { transactionAttributes ->
                            transactionAttributes.id.let {
                                productAction.put(Constants.Commerce.TRANSACTION_ID, it)
                            }
                            transactionAttributes.affiliation.let {
                                productAction.put(Constants.Commerce.TRANSACTION_AFFILIATION, it)
                            }

                            transactionAttributes.revenue.let {
                                productAction.put(Constants.Commerce.TRANSACTION_REVENUE, it)
                            }
                            transactionAttributes.tax.let {
                                productAction.put(Constants.Commerce.TRANSACTION_TAX, it)
                            }
                            transactionAttributes.shipping.let {
                                productAction.put(Constants.Commerce.TRANSACTION_SHIPPING, it)
                            }
                            transactionAttributes.couponCode.let {
                                productAction.put(Constants.Commerce.TRANSACTION_COUPON_CODE, it)
                            }
                        }
                        event.products?.takeIf { it.isNotEmpty() }?.let { products ->
                            val productsArray = JSONArray()
                            for (currentProduct in products) {
                                productsArray.put(JSONObject(currentProduct.toString()))
                            }
                            productAction.put(Constants.Commerce.PRODUCT_LIST, productsArray)
                        }
                    }

                    event.promotionAction?.let {
                        val promotionAction = JSONObject()
                        message.put(Constants.Commerce.PROMOTION_ACTION_OBJECT, promotionAction)
                        promotionAction.put(Constants.Commerce.PROMOTION_ACTION, event.promotionAction)
                        event.promotions?.takeIf { it.isNotEmpty() }?.let { eventPromotions ->
                            val promotions = JSONArray()
                            for (i in eventPromotions.indices) {
                                promotions.put(getPromotionJson(eventPromotions[i]))
                            }
                            promotionAction.put(Constants.Commerce.PROMOTION_LIST, promotions)
                        }
                    }

                    event.impressions?.takeIf { it.isNotEmpty() }?.let { eventImpressions ->
                        val impressionsArray = JSONArray()
                        for (impression in eventImpressions) {
                            val impressionJson = JSONObject()
                            impression.listName.let {
                                impressionJson.put(Constants.Commerce.IMPRESSION_LOCATION, it)
                            }

                            impression.products.takeIf { it.isNotEmpty() }?.let {
                                val productsJson = JSONArray()
                                impressionJson.put(Constants.Commerce.IMPRESSION_PRODUCT_LIST, productsJson)
                                for (product in it) {
                                    productsJson.put(JSONObject(product.toString()))
                                }
                            }
                            impressionJson.takeIf { it.length() > 0 }?.let {
                                impressionsArray.put(impressionJson)
                            }
                        }
                        impressionsArray.takeIf { it.length() > 0 }.let {
                            message.put(Constants.Commerce.IMPRESSION_OBJECT, impressionsArray)
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
