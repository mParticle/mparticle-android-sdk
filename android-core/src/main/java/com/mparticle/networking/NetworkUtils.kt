package com.mparticle.networking

object NetworkUtils {

    fun getUrlWithPrefix(
        url: String?,
        podPrefix: String,
        enablePodRedirection: Boolean
    ): String? {
        return url?.let {
            val newUrl = if (enablePodRedirection) {
                "$it.$podPrefix"
            } else {
                "$it"
            }
            "$newUrl.mparticle.com"
        }
    }
}
