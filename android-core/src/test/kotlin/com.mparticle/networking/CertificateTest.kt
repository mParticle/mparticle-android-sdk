package com.mparticle.networking

import com.mparticle.testutils.RandomUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CertificateTest {
    val randomUtils = RandomUtils()
    val alias = randomUtils.getAlphaNumericString(10)
    val certificateString = randomUtils.getAlphaNumericString(124)

    @Test
    fun buildCertificateTest() {
        val certificate = Certificate.with(alias, certificateString)
        assertEquals(alias, certificate?.getAlias())
        assertEquals(certificateString, certificate?.getCertificate())
    }

    @Test
    fun rejectMalformedCertificate() {
        var certificate = Certificate.with("", certificateString)
        assertNull(certificate)

        certificate = Certificate.with(alias, "")
        assertNull(certificate)
    }
}
