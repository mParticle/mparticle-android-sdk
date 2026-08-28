package com.mparticle.networking

import com.mparticle.internal.Constants
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class NetworkOptionsManagerCertificateTest {
    @Test
    fun `default certificates contain the self-signed GoDaddy TLS R1 root`() {
        val pinnedCertificate =
            NetworkOptionsManager
                .getDefaultCertificates()
                .single { it.alias == "godaddy_tls_root_r1" }
        val certificate =
            CertificateFactory
                .getInstance("X.509")
                .generateCertificate(
                    ByteArrayInputStream(pinnedCertificate.certificate.toByteArray(Charsets.US_ASCII)),
                ) as X509Certificate
        val fingerprint =
            MessageDigest
                .getInstance("SHA-256")
                .digest(certificate.encoded)
                .joinToString("") { "%02X".format(it.toInt() and 0xFF) }

        assertEquals(Constants.GODADDY_TLS_ROOT_R1_CRT, pinnedCertificate.certificate)
        assertEquals("25CF3DA8E9B97ADDBF92543C2B82527C8A4E2CFF2062A6483040D4B64ACE719F", fingerprint)
        assertEquals(certificate.subjectX500Principal, certificate.issuerX500Principal)
    }
}
