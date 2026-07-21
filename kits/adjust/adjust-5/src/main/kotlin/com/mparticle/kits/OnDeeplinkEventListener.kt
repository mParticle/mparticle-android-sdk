package com.mparticle.kits

import android.net.Uri

interface OnDeeplinkEventListener {
    fun launchReceivedDeeplink(deeplink: Uri?): Boolean
}
