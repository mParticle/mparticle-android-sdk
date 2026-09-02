package com.mparticle.internal

import android.app.Application
import android.content.Context
import android.content.ContextWrapper

open class KitContext(base: Context) : ContextWrapper(base) {
    private val applicationContextWrapper: ApplicationContextWrapper =
        ApplicationContextWrapper(base.applicationContext as Application)

    override fun getApplicationContext(): Context = applicationContextWrapper
}
