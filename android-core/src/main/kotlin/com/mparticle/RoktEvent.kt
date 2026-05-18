package com.mparticle

// RoktEvent interface for handling events from the Rokt SDK.
sealed interface RoktEvent {
    /**
     * ShowLoadingIndicator event will be triggered before SDK calls Rokt backend
     */
    data object ShowLoadingIndicator : RoktEvent

    /**
     * HideLoadingIndicator event will be triggered when SDK obtains a success or failure from
     * Rokt backend
     */
    data object HideLoadingIndicator : RoktEvent

    /**
     * OfferEngagement event will be triggered if User engaged with the offer
     * @param identifier - identifier for the placement emitting the event
     */
    data class OfferEngagement(val identifier: String) : RoktEvent

    /**
     * PositiveEngagement event will be triggered if User positively engaged with the offer
     * @param identifier - identifier for the placement emitting the event
     */
    data class PositiveEngagement(val identifier: String) : RoktEvent

    /**
     * FirstPositiveEngagement event will be triggered when the user positively engaged with the offer first time
     * @param identifier - identifier for the placement emitting the event
     */
    data class FirstPositiveEngagement(val identifier: String) : RoktEvent

    /**
     * PlacementInteractive event will be triggered when placement has been rendered and is interactable
     * @param identifier - identifier for the placement emitting the event
     */
    data class PlacementInteractive(val identifier: String) : RoktEvent

    /**
     * PlacementReady event will be triggered when placement is ready to display but has not rendered content yet
     * @param identifier - identifier for the placement emitting the event
     */
    data class PlacementReady(val identifier: String) : RoktEvent

    /**
     * PlacementClosed event will be triggered when placement closes by user
     * @param identifier - identifier for the placement emitting the event
     */
    data class PlacementClosed(val identifier: String) : RoktEvent

    /**
     * PlacementCompleted event will be triggered when the offer progression moves to the end and no more
     * offer to display
     * @param identifier - identifier for the placement emitting the event
     */
    data class PlacementCompleted(val identifier: String) : RoktEvent

    /**
     * PlacementFailure event will be triggered when placement could not be displayed due to some failure
     * @param identifier - optional identifier for the placement emitting the event
     */
    data class PlacementFailure(val identifier: String? = null) : RoktEvent

    /**
     * InitComplete event will be triggered when SDK has finished initialization
     * @param success - true if init was successful
     */
    data class InitComplete(val success: Boolean) : RoktEvent

    /**
     * OpenUrl event will be triggered when user clicks on a link and the link target is set to Passthrough
     * @param identifier - identifier for the placement emitting the event
     * @param url - url to open
     */
    data class OpenUrl(val identifier: String, val url: String) : RoktEvent

    /**
     * CartItemInstantPurchase event will be triggered when the catalog item purchase is initiated
     * by the user
     * @property identifier The layout identifier.
     * @property cartItemId The cart item identifier.
     * @property catalogItemId The catalog item identifier.
     * @property currency The currency used for the purchase.
     * @property description The description of the cart item.
     * @property linkedProductId The linked product identifier.
     * @property totalPrice The total price of the cart item.
     * @property quantity The quantity of the cart item.
     * @property unitPrice The unit price of the cart item.
     */
    data class CartItemInstantPurchase(
        val identifier: String,
        val cartItemId: String,
        val catalogItemId: String,
        val currency: String,
        val description: String,
        val linkedProductId: String,
        val totalPrice: Double,
        val quantity: Int,
        val unitPrice: Double,
    ) : RoktEvent
}
