package com.mparticle.commerce

import com.mparticle.BaseEvent
import com.mparticle.commerce.Product.ProductConstant
import com.mparticle.commerce.Promotion.PromotionConstant
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.internal.messages.BaseMPMessageBuilder
import com.mparticle.internal.messages.MPCommerceMessage
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class CommerceEvent private constructor(builder: Builder) : BaseEvent(Type.COMMERCE_EVENT) {
    private var mImpressions: List<Impression>? = null
    private var mProductAction: String? = null
    private var mPromotionAction: String? = null
    private var promotionList: MutableList<Promotion?>? = mutableListOf()
    private var mEventName: String? = null
    private var productList: MutableList<Product?>? = mutableListOf()
    private var mCheckoutStep: Int? = null
    private var mCheckoutOptions: String? = null
    private var mProductListName: String? = null
    private var mProductListSource: String? = null
    private var mCurrency: String? = null
    private var mTransactionAttributes: TransactionAttributes? = null
    private var mScreen: String? = null
    private var mNonIteraction: Boolean? = null

    init {
        mProductAction = builder.mProductAction
        mPromotionAction = builder.mPromotionAction
        customAttributes = builder.customAttributes
        if (!MPUtility.isEmpty(builder.promotionList)) {
            //builder.promotionList?.removeIf{it == null}
            builder.promotionList?.removeAll { it == null }
        }
        promotionList = builder.promotionList
        if (!MPUtility.isEmpty(builder.productList)) {
            productList = builder.productList?.let { LinkedList(it) }
            productList?.removeAll { it == null }
        }
        mCheckoutStep = builder.mCheckoutStep
        mCheckoutOptions = builder.mCheckoutOptions
        mProductListName = builder.mProductListName
        mProductListSource = builder.mProductListSource
        mCurrency = builder.mCurrency
        mTransactionAttributes = builder.mTransactionAttributes
        mScreen = builder.mScreen
        mImpressions = builder.mImpressions
        mNonIteraction = builder.mNonIteraction
        mEventName = builder.mEventName

        builder.mCustomFlags?.let {
            customFlags = builder.mCustomFlags
        }
        builder.mShouldUploadEvent?.let {
            isShouldUploadEvent = builder.mShouldUploadEvent!!
        }
        checkNullabilty()
        when {
            mProductAction != null -> handleMProductActionNotNull()
            mPromotionAction != null -> handleMPromotionActionNotNull()
            else -> handlePromotionActionIsNull()
        }


        checkMTransactionAttributes()
    }

    private fun checkMTransactionAttributes() {
        if (mTransactionAttributes == null || mTransactionAttributes!!.revenue == null) {
            var transactionRevenue = 0.0
            mTransactionAttributes?.let {
                val shipping: Double = if (it.shipping == null) 0.0 else {
                    it.shipping!!
                }
                val tax: Double = if (it.tax == null) 0.0 else {
                    it.tax!!
                }
                transactionRevenue += shipping
                transactionRevenue += tax
            }

            if (mTransactionAttributes == null) {
                mTransactionAttributes = TransactionAttributes()
            }

            productList?.let {
                for (product in it) {
                    product?.let {
                        var price = product.unitPrice
                        price *= product.quantity
                        transactionRevenue += price
                    }
                }
            }
            mTransactionAttributes?.revenue = transactionRevenue
        }
    }

    private fun handlePromotionActionIsNull() {
        if (!productList.isNullOrEmpty()) {
            Logger.error("Impression CommerceEvent should not contain Products.")
        }
        if (!promotionList.isNullOrEmpty()) {
            Logger.error("Impression CommerceEvent should not contain Promotions.")
        }
    }

    private fun handleMPromotionActionNotNull() {
        if (!productList.isNullOrEmpty()) {
            Logger.error("Promotion CommerceEvent should not contain Products.")
        }
        if (mPromotionAction != Promotion.VIEW
            && mPromotionAction != Promotion.CLICK
        ) {
            Logger.error("CommerceEvent created with unrecognized Promotion action: $mProductAction")
        }
    }

    private fun handleMProductActionNotNull() {
        if (mProductAction.equals(Product.PURCHASE, ignoreCase = true) ||
            mProductAction.equals(Product.REFUND, ignoreCase = true)
        ) {
            if (mTransactionAttributes == null || MPUtility.isEmpty(mTransactionAttributes!!.id)) {
                val message =
                    "CommerceEvent with action $mProductAction must include a TransactionAttributes object with a transaction ID."
                Logger.error(message)
            }
        }
        if (!promotionList.isNullOrEmpty()) {
            Logger.error("Product CommerceEvent should not contain Promotions.")
        }
        if (mProductAction != Product.ADD_TO_CART
            && mProductAction != Product.ADD_TO_WISHLIST
            && mProductAction != Product.CHECKOUT
            && mProductAction != Product.CHECKOUT_OPTION
            && mProductAction != Product.CLICK
            && mProductAction != Product.DETAIL
            && mProductAction != Product.PURCHASE
            && mProductAction != Product.REFUND
            && mProductAction != Product.REMOVE_FROM_CART
            && mProductAction != Product.REMOVE_FROM_WISHLIST
        ) {
            Logger.error("CommerceEvent created with unrecognized Product action: $mProductAction")
        }
    }

    private fun checkNullabilty() {
        if (mProductAction.isNullOrEmpty()
            && mPromotionAction.isNullOrEmpty()
            && mImpressions.isNullOrEmpty()
        ) {
            Logger.error("CommerceEvent must be created with either a product action, promotion action, or an impression.")
        }
    }


    override fun toString(): String {
        try {
            val eventJson = JSONObject()
            if (mScreen != null) {
                eventJson.put("sn", mScreen)
            }
            if (mNonIteraction != null) {
                eventJson.put("ni", mNonIteraction == true)
            }
            if (mProductAction != null) {
                checkProductionAction(eventJson)
            } else {
                handleMProductionActionNull(eventJson)
            }
            if (!mImpressions.isNullOrEmpty()) {
                handleImpressions(eventJson)
            }
            return eventJson.toString()
        } catch (jse: JSONException) {
        }
        return super.toString()
    }

    private fun handleImpressions(eventJson: JSONObject) {
        val impressions = JSONArray()
        for (impression in mImpressions!!) {
            val impressionJson = JSONObject()
            if (impression.listName != null) {
                impressionJson.put("pil", impression.listName)
            }
            if (impression.products != null && impression.products.size > 0) {
                val productsJson = JSONArray()
                impressionJson.put("pl", productsJson)
                for (product in impression.products) {
                    productsJson.put(JSONObject(product.toString()))
                }
            }
            if (impressionJson.length() > 0) {
                impressions.put(impressionJson)
            }
        }
        if (impressions.length() > 0) {
            eventJson.put("pi", impressions)
        }
    }

    private fun handleMProductionActionNull(eventJson: JSONObject) {
        val promotionAction = JSONObject()
        eventJson.put("pm", promotionAction)
        promotionAction.put("an", mPromotionAction)
        if (promotionList != null && promotionList!!.size > 0) {
            val promotions = JSONArray()
            for (i in promotionList!!.indices) {
                promotions.put(JSONObject(promotionList!![i].toString()))
            }
            promotionAction.put("pl", promotions)
        }
    }

    private fun checkProductionAction(eventJson: JSONObject) {
        val productAction = JSONObject()
        eventJson.put("pd", productAction)
        productAction.put("an", mProductAction)
        if (mCheckoutStep != null) {
            productAction.put("cs", mCheckoutStep)
        }
        if (mCheckoutOptions != null) {
            productAction.put("co", mCheckoutOptions)
        }
        if (mProductListName != null) {
            productAction.put("pal", mProductListName)
        }
        if (mProductListSource != null) {
            productAction.put("pls", mProductListSource)
        }
        if (mTransactionAttributes != null) {
            if (mTransactionAttributes!!.id != null) {
                productAction.put("ti", mTransactionAttributes!!.id)
            }
            if (mTransactionAttributes!!.affiliation != null) {
                productAction.put("ta", mTransactionAttributes!!.affiliation)
            }
            if (mTransactionAttributes!!.revenue != null) {
                productAction.put("tr", mTransactionAttributes!!.revenue)
            }
            if (mTransactionAttributes!!.tax != null) {
                productAction.put("tt", mTransactionAttributes!!.tax)
            }
            if (mTransactionAttributes!!.shipping != null) {
                productAction.put("ts", mTransactionAttributes!!.shipping)
            }
            if (mTransactionAttributes!!.couponCode != null) {
                productAction.put("tcc", mTransactionAttributes!!.couponCode)
            }
        }
        if (productList != null && productList!!.size > 0) {
            val products = JSONArray()
            for (i in productList!!.indices) {
                products.put(JSONObject(productList!![i].toString()))
            }
            productAction.put("pl", products)
        }
    }

    /**
     * Retrieve the Screen where the event occurred.
     *
     * @return the String descriptor/name of the Screen where this event occurred, or null if this is not set.
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder.screen
     */
    fun getScreen(): String? {
        return mScreen
    }

    /**
     * Retrieve the boolean indicating if the event was triggered by a user.
     *
     * @return a Boolean indicating if this event was triggered by a user, or null if not set.
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder.nonInteraction
     */
    fun getNonInteraction(): Boolean? {
        return mNonIteraction
    }

    /**
     * Retrieve the Product action of the event. CommerceEvents are either Product, Promotion, or Impression based. The Product Action
     * will be null in the case of Promotion and Impression CommerceEvents.
     *
     * @return a String indicating the Product action, or null if this is not a Product-based CommerceEvent.
     *
     * @see `[](CommerceEvent.Builder.html.CommerceEvent.Builder
@see Product
)` */
    val productAction
        get() = mProductAction


    /**
     * Retrieve the Checkout Step of the CommerceEvent. This should typically only be set in the case of a [Product.CHECKOUT] CommerceEvent.
     *
     * @return an Integer representing the step, or null if none is set.
     */
    val checkoutStep
        get() = mCheckoutStep


    /**
     * Retrieve the Checkout options of the CommerceEvent. This describes custom options that a user may select at particular steps in the checkout process.
     *
     * @return a String describing any checkout options, or null if none are set.
     */
    val checkoutOptions
        get() = mCheckoutOptions


    /**
     * Retrieve the Product List Name associated with the CommerceEvent. This value should be set for [Product.DETAIL] and [Product.CLICK] CommerceEvents.
     *
     * @return the product list name, or null if not set.
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder.productListName
     */
    val productListName
        get() = mProductListName


    /**
     * Retrieve the product list source associated with the CommerceEvent. This value should be set for [Product.DETAIL] and [Product.CLICK] CommerceEvents.
     *
     * @return the product list source, or null if not set.
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder.productListSource
     */
    val productListSource
        get() = mProductListSource


    /**
     * Retrieve the [TransactionAttributes] object associated with the CommerceEvent.
     *
     * @return the TransactionAttributes object, or null if not set.
     *
     * @see com.mparticle.commerce.CommerceEvent.Builder.transactionAttributes
     */
    val transactionAttributes
        get() = mTransactionAttributes


    /**
     * Retrieve the list of Products to which the CommerceEvent applies. This should only be set for Product-type CommerceEvents.
     *
     * @return the list of Products, or null if not set.
     *
     * @see `[](CommerceEvent.Builder.html.CommerceEvent.Builder
@see com.mparticle.commerce.CommerceEvent.Builder.addProduct
@see com.mparticle.commerce.CommerceEvent.Builder.products
)` */
    val products: List<Product?>?
        get() {
            return if (productList == null) {
                null
            } else productList
        }

    /**
     * Retrieve the Promotion action of the CommerceEvent. CommerceEvents are either Product, Promotion, or Impression based. The Promotion Action
     * will be null in the case of Product and Impression CommerceEvents.
     *
     * @return a String indicating the Promotion action, or null if this is not a Promotion-based CommerceEvent.
     *
     * @see `[](CommerceEvent.Builder.html.CommerceEvent.Builder
@see Promotion for supported product actions.
)` */
    val promotionAction
        get() = mPromotionAction


    /**
     * Retrieve the [Promotion] list associated with the CommerceEvent.
     *
     * @return returns an unmodifiable List of Promotions, or null if this is a [Product] or [Impression] based [CommerceEvent].
     *
     * @see `[](CommerceEvent.Builder.html.CommerceEvent.Builder
@see com.mparticle.commerce.CommerceEvent.Builder.addPromotion
@see com.mparticle.commerce.CommerceEvent.Builder.promotions
)` */
    val promotions: List<Promotion?>?
        get() {
            return if (promotionList == null) {
                null
            } else promotionList
        }

    override fun equals(other: Any?): Boolean {
        return other != null && other.toString() == toString()
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    /**
     * Retrieve the [Impression] list associated with the CommerceEvent.
     *
     * @return returns an unmodifiable List of Impressions, or null if this is a [Product] or [Promotion] based [CommerceEvent].
     *
     * @see `[](CommerceEvent.Builder.html.CommerceEvent.Builder
@see com.mparticle.commerce.CommerceEvent.Builder.addImpression
@see com.mparticle.commerce.CommerceEvent.Builder.impressions
)` */
    val impressions: List<Impression?>?
        get() {
            return if (mImpressions == null) {
                null
            } else mImpressions
        }

    /**
     * Retrieve the currency code associated with the CommerceEvent.
     *
     * @return returns a String representing the currency code, or null if not set.
     */
    val currency
        get() = mCurrency


    /**
     * Retrieve the event name associated with the CommerceEvent. Most service providers do not require that this value is set.
     *
     * @return the name associated with the CommerceEvent, or null if not set.
     */
    val eventName
        get() = mEventName


    override fun getMessage(): BaseMPMessageBuilder? {
        return MPCommerceMessage.Builder(this)
            .flags(customFlags)
    }

    /**
     * The Builder class for [CommerceEvent]. Use this class to create immutable CommerceEvents that can then be logged.
     *
     * There are 3 types of [CommerceEvent]:
     *
     *  * [Product]
     *  * [Promotion]
     *  * [Impression]
     *
     * <br></br>
     * This class provides a constructor for each type and will verify the contents of the event when [Builder.build] is called.
     * <br></br><br></br>
     * **Sample Product event**
     * <br></br>
     * <pre>
     * `Product product = new Product.Builder("Foo name", "Foo sku", 100.00).quantity(4).build();
     * CommerceEvent event = new CommerceEvent.Builder(Product.PURCHASE, product)
     * .transactionAttributes(new TransactionAttributes().setId("bar-transaction-id")
     * .setRevenue(product.getTotalAmount()))
     * .build();
     * MParticle.getInstance().logEvent(event);
    ` *
    </pre> *
     * <br></br>
     * <br></br>
     * **Sample Promotion event**<br></br>
     * <pre>
     * `Promotion promotion = new Promotion().setCreative("foo-creative").setName("bar campaign");
     * CommerceEvent event = new CommerceEvent.Builder(Promotion.VIEW, promotion).build();
     * MParticle.getInstance().logEvent(event);
    ` *
    </pre> *
     * <br></br>
     * <br></br>
     * **Sample Impression event**<br></br>
     * <pre>
     * `Product product = new Product.Builder("Foo name", "Foo sku", 100.00).quantity(4).build();
     * Impression impression = new Impression("foo-list-name", product);
     * CommerceEvent event = new CommerceEvent.Builder(impression).build();
     * MParticle.getInstance().logEvent(event);
    ` *
    </pre> *
     */
    class Builder {
        var mProductAction: String?
        var mPromotionAction: String?
        var customAttributes: Map<String, String>? = null
        var promotionList: MutableList<Promotion?>? = null
        var productList: MutableList<Product?>? = null
        var mCheckoutStep: Int? = null
        var mCheckoutOptions: String? = null
        var mProductListName: String? = null
        var mProductListSource: String? = null
        var mCurrency: String? = null
        var mTransactionAttributes: TransactionAttributes? = null
        var mScreen: String? = null
        var mNonIteraction: Boolean? = null
        var mImpressions: MutableList<Impression>? = null
        var mEventName: String? = null
        var mCustomFlags: MutableMap<String?, MutableList<String?>>? = null
        var mShouldUploadEvent: Boolean? = null

        internal constructor() {
            mPromotionAction = null
            mProductAction = mPromotionAction
        }

        /**
         * Constructor for a [Product]-based CommerceEvent.
         *
         * @param productAction a String representing the action that the user performed. This must be one of the String constants defined by the [Product] class. Must not be null.
         * @param product at [Product] object to associate with the given action. Must not be null.
         *
         *
         * @see Product.CLICK
         *
         * @see Product.DETAIL
         *
         * @see Product.CHECKOUT
         *
         * @see Product.ADD_TO_CART
         *
         * @see Product.REMOVE_FROM_CART
         *
         * @see Product.ADD_TO_WISHLIST
         *
         * @see Product.REMOVE_FROM_WISHLIST
         *
         * @see Product.CHECKOUT
         *
         * @see Product.CHECKOUT_OPTION
         *
         * @see Product.PURCHASE
         */
        constructor(@ProductConstant productAction: String, product: Product?) {
            mProductAction = productAction
            mPromotionAction = null
            addProduct(product)
        }

        /**
         * Constructor for a [Promotion]-based CommerceEvent.
         *
         * @param promotionAction a String representing the action that use user performed. This must be on the String constants defined by the [Promotion] class. Must not be null.
         * @param promotion at least 1 [Promotion] object to associate with the given action. Must not be null.
         *
         * @see Promotion.CLICK
         *
         * @see Promotion.VIEW
         */
        constructor(@PromotionConstant promotionAction: String, promotion: Promotion) {
            mProductAction = null
            mPromotionAction = promotionAction
            addPromotion(promotion)
        }

        /**
         * Constructor for a [Impression]-based CommerceEvent.
         *
         * @param impression the impression to associate with this event. Must not be null.
         */
        constructor(impression: Impression) {
            addImpression(impression)
            mPromotionAction = null
            mProductAction = null
        }

        /**
         * Convenience copy-constructor. Use this to convert or mutate a given CommerceEvent.
         *
         * @param event an existing CommerceEvent. Must not be null.
         */
        constructor(event: CommerceEvent) {
            mProductAction = event.productAction
            mPromotionAction = event.promotionAction
            if (event.customAttributes != null) {
                val shallowCopy: MutableMap<String, String> = HashMap()
                shallowCopy.putAll(event.customAttributes!!)
                customAttributes = shallowCopy
            }
            if (event.promotions != null) {
                for (promotion in event.promotions!!) {
                    addPromotion(Promotion(promotion!!))
                }
            }
            if (event.products != null) {
                for (product in event.products!!) {
                    addProduct(Product.Builder(product!!).build())
                }
            }
            mCheckoutStep = event.checkoutStep
            mCheckoutOptions = event.checkoutOptions
            mProductListName = event.productListName
            mProductListSource = event.productListSource
            mCurrency = event.currency
            if (event.transactionAttributes != null) {
                mTransactionAttributes = TransactionAttributes(event.transactionAttributes!!)
            }
            mScreen = event.mScreen
            mNonIteraction = event.mNonIteraction
            if (event.impressions != null) {
                for (impression in event.impressions!!) {
                    addImpression(Impression(impression!!))
                }
            }
            mEventName = event.eventName
            mCustomFlags = event.customFlags
            mShouldUploadEvent = event.isShouldUploadEvent
        }

        /**
         * Set the screen to associate with this event.
         *
         * @param screenName a String name or description of the screen where this event occurred.
         * @return returns this Builder for easy method chaining.
         */
        fun screen(screenName: String?): Builder {
            mScreen = screenName
            return this
        }

        /**
         * Add a [Product] to this CommerceEvent.
         *
         * *This should only be called for [Product]-based CommerceEvents created with `[Builder(java.lang.String, com.mparticle.commerce.Product)](CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Product))` *.
         *
         * @param product the [Product] to add to this CommerceEvent.
         * @return returns this Builder for easy method chaining.
         */
        fun addProduct(product: Product?): Builder {
            if (productList == null) {
                productList = LinkedList()
            }
            productList!!.add(product)
            return this
        }

        /**
         * Associate a [TransactionAttributes] object with this event. This will typically be used with [Product]-based CommerceEvents.
         *
         * *For [Product.PURCHASE] and [Product.REFUND] CommerceEvents, this is required to be set.*
         *
         * @param attributes the [TransactionAttributes] object
         * @return returns this Builder for easy method chaining.
         */
        fun transactionAttributes(attributes: TransactionAttributes): Builder {
            mTransactionAttributes = attributes
            return this
        }

        /**
         * Set the ISO 4217 currency code to associate with this event.
         *
         * @param currency an ISO 4217 String
         * @return returns this Builder for easy method chaining.
         */
        fun currency(currency: String?): Builder {
            mCurrency = currency
            return this
        }

        /**
         * Set this CommerceEvent to be measured as non-user-triggered.
         *
         * @param userTriggered a Boolean indicating if a user actually performed this event
         * @return returns this Builder for easy method chaining.
         */
        fun nonInteraction(userTriggered: Boolean): Builder {
            mNonIteraction = userTriggered
            return this
        }

        /**
         * Associate a Map of custom attributes with this event.
         *
         * @param attributes the Map of attributes
         * @return returns this Builder for easy method chaining.
         */
        fun customAttributes(attributes: Map<String, String>?): Builder {
            customAttributes = attributes
            return this
        }

        /**
         * Add a custom flag to this event. Flag keys can have multiple values - if the provided flag key already has an associated
         * value, the value will be appended.
         *
         * @param key (required) a flag key, retrieve this from the mParticle docs or solution team for your intended services(s)
         * @param value (required) a flag value to be send to the service indicated by the flag key
         * @return returns this builder for easy method chaining
         */
        fun addCustomFlag(key: String?, value: String?): Builder {
            if (mCustomFlags == null) {
                mCustomFlags = HashMap()
            }
            if (!mCustomFlags!!.containsKey(key)) {
                mCustomFlags!![key] = LinkedList()
            }
            mCustomFlags!![key]!!.add(value)
            return this
        }

        /**
         * Bulk add custom flags to this event. This will replace any flags previously set via [CommerceEvent.Builder.addCustomFlag]
         *
         * @param customFlags (required) a map containing the custom flags for the CommerceEvent
         * @return returns this builder for easy method chaining
         */
        fun customFlags(customFlags: MutableMap<String?, MutableList<String?>>?): Builder {
            mCustomFlags = customFlags
            return this
        }

        /**
         * Add a [Promotion] to this CommerceEvent.
         *
         * *This should only be called for [Promotion]-based CommerceEvents created with `[Builder(java.lang.String, com.mparticle.commerce.Promotion)](CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Promotion))`*
         *
         * @param promotion the [Promotion] to add to this CommerceEvent.
         * @return returns this Builder for easy method chaining.
         */
        fun addPromotion(promotion: Promotion): Builder {
            if (promotionList == null) {
                promotionList = LinkedList()
            }
            promotionList!!.add(promotion)
            return this
        }

        /**
         * Set the checkout step of this event. Should be used with the [Product.CHECKOUT] and [Product.CHECKOUT_OPTION] actions.
         *
         * @param step the Integer step
         * @return returns this Builder for easy method chaining.
         */
        fun checkoutStep(step: Int?): Builder {
            if (step == null || step >= 0) {
                mCheckoutStep = step
            }
            return this
        }

        /**
         * Add a [Impression] to this CommerceEvent.
         *
         * *This should only be called for [Impression]-based CommerceEvents created with `[Builder(com.mparticle.commerce.Impression)](CommerceEvent.Builder.html#CommerceEvent.Builder(com.mparticle.commerce.Impression))` *
         *
         * @param impression the [Impression] to add to the CommerceEvent.
         * @return returns this Builder for easy method chaining.
         */
        fun addImpression(impression: Impression): Builder {
            if (impression != null) {
                if (mImpressions == null) {
                    mImpressions = LinkedList()
                }
                mImpressions!!.add(impression)
            }
            return this
        }

        /**
         * Set custom options to be associated with the event. Should be used with the [Product.CHECKOUT] and [Product.CHECKOUT_OPTION] actions.
         *
         *
         * @param options a String describing this checkout step
         * @return returns this Builder for easy method chaining.
         */
        fun checkoutOptions(options: String?): Builder {
            mCheckoutOptions = options
            return this
        }

        /**
         * Create the CommerceEvent. This method should be called when all of your desired datapoints have been added.
         *
         * When the SDK is in [com.mparticle.MParticle.Environment.Development] mode, this method will throw an [IllegalStateException] if you have created an invalid CommerceEvent, such as by combining
         * [Product] objects with [Promotion] objects. When in [com.mparticle.MParticle.Environment.Production] mode, errors will be logged to the console.
         *
         * @return returns the resulting immutable CommerceEvent to be logged
         *
         * @see `[](../MParticle.html.logEvent
)` */
        fun build(): CommerceEvent {
            return CommerceEvent(this)
        }

        /**
         * Set the list name with the Products of the CommerceEvent. This value should be used with the [Product.DETAIL] and [Product.CLICK] actions.
         *
         * @param listName a String name identifying the product list
         * @return returns this Builder for easy method chaining.
         */
        fun productListName(listName: String?): Builder {
            mProductListName = listName
            return this
        }

        /**
         * Set the list source name with the Products of the CommerceEvent. This value should be used with the [Product.DETAIL] and [Product.CLICK] actions.
         *
         * @param listSource a String name identifying the product's list source
         * @return returns this Builder for easy method chaining.
         */
        fun productListSource(listSource: String?): Builder {
            mProductListSource = listSource
            return this
        }

        /**
         * Overwrite the Products associated with the CommerceEvent. This should only be used with [Product]-based CommerceEvents created with `[Builder(java.lang.String, com.mparticle.commerce.Product)](CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Product))`
         *
         * @param products the List of products to associate with the CommerceEvent
         * @return returns this Builder for easy method chaining.
         */
        fun products(products: MutableList<Product?>): Builder {
            productList = products
            return this
        }

        /**
         * Overwrite the Impressions associated with the CommerceEvent. This should only be used with [Impression]-based CommerceEvents created with `[Builder(com.mparticle.commerce.Impression)](CommerceEvent.Builder.html#CommerceEvent.Builder(com.mparticle.commerce.Impression))`
         *
         * @param impressions the List of products to associate with the CommerceEvent
         * @return returns this Builder for easy method chaining.
         */
        fun impressions(impressions: MutableList<Impression>): Builder {
            mImpressions = impressions
            return this
        }

        /**
         * Overwrite the Promotions associated with the CommerceEvent. This should only be used with [Promotion]-based CommerceEvents created with `[Builder(java.lang.String, com.mparticle.commerce.Promotion)](CommerceEvent.Builder.html#CommerceEvent.Builder(java.lang.String,%20com.mparticle.commerce.Promotion))`
         *
         * @param promotions the List of products to associate with the CommerceEvent
         * @return returns this Builder for easy method chaining.
         */
        fun promotions(promotions: MutableList<Promotion?>): Builder {
            promotionList = promotions
            return this
        }

        /**
         * Private API used internally by the SDK.
         *
         */
        fun internalEventName(eventName: String?): Builder {
            mEventName = eventName
            return this
        }

        /**
         * Manually choose to skip uploading this event to mParticle server and only forward to kits.
         *
         * Note that if this method is not called, the default is to upload to mParticle as well as
         * forward to kits to match the previous behavior.
         *
         * @param shouldUploadEvent
         * @return returns this builder for easy method chaining
         */
        fun shouldUploadEvent(shouldUploadEvent: Boolean): Builder {
            mShouldUploadEvent = shouldUploadEvent
            return this
        }
    }
}

