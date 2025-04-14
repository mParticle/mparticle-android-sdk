package com.mparticle.modernization

import com.mparticle.modernization.core.MParticleMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun MParticleMediator.launch(block: suspend CoroutineScope.() -> Unit) {
    this.coroutineScope.launch(this.coroutineDispatcher) { block }
}
