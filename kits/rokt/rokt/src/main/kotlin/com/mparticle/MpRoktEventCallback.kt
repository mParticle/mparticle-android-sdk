package com.mparticle

import com.rokt.roktsdk.Rokt

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
    fun onUnload(reason: Rokt.UnloadReasons)

    /**
     * onShouldShowLoadingIndicator callback will be triggered if View starts processing.
     */
    fun onShouldShowLoadingIndicator()

    /**
     * onShouldHideLoadingIndicator callback will be triggered if View ends processing.
     */
    fun onShouldHideLoadingIndicator()
}
