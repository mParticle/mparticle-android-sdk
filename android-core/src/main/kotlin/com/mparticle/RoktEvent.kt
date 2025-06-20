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
     * @param placementId - identifier for the placement emitting the event
     */
    data class OfferEngagement(val placementId: String) : RoktEvent

    /**
     * PositiveEngagement event will be triggered if User positively engaged with the offer
     * @param placementId - identifier for the placement emitting the event
     */
    data class PositiveEngagement(val placementId: String) : RoktEvent

    /**
     * FirstPositiveEngagement event will be triggered when the user positively engaged with the offer first time
     * @param placementId - identifier for the placement emitting the event
     */
    data class FirstPositiveEngagement(val placementId: String) : RoktEvent

    /**
     * PlacementInteractive event will be triggered when placement has been rendered and is interactable
     * @param placementId - identifier for the placement emitting the event
     */
    data class PlacementInteractive(val placementId: String) : RoktEvent

    /**
     * PlacementReady event will be triggered when placement is ready to display but has not rendered content yet
     * @param placementId - identifier for the placement emitting the event
     */
    data class PlacementReady(val placementId: String) : RoktEvent

    /**
     * PlacementClosed event will be triggered when placement closes by user
     * @param placementId - identifier for the placement emitting the event
     */
    data class PlacementClosed(val placementId: String) : RoktEvent

    /**
     * PlacementCompleted event will be triggered when the offer progression moves to the end and no more
     * offer to display
     * @param placementId - identifier for the placement emitting the event
     */
    data class PlacementCompleted(val placementId: String) : RoktEvent

    /**
     * PlacementFailure event will be triggered when placement could not be displayed due to some failure
     * @param placementId - optional identifier for the placement emitting the event
     */
    data class PlacementFailure(val placementId: String? = null) : RoktEvent

    /**
     * InitComplete event will be triggered when SDK has finished initialization
     * @param success - true if init was successful
     */
    data class InitComplete(val success: Boolean) : RoktEvent

    /**
     * OpenUrl event will be triggered when user clicks on a link and the link target is set to Passthrough
     * @param placementId - identifier for the placement emitting the event
     * @param url - url to open
     */
    data class OpenUrl(val placementId: String, val url: String) : RoktEvent

    /**
     * CartItemInstantPurchase event will be triggered when the catalog item purchase is initiated
     * by the user
     * @property placementId The layout identifier.
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
        val placementId: String,
        val cartItemId: String,
        val catalogItemId: String,
        val currency: String,
        val description: String,
        val linkedProductId: String,
        val totalPrice: Double,
        val quantity: Int,
        val unitPrice: Double
    ) : RoktEvent
}
