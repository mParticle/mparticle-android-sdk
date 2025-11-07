package com.mparticle

/**
 * ### Optional callback events for when the view loads and unloads.
 */
interface MpRoktEventCallback {
    /**
     * onLoad Callback will be triggered immediately when the View displays.
     */
    fun onLoad()

    /**
     * onUnLoad Callback will be triggered if the View failed to show or it closed.
     */
    fun onUnload(reason: UnloadReasons)

    /**
     * onShouldShowLoadingIndicator callback will be triggered if View starts processing.
     */
    fun onShouldShowLoadingIndicator()

    /**
     * onShouldHideLoadingIndicator callback will be triggered if View ends processing.
     */
    fun onShouldHideLoadingIndicator()
}

/**
 * Enum representing the reasons for unloading.
 */
enum class UnloadReasons {
    /**
     * Called when there are no offers to display so the view does not get loaded in.
     */
    NO_OFFERS,

    /**
     * View has been rendered and has been completed.
     */
    FINISHED,

    /**
     * Operation to fetch view took too long to resolve.
     */
    TIMEOUT,

    /**
     * Some error has occurred regarding the network.
     */
    NETWORK_ERROR,

    /**
     * View is empty.
     */
    NO_WIDGET,

    /**
     * Init request was not successful.
     */
    INIT_FAILED,

    /**
     * Placeholder string mismatch.
     */
    UNKNOWN_PLACEHOLDER,

    /**
     * Catch-all for all issues.
     */
    UNKNOWN,

    ;

    companion object {
        /**
         * Returns the enum constant matching the provided string.
         * If no match is found, UNKNOWN is returned.
         *
         * @param value the name of the enum constant to look up
         * @return the corresponding UnloadReasons constant or UNKNOWN if no match is found
         */
        fun from(value: String): UnloadReasons =
            try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                UNKNOWN
            }
    }
}
