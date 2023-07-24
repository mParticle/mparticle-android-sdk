package com.mparticle.internal

import com.mparticle.Configuration

internal class KitsLoadedListenerConfiguration(private var kitsLoadedListener: KitsLoadedListener) :
    Configuration<KitFrameworkWrapper> {
    override fun configures() = KitFrameworkWrapper::class.java
    override fun apply(kitFrameworkWrapper: KitFrameworkWrapper) =
        kitFrameworkWrapper.addKitsLoadedListener(kitsLoadedListener)
}
