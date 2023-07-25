package com.mparticle.internal

class KitsLoadedCallback {
    @Volatile
    private var onKitsLoadedRunnable: OnKitManagerLoaded? = null

    @Volatile
    private var loaded: Boolean = false

    fun setKitsLoaded() {
        synchronized(this) {
            if (!loaded) {
                loaded = true
                onKitsLoadedRunnable?.onKitManagerLoaded()
            }
        }
    }

    fun onKitsLoaded(callback: OnKitManagerLoaded) {
        synchronized(this) {
            if (loaded) {
                callback.onKitManagerLoaded()
            } else {
                onKitsLoadedRunnable = callback
            }
        }
    }
}

interface OnKitManagerLoaded {
    fun onKitManagerLoaded()
}
