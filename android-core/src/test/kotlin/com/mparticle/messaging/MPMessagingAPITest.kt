package com.mparticle.messaging

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import com.mparticle.MPService
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class MPMessagingAPITest {
    @Test
    fun testExportedServiceWithoutPermissionIsFlagged() {
        Assert.assertTrue(isExportedWithoutPermission(packageManagerWith(exported = true, permission = null)))
    }

    @Test
    fun testExportedServiceWithPermissionIsNotFlagged() {
        Assert.assertFalse(isExportedWithoutPermission(packageManagerWith(exported = true, permission = "com.example.fake.permission.SEND")))
    }

    @Test
    fun testServiceNotExportedIsNotFlagged() {
        Assert.assertFalse(isExportedWithoutPermission(packageManagerWith(exported = false, permission = null)))
    }

    @Test
    fun testServiceNotDeclaredIsNotFlagged() {
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(packageManager.getServiceInfo(Mockito.any(), Mockito.anyInt())).thenThrow(PackageManager.NameNotFoundException::class.java)
        Assert.assertFalse(isExportedWithoutPermission(packageManager))
    }

    private fun packageManagerWith(exported: Boolean, permission: String?): PackageManager {
        val info = Mockito.mock(ServiceInfo::class.java)
        info.exported = exported
        info.permission = permission
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(packageManager.getServiceInfo(Mockito.any(), Mockito.anyInt())).thenReturn(info)
        return packageManager
    }

    private fun isExportedWithoutPermission(packageManager: PackageManager): Boolean {
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.packageManager).thenReturn(packageManager)
        Mockito.mockConstruction(ComponentName::class.java).use {
            return MPMessagingAPI.isExportedWithoutPermission(context, MPService::class.java)
        }
    }
}
