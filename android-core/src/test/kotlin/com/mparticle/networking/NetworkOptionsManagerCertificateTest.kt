package com.mparticle.networking

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class NetworkOptionsManagerCertificateTest {

    // SHA-256 of the DER encoding, as published by each CA.
    private val expectedFingerprints = mapOf(
        "godaddy_root_g2" to "45140B3247EB9CC8C5B4F0D7B53091F73292089E6E5A63E2749DD3ACA9198EDA",
        "godaddy_tls_root_r1" to "25CF3DA8E9B97ADDBF92543C2B82527C8A4E2CFF2062A6483040D4B64ACE719F",
        "godaddy_root_class2" to "C3846BF24B9E93CA64274C0EC67C1ECC5E024FFCACD2D74019350E81FE546AE4",
        "lets_encrypt_root_x1" to "96BCEC06264976F37460779ACF28C5A7CFE8A3C0AAE11A8FFCEE05C0BDDF08C6",
        "lets_encrypt_root_x2_self" to "69729B8E15A86EFC177A57AFB7171DFC64ADD28C2FCA8CF1507E34453CCB1470",
        "lets_encrypt_root_x2_cross" to "8B05B68CC659E5ED0FCB38F2C942FBFD200E6F2FF9F85D63C6994EF5E0B02701",
    )

    @Test
    fun defaultCertificatesArePinnedRootsWithExpectedFingerprints() {
        val pinned = NetworkOptionsManager.getDefaultCertificates()

        assertEquals(expectedFingerprints.keys.toList(), pinned.map { it.alias })
        for (certificate in pinned) {
            assertEquals(
                "fingerprint mismatch for ${certificate.alias}",
                expectedFingerprints[certificate.alias],
                fingerprintOf(parse(certificate.certificate)),
            )
        }
    }

    @Test
    fun goDaddyR1RootIsTheSelfSignedRootNotTheCrossSignedVariant() {
        val r1 = parse(
            NetworkOptionsManager.getDefaultCertificates()
                .single { it.alias == "godaddy_tls_root_r1" }
                .certificate,
        )

        assertEquals(r1.subjectX500Principal, r1.issuerX500Principal)
    }

    private fun parse(pem: String): X509Certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(pem.toByteArray(Charsets.US_ASCII)))
            as X509Certificate

    private fun fingerprintOf(certificate: X509Certificate): String =
        MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}
