package com.mparticle.kits

import android.text.TextUtils
import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.commerce.TransactionAttributes
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CurrencyType
import io.branch.referral.util.ProductCategory

/**
 * Created by sojanpr on 4/11/18.
 *
 *
 * Class for Branch utility methods to convert MParticle events to Branch events
 *
 */
internal class BranchUtil {
    internal enum class MPEventKeys {
        Position,
        Amount,
        ScreenName,
        Impression,
        ProductListName,
        ProductListSource,
        CheckoutOptions,
        CheckoutStep,
    }

    internal enum class ExtraBranchEventKeys {
        ProductCategory,
    }

    private val branchMParticleEventNames = HashMap<String?, String>()
    private var branchMParticlePromotionEventNames = HashMap<String?, String>()

    private fun createBranchEventFromEventName(eventName: String?): BranchEvent {
        val branchEvent: BranchEvent
        val branchStandardEvent = branchMParticleEventNames[eventName]
        branchEvent = branchStandardEvent?.let { BranchEvent(it) } ?: BranchEvent(eventName)
        branchEvent.setDescription(eventName)
        return branchEvent
    }

    private fun createBranchEventFromPromotionEventName(eventName: String?): BranchEvent {
        val branchEvent: BranchEvent
        val branchStandardEvent = branchMParticlePromotionEventNames[eventName]
        branchEvent = branchStandardEvent?.let { BranchEvent(it) } ?: BranchEvent(eventName)
        branchEvent.setDescription(eventName)
        return branchEvent
    }

    internal class MapReader(
        mapObject: Map<String, String>?,
    ) {
        private lateinit var mapObj: MutableMap<String, String>

        fun readOutString(key: String): String? = mapObj.remove(key)

        fun readOutDouble(key: String): Double? {
            var value: Double? = null
            try {
                value = mapObj[key]?.toDouble()
                mapObj.remove(key)
            } catch (ignore: Exception) {
            }
            return value
        }

        val map: Map<String, String>
            get() = mapObj

        init {

            if (!mapObject.isNullOrEmpty()) {
                this.mapObj = HashMap(mapObject)
            }
        }
    }

    // Region Translate MPEvents
    fun createBranchEventFromMPEvent(mpEvent: MPEvent): BranchEvent {
        val branchEvent = BranchEvent(mpEvent.eventName)
        branchEvent.setDescription(mpEvent.eventName)
        val buo = BranchUniversalObject()
        branchEvent.addContentItems(buo)
        // Apply event category
        if (!TextUtils.isEmpty(mpEvent.category)) {
            translateEventCategory(buo, mpEvent.category)
        }
        // Apply event name
        if (!TextUtils.isEmpty(mpEvent.eventName)) {
            buo.title = mpEvent.eventName
        }
        if (mpEvent.customAttributeStrings != null) {
            updateEventWithInfo(branchEvent, mpEvent.customAttributeStrings)
        }
        return branchEvent
    }

    private fun updateEventWithInfo(
        event: BranchEvent,
        info: Map<String, String>?,
    ) {
        val mapReader = MapReader(info)
        updateBranchEventWithCustomData(event, mapReader.map)
    }

    // End Region Translate MPEvents
    // Region Translate CommerceEvents
    fun createBranchEventFromMPCommerceEvent(event: CommerceEvent): BranchEvent {
        val branchEvent: BranchEvent =
            if (event.productAction != null) {
                createBranchEventFromEventName(event.productAction)
            } else if (event.promotionAction != null) {
                createBranchEventFromPromotionEventName(event.promotionAction)
            } else {
                createBranchEventFromEventName(MPEventKeys.Impression.name)
            }
        // Add all the products in the product list to Branch event
        if (event.products != null) {
            val additionalMetadata = HashMap<String, String?>()
            if (!TextUtils.isEmpty(event.productListName)) {
                additionalMetadata[MPEventKeys.ProductListName.name] = event.productListName
            }
            if (!TextUtils.isEmpty(event.productListSource)) {
                additionalMetadata[MPEventKeys.ProductListSource.name] = event.productListSource
            }
            addProductListToBranchEvent(branchEvent, event.products, event, additionalMetadata)
        }

        // Add all impressions to the Branch Event
        event.impressions?.let {
            for (impression in it) {
                val additionalMetadata = HashMap<String, String?>()
                if (!TextUtils.isEmpty(impression.listName)) {
                    additionalMetadata[MPEventKeys.Impression.name] = impression.listName
                }
                addProductListToBranchEvent(
                    branchEvent,
                    impression.products,
                    event,
                    additionalMetadata,
                )
            }
        }
        event.transactionAttributes?.let {
            updateBranchEventWithTransactionAttributes(branchEvent, it)
        }
        event.customAttributeStrings?.let {
            updateBranchEventWithCustomData(branchEvent, it)
        }
        if (!TextUtils.isEmpty(event.productListName)) {
            branchEvent.addCustomDataProperty(
                MPEventKeys.ProductListName.name,
                event.productListName,
            )
        }
        if (!TextUtils.isEmpty(event.productListSource)) {
            branchEvent.addCustomDataProperty(
                MPEventKeys.ProductListSource.name,
                event.productListSource,
            )
        }
        if (!TextUtils.isEmpty(event.checkoutOptions)) {
            branchEvent.addCustomDataProperty(
                MPEventKeys.CheckoutOptions.name,
                event.checkoutOptions,
            )
        }
        if (!TextUtils.isEmpty(event.screen)) {
            branchEvent.addCustomDataProperty(MPEventKeys.ScreenName.name, event.screen)
        }
        if (event.checkoutStep != null) {
            try {
                branchEvent.addCustomDataProperty(
                    MPEventKeys.CheckoutStep.name,
                    event.checkoutStep.toString(),
                )
            } catch (ignore: Exception) {
            }
        }
        if (!TextUtils.isEmpty(event.currency)) {
            branchEvent.setCurrency(CurrencyType.getValue(event.currency))
        }
        return branchEvent
    }

    private fun addProductListToBranchEvent(
        branchEvent: BranchEvent,
        products: List<Product>?,
        event: CommerceEvent,
        additionalMetadata: Map<String, String?>,
    ) {
        if (products != null) {
            for (product in products) {
                branchEvent.addContentItems(
                    createBranchUniversalObjectFromMProduct(
                        product,
                        event,
                        additionalMetadata,
                    ),
                )
            }
        }
    }

    private fun createBranchUniversalObjectFromMProduct(
        product: Product,
        event: CommerceEvent,
        additionalMetadata: Map<String, String?>?,
    ): BranchUniversalObject {
        val buo = BranchUniversalObject()
        if (!TextUtils.isEmpty(product.brand)) {
            buo.contentMetadata.setProductBrand(product.brand)
        }
        if (!TextUtils.isEmpty(product.category)) {
            translateEventCategory(buo, product.category)
        }
        if (!TextUtils.isEmpty(product.couponCode)) {
            buo.contentMetadata.addCustomMetadata(Defines.Jsonkey.Coupon.key, product.couponCode)
        }
        if (!TextUtils.isEmpty(product.name)) {
            buo.contentMetadata.setProductName(product.name)
        }
        if (!TextUtils.isEmpty(product.variant)) {
            buo.contentMetadata.setProductVariant(product.variant)
        }
        if (!TextUtils.isEmpty(product.sku)) {
            buo.contentMetadata.setSku(product.sku)
        }
        if (product.position != null) {
            buo.contentMetadata.addCustomMetadata(
                MPEventKeys.Position.name,
                (product.position).toString(),
            )
        }
        buo.contentMetadata.setPrice(product.unitPrice, CurrencyType.getValue(event.currency))
        buo.contentMetadata.setQuantity(product.quantity)
        buo.contentMetadata.addCustomMetadata(
            MPEventKeys.Amount.name,
            product.totalAmount.toString(),
        )
        product.customAttributes?.let {
            addCustomDataToBranchUniversalObject(buo, it)
        }
        additionalMetadata?.let { addCustomDataToBranchUniversalObject(buo, it) }
        return buo
    }

    private fun addCustomDataToBranchUniversalObject(
        buo: BranchUniversalObject,
        customAttr: Map<String, String?>,
    ) {
        val contentMetadata = buo.contentMetadata
        for (key in customAttr.keys) {
            contentMetadata.addCustomMetadata(key, customAttr[key])
        }
    }

    private fun updateBranchEventWithTransactionAttributes(
        event: BranchEvent,
        transAttr: TransactionAttributes,
    ) {
        if (!TextUtils.isEmpty(transAttr.affiliation)) {
            event.setAffiliation(transAttr.affiliation)
        }
        if (!TextUtils.isEmpty(transAttr.couponCode)) {
            event.setCoupon(transAttr.couponCode)
        }
        if (!TextUtils.isEmpty(transAttr.id)) {
            event.setTransactionID(transAttr.id)
        }
        transAttr.revenue?.let {
            event.setRevenue(it)
        }
        transAttr.shipping?.let {
            event.setShipping(it)
        }
        transAttr.tax?.let {
            event.setTax(it)
        }
    }

    fun updateBranchEventWithCustomData(
        branchEvent: BranchEvent,
        eventAttributes: Map<String, String>,
    ) {
        for (key in eventAttributes.keys) {
            branchEvent.addCustomDataProperty(key, eventAttributes[key])
            if (key == "customer_event_alias") {
                branchEvent.setCustomerEventAlias(eventAttributes[key])
            }
        }
    } // End Region Translate CommerceEvents

    companion object {
        /**
         * Translate the given MPEvent / Commerce event to [ProductCategory] and add to the BUO
         *
         * @param buo          BUO representing content for the event
         * @param categoryName MPEvent / Commerce event category
         */
        private fun translateEventCategory(
            buo: BranchUniversalObject,
            categoryName: String?,
        ) {
            val category = ProductCategory.getValue(categoryName)
            if (category != null) {
                buo.contentMetadata.setProductCategory(category)
            } else {
                buo.contentMetadata.addCustomMetadata(
                    ExtraBranchEventKeys.ProductCategory.name,
                    categoryName,
                )
            }
        }
    }

    init {
        // Mapping MParticle Commerce Event names to possible matches in Branch events
        branchMParticleEventNames[Product.ADD_TO_CART] = BRANCH_STANDARD_EVENT.ADD_TO_CART.getName()
        branchMParticleEventNames[Product.REMOVE_FROM_CART] = "REMOVE_FROM_CART"
        branchMParticleEventNames[Product.ADD_TO_WISHLIST] = BRANCH_STANDARD_EVENT.ADD_TO_WISHLIST.getName()
        branchMParticleEventNames[Product.REMOVE_FROM_WISHLIST] = "REMOVE_FROM_WISHLIST"
        branchMParticleEventNames[Product.CHECKOUT] = BRANCH_STANDARD_EVENT.INITIATE_PURCHASE.getName()
        branchMParticleEventNames[Product.CLICK] = "CLICK_ITEM"
        branchMParticleEventNames[Product.PURCHASE] = BRANCH_STANDARD_EVENT.PURCHASE.getName()
        branchMParticleEventNames[Product.DETAIL] = BRANCH_STANDARD_EVENT.VIEW_ITEM.getName()
        branchMParticleEventNames[Product.CHECKOUT_OPTION] = "PURCHASE_OPTION"
        branchMParticleEventNames[Product.REFUND] = "REFUND"
        branchMParticleEventNames[MPEventKeys.Impression.name] = "IMPRESSION"
        branchMParticlePromotionEventNames = HashMap()
        branchMParticlePromotionEventNames[Promotion.VIEW] = "VIEW_PROMOTION"
        branchMParticlePromotionEventNames[Promotion.CLICK] = "CLICK_PROMOTION"
    }
}
