package com.mparticle.kits.mocks

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources

class MockApplication(var mContext: MockContext) : Application() {
    var mCallbacks: ActivityLifecycleCallbacks? = null
    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        mCallbacks = callback
    }

    override fun getApplicationContext(): Context = this

    fun setSharedPreferences(prefs: SharedPreferences) {
        mContext.setSharedPreferences(prefs)
    }

    override fun getSystemService(name: String): Any? = mContext.getSystemService(name)

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        mContext.getSharedPreferences(name, mode)

    override fun getPackageManager(): PackageManager = mContext.packageManager

    override fun getPackageName(): String = mContext.packageName

    override fun getApplicationInfo(): ApplicationInfo = mContext.applicationInfo

    override fun getResources(): Resources = mContext.resources
}
