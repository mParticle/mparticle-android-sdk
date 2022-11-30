package com.mparticle.kits

import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.internal.MPUtility
import java.util.*

object CommerceEventUtils {
    const val PLUSONE_NAME = "eCommerce - %s - Total"
    private const val ITEM_NAME = "eCommerce - %s - Item"
    private const val IMPRESSION_NAME = "eCommerce - Impression - Item"
    fun expand(event: CommerceEvent?): List<MPEvent> {
        val eventList: MutableList<MPEvent> = LinkedList()
        if (event == null) {
            return eventList
        }
        eventList.addAll(expandProductAction(event))
        eventList.addAll(expandPromotionAction(event))
        eventList.addAll(expandProductImpression(event))
        return eventList
    }

    fun expandProductAction(event: CommerceEvent): List<MPEvent> {
        val events: MutableList<MPEvent> = LinkedList()
        val productAction = event.productAction ?: return events
        if (productAction.equals(Product.PURCHASE, ignoreCase = true) || productAction.equals(
                Product.REFUND, ignoreCase = true
            )
        ) {
            val plusOne = MPEvent.Builder(
                String.format(PLUSONE_NAME, event.productAction),
                MParticle.EventType.Transaction
            )
            // Set all product action fields to attributes.
            val attributes: MutableMap<String, String?> = HashMap()
            // Start with the custom attributes then overwrite with action fields.
            if (event.customAttributeStrings != null) {
                attributes.putAll(event.customAttributeStrings!!)
            }
            extractActionAttributes(event, attributes)
            events.add(
                plusOne.customAttributes(attributes).shouldUploadEvent(event.isShouldUploadEvent)
                    .build()
            )
        }
        val products = event.products
        if (products != null) {
            for (i in products.indices) {
                val itemEvent = MPEvent.Builder(
                    String.format(ITEM_NAME, productAction),
                    MParticle.EventType.Transaction
                )
                val attributes: MutableMap<String, String?> = HashMap()
                val attributeExtracted: OnAttributeExtracted = StringAttributeExtractor(attributes)
                extractProductFields(products[i], attributeExtracted)
                extractProductAttributes(products[i], attributeExtracted)
                extractTransactionId(event, attributeExtracted)
                events.add(
                    itemEvent.customAttributes(attributes)
                        .shouldUploadEvent(event.isShouldUploadEvent).build()
                )
            }
        }
        return events
    }

    fun extractProductFields(product: Product?, attributes: MutableMap<String, String?>) {
        extractProductFields(product, StringAttributeExtractor(attributes))
    }

    fun extractProductFields(product: Product?, onAttributeExtracted: OnAttributeExtracted) {
        if (product != null) {
            if (!MPUtility.isEmpty(product.couponCode)) {
                onAttributeExtracted.onAttributeExtracted(
                    Constants.ATT_PRODUCT_COUPON_CODE,
                    product.couponCode
                )
            }
            if (!MPUtility.isEmpty(product.brand)) {
                onAttributeExtracted.onAttributeExtracted(
                    Constants.ATT_PRODUCT_BRAND,
                    product.brand
                )
            }
            if (!MPUtility.isEmpty(product.category)) {
                onAttributeExtracted.onAttributeExtracted(
                    Constants.ATT_PRODUCT_CATEGORY,
                    product.category
                )
            }
            if (!MPUtility.isEmpty(product.name)) {
                onAttributeExtracted.onAttributeExtracted(Constants.ATT_PRODUCT_NAME, product.name)
            }
            if (!MPUtility.isEmpty(product.sku)) {
                onAttributeExtracted.onAttributeExtracted(Constants.ATT_PRODUCT_ID, product.sku)
            }
            if (!MPUtility.isEmpty(product.variant)) {
                onAttributeExtracted.onAttributeExtracted(
                    Constants.ATT_PRODUCT_VARIANT,
                    product.variant
                )
            }
            if (product.position != null) {
                onAttributeExtracted.onAttributeExtracted(
                    Constants.ATT_PRODUCT_POSITION,
                    product.position!!
                )
            }
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_PRODUCT_PRICE,
                product.unitPrice
            )
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_PRODUCT_QUANTITY,
                product.quantity
            )
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_PRODUCT_TOTAL_AMOUNT,
                product.totalAmount
            )
        }
    }

    fun extractProductAttributes(product: Product?, attributes: MutableMap<String, String?>) {
        extractProductAttributes(product, StringAttributeExtractor(attributes))
    }

    fun extractProductAttributes(product: Product?, onAttributeExtracted: OnAttributeExtracted) {
        if (product != null) {
            if (product.customAttributes != null) {
                onAttributeExtracted.onAttributeExtracted(product.customAttributes)
            }
        }
    }

    private fun extractTransactionId(
        event: CommerceEvent?,
        onAttributeExtracted: OnAttributeExtracted
    ) {
        if (event != null && event.transactionAttributes != null && !MPUtility.isEmpty(
                event.transactionAttributes!!.id
            )
        ) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_TRANSACTION_ID,
                event.transactionAttributes!!
                    .id
            )
        }
    }

    fun extractActionAttributes(event: CommerceEvent, attributes: MutableMap<String, String?>) {
        extractActionAttributes(event, StringAttributeExtractor(attributes))
    }

    fun extractActionAttributes(event: CommerceEvent, onAttributeExtracted: OnAttributeExtracted) {
        extractTransactionAttributes(event, onAttributeExtracted)
        extractTransactionId(event, onAttributeExtracted)
        var currency = event.currency
        if (MPUtility.isEmpty(currency)) {
            currency = Constants.DEFAULT_CURRENCY_CODE
        }
        onAttributeExtracted.onAttributeExtracted(Constants.ATT_ACTION_CURRENCY_CODE, currency)
        val checkoutOptions = event.checkoutOptions
        if (!MPUtility.isEmpty(checkoutOptions)) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_ACTION_CHECKOUT_OPTIONS,
                checkoutOptions
            )
        }
        if (event.checkoutStep != null) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_ACTION_CHECKOUT_STEP,
                event.checkoutStep!!
            )
        }
        if (!MPUtility.isEmpty(event.productListSource)) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_ACTION_PRODUCT_LIST_SOURCE,
                event.productListSource
            )
        }
        if (!MPUtility.isEmpty(event.productListName)) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_ACTION_PRODUCT_ACTION_LIST,
                event.productListName
            )
        }
    }

    fun extractTransactionAttributes(
        event: CommerceEvent?,
        attributes: MutableMap<String, String?>
    ): Map<String, String?> {
        if (event == null || event.transactionAttributes == null) {
            return attributes
        }
        extractTransactionAttributes(event, StringAttributeExtractor(attributes))
        return attributes
    }

    fun extractTransactionAttributes(
        event: CommerceEvent,
        onAttributeExtracted: OnAttributeExtracted
    ) {
        val transactionAttributes = event.transactionAttributes
        extractTransactionId(event, onAttributeExtracted)
        if (!MPUtility.isEmpty(transactionAttributes!!.affiliation)) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_AFFILIATION,
                transactionAttributes.affiliation
            )
        }
        if (!MPUtility.isEmpty(transactionAttributes.couponCode)) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_TRANSACTION_COUPON_CODE,
                transactionAttributes.couponCode
            )
        }
        if (transactionAttributes.revenue != null) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_TOTAL,
                transactionAttributes.revenue!!
            )
        }
        if (transactionAttributes.shipping != null) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_SHIPPING,
                transactionAttributes.shipping!!
            )
        }
        if (transactionAttributes.tax != null) {
            onAttributeExtracted.onAttributeExtracted(
                Constants.ATT_TAX,
                transactionAttributes.tax!!
            )
        }
    }

    fun expandPromotionAction(event: CommerceEvent): List<MPEvent> {
        val events: MutableList<MPEvent> = LinkedList()
        val promotionAction = event.promotionAction ?: return events
        val promotions = event.promotions
        if (promotions != null) {
            for (i in promotions.indices) {
                val itemEvent = MPEvent.Builder(
                    String.format(ITEM_NAME, promotionAction),
                    MParticle.EventType.Transaction
                )
                val attributes: MutableMap<String, String?> = HashMap()
                if (event.customAttributeStrings != null) {
                    attributes.putAll(event.customAttributeStrings!!)
                }
                extractPromotionAttributes(promotions[i], attributes)
                events.add(
                    itemEvent.customAttributes(attributes)
                        .shouldUploadEvent(event.isShouldUploadEvent).build()
                )
            }
        }
        return events
    }

    fun extractPromotionAttributes(promotion: Promotion?, attributes: MutableMap<String, String?>) {
        extractPromotionAttributes(promotion, StringAttributeExtractor(attributes))
    }

    fun extractPromotionAttributes(
        promotion: Promotion?,
        onAttributeExtracted: OnAttributeExtracted
    ) {
        if (promotion != null) {
            if (!MPUtility.isEmpty(promotion.id)) {
                onAttributeExtracted.onAttributeExtracted(Constants.ATT_PROMOTION_ID, promotion.id)
            }
            if (!MPUtility.isEmpty(promotion.position)) {
                onAttributeExtracted.onAttributeExtracted(
                    Constants.ATT_PROMOTION_POSITION,
                    promotion.position
                )
            }
            if (!MPUtility.isEmpty(promotion.name)) {
                onAttributeExtracted.onAttributeExtracted(
                    Constants.ATT_PROMOTION_NAME,
                    promotion.name
                )
            }
            if (!MPUtility.isEmpty(promotion.creative)) {
                onAttributeExtracted.onAttributeExtracted(
                    Constants.ATT_PROMOTION_CREATIVE,
                    promotion.creative
                )
            }
        }
    }

    fun expandProductImpression(event: CommerceEvent): List<MPEvent> {
        val impressions = event.impressions
        val events: MutableList<MPEvent> = LinkedList()
        if (impressions == null) {
            return events
        }
        for (i in impressions.indices) {
            val products = impressions[i].products
            if (products != null) {
                for (j in products.indices) {
                    val itemEvent =
                        MPEvent.Builder(IMPRESSION_NAME, MParticle.EventType.Transaction)
                    val attributes: MutableMap<String, String?> = HashMap()
                    if (event.customAttributeStrings != null) {
                        attributes.putAll(event.customAttributeStrings!!)
                    }
                    extractProductAttributes(products[i], attributes)
                    extractProductFields(products[i], attributes)
                    extractImpressionAttributes(impressions[i], attributes)
                    events.add(
                        itemEvent.customAttributes(attributes)
                            .shouldUploadEvent(event.isShouldUploadEvent).build()
                    )
                }
            }
        }
        return events
    }

    private fun extractImpressionAttributes(
        impression: Impression?,
        attributes: MutableMap<String, String?>
    ) {
        if (impression != null) {
            if (!MPUtility.isEmpty(impression.listName)) {
                attributes["Product Impression List"] = impression.listName
            }
        }
    }

    fun getEventTypeString(filteredEvent: CommerceEvent?): String {
        if (filteredEvent == null) return Constants.EVENT_TYPE_STRING_UNKNOWN
        val eventType = getEventType(filteredEvent)
        when (eventType) {
            Constants.EVENT_TYPE_ADD_TO_CART -> return Constants.EVENT_TYPE_STRING_ADD_TO_CART
            Constants.EVENT_TYPE_REMOVE_FROM_CART -> return Constants.EVENT_TYPE_STRING_REMOVE_FROM_CART
            Constants.EVENT_TYPE_CHECKOUT -> return Constants.EVENT_TYPE_STRING_CHECKOUT
            Constants.EVENT_TYPE_CHECKOUT_OPTION -> return Constants.EVENT_TYPE_STRING_CHECKOUT_OPTION
            Constants.EVENT_TYPE_CLICK -> return Constants.EVENT_TYPE_STRING_CLICK
            Constants.EVENT_TYPE_VIEW_DETAIL -> return Constants.EVENT_TYPE_STRING_VIEW_DETAIL
            Constants.EVENT_TYPE_PURCHASE -> return Constants.EVENT_TYPE_STRING_PURCHASE
            Constants.EVENT_TYPE_REFUND -> return Constants.EVENT_TYPE_STRING_REFUND
            Constants.EVENT_TYPE_ADD_TO_WISHLIST -> return Constants.EVENT_TYPE_STRING_ADD_TO_WISHLIST
            Constants.EVENT_TYPE_REMOVE_FROM_WISHLIST -> return Constants.EVENT_TYPE_STRING_REMOVE_FROM_WISHLIST
            Constants.EVENT_TYPE_PROMOTION_VIEW -> return Constants.EVENT_TYPE_STRING_PROMOTION_VIEW
            Constants.EVENT_TYPE_PROMOTION_CLICK -> return Constants.EVENT_TYPE_STRING_PROMOTION_CLICK
            Constants.EVENT_TYPE_IMPRESSION -> return Constants.EVENT_TYPE_STRING_IMPRESSION
        }
        return Constants.EVENT_TYPE_STRING_UNKNOWN
    }

    fun getEventType(filteredEvent: CommerceEvent): Int {
        if (!MPUtility.isEmpty(filteredEvent.productAction)) {
            val action = filteredEvent.productAction
            if (action.equals(Product.ADD_TO_CART, ignoreCase = true)) {
                return Constants.EVENT_TYPE_ADD_TO_CART
            } else if (action.equals(Product.REMOVE_FROM_CART, ignoreCase = true)) {
                return Constants.EVENT_TYPE_REMOVE_FROM_CART
            } else if (action.equals(Product.CHECKOUT, ignoreCase = true)) {
                return Constants.EVENT_TYPE_CHECKOUT
            } else if (action.equals(Product.CHECKOUT_OPTION, ignoreCase = true)) {
                return Constants.EVENT_TYPE_CHECKOUT_OPTION
            } else if (action.equals(Product.CLICK, ignoreCase = true)) {
                return Constants.EVENT_TYPE_CLICK
            } else if (action.equals(Product.DETAIL, ignoreCase = true)) {
                return Constants.EVENT_TYPE_VIEW_DETAIL
            } else if (action.equals(Product.PURCHASE, ignoreCase = true)) {
                return Constants.EVENT_TYPE_PURCHASE
            } else if (action.equals(Product.REFUND, ignoreCase = true)) {
                return Constants.EVENT_TYPE_REFUND
            } else if (action.equals(Product.ADD_TO_WISHLIST, ignoreCase = true)) {
                return Constants.EVENT_TYPE_ADD_TO_WISHLIST
            } else if (action.equals(Product.REMOVE_FROM_WISHLIST, ignoreCase = true)) {
                return Constants.EVENT_TYPE_REMOVE_FROM_WISHLIST
            }
        }
        if (!MPUtility.isEmpty(filteredEvent.promotionAction)) {
            val action = filteredEvent.promotionAction
            if (action.equals(Promotion.VIEW, ignoreCase = true)) {
                return Constants.EVENT_TYPE_PROMOTION_VIEW
            } else if (action.equals(Promotion.CLICK, ignoreCase = true)) {
                return Constants.EVENT_TYPE_PROMOTION_CLICK
            }
        }
        return Constants.EVENT_TYPE_IMPRESSION
    }

    interface Constants {
        companion object {
            const val ATT_AFFILIATION = "Affiliation"
            const val ATT_TRANSACTION_COUPON_CODE = "Coupon Code"
            const val ATT_TOTAL = "Total Amount"
            const val ATT_SHIPPING = "Shipping Amount"
            const val ATT_TAX = "Tax Amount"
            const val ATT_TRANSACTION_ID = "Transaction Id"
            const val ATT_PRODUCT_QUANTITY = "Quantity"
            const val ATT_PRODUCT_POSITION = "Position"
            const val ATT_PRODUCT_VARIANT = "Variant"
            const val ATT_PRODUCT_ID = "Id"
            const val ATT_PRODUCT_NAME = "Name"
            const val ATT_PRODUCT_CATEGORY = "Category"
            const val ATT_PRODUCT_BRAND = "Brand"
            const val ATT_PRODUCT_COUPON_CODE = "Coupon Code"
            const val ATT_PRODUCT_PRICE = "Item Price"
            const val ATT_ACTION_PRODUCT_ACTION_LIST = "Product Action List"
            const val ATT_ACTION_PRODUCT_LIST_SOURCE = "Product List Source"
            const val ATT_ACTION_CHECKOUT_OPTIONS = "Checkout Options"
            const val ATT_ACTION_CHECKOUT_STEP = "Checkout Step"
            const val ATT_ACTION_CURRENCY_CODE = "Currency Code"
            const val ATT_SCREEN_NAME = "Screen Name"
            const val ATT_PROMOTION_ID = "Id"
            const val ATT_PROMOTION_POSITION = "Position"
            const val ATT_PROMOTION_NAME = "Name"
            const val ATT_PROMOTION_CREATIVE = "Creative"
            const val ATT_PRODUCT_TOTAL_AMOUNT = "Total Product Amount"
            const val RESERVED_KEY_LTV = "\$Amount"

            /**
             * This is only set when required. Otherwise default to null.
             */
            const val DEFAULT_CURRENCY_CODE = "USD"
            const val EVENT_TYPE_ADD_TO_CART = 10
            const val EVENT_TYPE_REMOVE_FROM_CART = 11
            const val EVENT_TYPE_CHECKOUT = 12
            const val EVENT_TYPE_CHECKOUT_OPTION = 13
            const val EVENT_TYPE_CLICK = 14
            const val EVENT_TYPE_VIEW_DETAIL = 15
            const val EVENT_TYPE_PURCHASE = 16
            const val EVENT_TYPE_REFUND = 17
            const val EVENT_TYPE_PROMOTION_VIEW = 18
            const val EVENT_TYPE_PROMOTION_CLICK = 19
            const val EVENT_TYPE_ADD_TO_WISHLIST = 20
            const val EVENT_TYPE_REMOVE_FROM_WISHLIST = 21
            const val EVENT_TYPE_IMPRESSION = 22
            const val EVENT_TYPE_STRING_ADD_TO_CART = "ProductAddToCart"
            const val EVENT_TYPE_STRING_REMOVE_FROM_CART = "ProductRemoveFromCart"
            const val EVENT_TYPE_STRING_CHECKOUT = "ProductCheckout"
            const val EVENT_TYPE_STRING_CHECKOUT_OPTION = "ProductCheckoutOption"
            const val EVENT_TYPE_STRING_CLICK = "ProductClick"
            const val EVENT_TYPE_STRING_VIEW_DETAIL = "ProductViewDetail"
            const val EVENT_TYPE_STRING_PURCHASE = "ProductPurchase"
            const val EVENT_TYPE_STRING_REFUND = "ProductRefund"
            const val EVENT_TYPE_STRING_PROMOTION_VIEW = "PromotionView"
            const val EVENT_TYPE_STRING_PROMOTION_CLICK = "PromotionClick"
            const val EVENT_TYPE_STRING_ADD_TO_WISHLIST = "ProductAddToWishlist"
            const val EVENT_TYPE_STRING_REMOVE_FROM_WISHLIST = "ProductRemoveFromWishlist"
            const val EVENT_TYPE_STRING_IMPRESSION = "ProductImpression"
            const val EVENT_TYPE_STRING_UNKNOWN = "Unknown"
        }
    }

    private class StringAttributeExtractor internal constructor(var attributes: MutableMap<String, String?>) :
        OnAttributeExtracted {
        override fun onAttributeExtracted(key: String, value: String?) {
            attributes[key] = value
        }

        override fun onAttributeExtracted(key: String, value: Double) {
            attributes[key] = java.lang.Double.toString(value)
        }

        override fun onAttributeExtracted(key: String, value: Int) {
            attributes[key] = Integer.toString(value)
        }

        override fun onAttributeExtracted(customAttributes: Map<String, String?>?) {
            attributes.putAll(customAttributes!!)
        }
    }

    interface OnAttributeExtracted {
        fun onAttributeExtracted(key: String, value: String?)
        fun onAttributeExtracted(key: String, value: Double)
        fun onAttributeExtracted(key: String, value: Int)
        fun onAttributeExtracted(attributes: Map<String, String?>?)
    }
}
