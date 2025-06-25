package com.mparticle.internal

import androidx.annotation.VisibleForTesting
import com.mparticle.BuildConfig

/**
 *
 */
object Constants {
    private var MAX_MESSAGE_PER_BATCH = 100

    @JvmStatic
    @set:VisibleForTesting
    var maxMessagePerBatch: Int
        get() = MAX_MESSAGE_PER_BATCH
        set(max) {
            if (max > 1) {
                MAX_MESSAGE_PER_BATCH = max
            } else {
                MAX_MESSAGE_PER_BATCH = 100
            }
        }

    const val LOG_TAG: String = "mParticle"

    const val TEMPORARY_MPID: Long = 0L

    const val MPARTICLE_VERSION: String = BuildConfig.VERSION_NAME

    // delay (millis) before processing uploads to allow app to get started
    const val INITIAL_UPLOAD_DELAY: Long = (10 * 1000).toLong()

    // preferences persistence
    const val PREFS_FILE: String = "mParticlePrefs"

    const val LIMIT_ATTR_KEY: Int = 256
    const val LIMIT_ATTR_VALUE: Int = 4096
    const val LIMIT_MAX_MESSAGE_SIZE: Int = 100 * 1024
    const val LIMIT_MAX_UPLOAD_SIZE: Int = 2 * LIMIT_MAX_MESSAGE_SIZE

    const val GODADDY_CLASS_2_ROOT_CRT: String = "-----BEGIN CERTIFICATE-----\n" +
            "MIIEADCCAuigAwIBAgIBADANBgkqhkiG9w0BAQUFADBjMQswCQYDVQQGEwJVUzEh\n" +
            "MB8GA1UEChMYVGhlIEdvIERhZGR5IEdyb3VwLCBJbmMuMTEwLwYDVQQLEyhHbyBE\n" +
            "YWRkeSBDbGFzcyAyIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MB4XDTA0MDYyOTE3\n" +
            "MDYyMFoXDTM0MDYyOTE3MDYyMFowYzELMAkGA1UEBhMCVVMxITAfBgNVBAoTGFRo\n" +
            "ZSBHbyBEYWRkeSBHcm91cCwgSW5jLjExMC8GA1UECxMoR28gRGFkZHkgQ2xhc3Mg\n" +
            "MiBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTCCASAwDQYJKoZIhvcNAQEBBQADggEN\n" +
            "ADCCAQgCggEBAN6d1+pXGEmhW+vXX0iG6r7d/+TvZxz0ZWizV3GgXne77ZtJ6XCA\n" +
            "PVYYYwhv2vLM0D9/AlQiVBDYsoHUwHU9S3/Hd8M+eKsaA7Ugay9qK7HFiH7Eux6w\n" +
            "wdhFJ2+qN1j3hybX2C32qRe3H3I2TqYXP2WYktsqbl2i/ojgC95/5Y0V4evLOtXi\n" +
            "EqITLdiOr18SPaAIBQi2XKVlOARFmR6jYGB0xUGlcmIbYsUfb18aQr4CUWWoriMY\n" +
            "avx4A6lNf4DD+qta/KFApMoZFv6yyO9ecw3ud72a9nmYvLEHZ6IVDd2gWMZEewo+\n" +
            "YihfukEHU1jPEX44dMX4/7VpkI+EdOqXG68CAQOjgcAwgb0wHQYDVR0OBBYEFNLE\n" +
            "sNKR1EwRcbNhyz2h/t2oatTjMIGNBgNVHSMEgYUwgYKAFNLEsNKR1EwRcbNhyz2h\n" +
            "/t2oatTjoWekZTBjMQswCQYDVQQGEwJVUzEhMB8GA1UEChMYVGhlIEdvIERhZGR5\n" +
            "IEdyb3VwLCBJbmMuMTEwLwYDVQQLEyhHbyBEYWRkeSBDbGFzcyAyIENlcnRpZmlj\n" +
            "YXRpb24gQXV0aG9yaXR5ggEAMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQAD\n" +
            "ggEBADJL87LKPpH8EsahB4yOd6AzBhRckB4Y9wimPQoZ+YeAEW5p5JYXMP80kWNy\n" +
            "OO7MHAGjHZQopDH2esRU1/blMVgDoszOYtuURXO1v0XJJLXVggKtI3lpjbi2Tc7P\n" +
            "TMozI+gciKqdi0FuFskg5YmezTvacPd+mSYgFFQlq25zheabIZ0KbIIOqPjCDPoQ\n" +
            "HmyW74cNxA9hi63ugyuV+I6ShHI56yDqg+2DzZduCLzrTia2cyvk0/ZM/iZx4mER\n" +
            "dEr/VxqHD3VILs9RaRegAhJhldXRQLIQTO7ErBBDpqWeCtWVYpoNz4iCxTIM5Cuf\n" +
            "ReYNnyicsbkqWletNw+vHX/bvZ8=\n" +
            "-----END CERTIFICATE-----"

    const val GODADDY_ROOT_G2_CRT: String = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDxTCCAq2gAwIBAgIBADANBgkqhkiG9w0BAQsFADCBgzELMAkGA1UEBhMCVVMx\n" +
            "EDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxGjAYBgNVBAoT\n" +
            "EUdvRGFkZHkuY29tLCBJbmMuMTEwLwYDVQQDEyhHbyBEYWRkeSBSb290IENlcnRp\n" +
            "ZmljYXRlIEF1dGhvcml0eSAtIEcyMB4XDTA5MDkwMTAwMDAwMFoXDTM3MTIzMTIz\n" +
            "NTk1OVowgYMxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQH\n" +
            "EwpTY290dHNkYWxlMRowGAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjExMC8GA1UE\n" +
            "AxMoR28gRGFkZHkgUm9vdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkgLSBHMjCCASIw\n" +
            "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL9xYgjx+lk09xvJGKP3gElY6SKD\n" +
            "E6bFIEMBO4Tx5oVJnyfq9oQbTqC023CYxzIBsQU+B07u9PpPL1kwIuerGVZr4oAH\n" +
            "/PMWdYA5UXvl+TW2dE6pjYIT5LY/qQOD+qK+ihVqf94Lw7YZFAXK6sOoBJQ7Rnwy\n" +
            "DfMAZiLIjWltNowRGLfTshxgtDj6AozO091GB94KPutdfMh8+7ArU6SSYmlRJQVh\n" +
            "GkSBjCypQ5Yj36w6gZoOKcUcqeldHraenjAKOc7xiID7S13MMuyFYkMlNAJWJwGR\n" +
            "tDtwKj9useiciAF9n9T521NtYJ2/LOdYq7hfRvzOxBsDPAnrSTFcaUaz4EcCAwEA\n" +
            "AaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYE\n" +
            "FDqahQcQZyi27/a9BUFuIMGU2g/eMA0GCSqGSIb3DQEBCwUAA4IBAQCZ21151fmX\n" +
            "WWcDYfF+OwYxdS2hII5PZYe096acvNjpL9DbWu7PdIxztDhC2gV7+AJ1uP2lsdeu\n" +
            "9tfeE8tTEH6KRtGX+rcuKxGrkLAngPnon1rpN5+r5N9ss4UXnT3ZJE95kTXWXwTr\n" +
            "gIOrmgIttRD02JDHBHNA7XIloKmf7J6raBKZV8aPEjoJpL1E/QYVN8Gb5DKj7Tjo\n" +
            "2GTzLH4U/ALqn83/B2gX2yKQOC16jdFU8WnjXzPKej17CuPKf1855eJ1usV2GDPO\n" +
            "LPAvTK33sefOT6jEm0pUBsV/fdUID+Ic/n4XuKxe9tQWskMJDE32p2u0mYRlynqI\n" +
            "4uJEvlz36hz1\n" +
            "-----END CERTIFICATE-----"

    const val LETS_ENCRYPTS_ROOT_X1_CRT: String = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw\n" +
            "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n" +
            "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4\n" +
            "WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu\n" +
            "ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY\n" +
            "MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc\n" +
            "h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+\n" +
            "0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U\n" +
            "A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW\n" +
            "T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH\n" +
            "B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC\n" +
            "B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv\n" +
            "KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn\n" +
            "OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn\n" +
            "jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw\n" +
            "qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI\n" +
            "rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV\n" +
            "HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq\n" +
            "hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL\n" +
            "ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ\n" +
            "3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK\n" +
            "NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5\n" +
            "ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur\n" +
            "TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC\n" +
            "jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc\n" +
            "oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq\n" +
            "4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA\n" +
            "mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d\n" +
            "emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=\n" +
            "-----END CERTIFICATE-----"

    const val LETS_ENCRYPTS_ROOT_X2_SELF_SIGN_CRT: String = "-----BEGIN CERTIFICATE-----\n" +
            "MIICGzCCAaGgAwIBAgIQQdKd0XLq7qeAwSxs6S+HUjAKBggqhkjOPQQDAzBPMQsw\n" +
            "CQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJuZXQgU2VjdXJpdHkgUmVzZWFyY2gg\n" +
            "R3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBYMjAeFw0yMDA5MDQwMDAwMDBaFw00\n" +
            "MDA5MTcxNjAwMDBaME8xCzAJBgNVBAYTAlVTMSkwJwYDVQQKEyBJbnRlcm5ldCBT\n" +
            "ZWN1cml0eSBSZXNlYXJjaCBHcm91cDEVMBMGA1UEAxMMSVNSRyBSb290IFgyMHYw\n" +
            "EAYHKoZIzj0CAQYFK4EEACIDYgAEzZvVn4CDCuwJSvMWSj5cz3es3mcFDR0HttwW\n" +
            "+1qLFNvicWDEukWVEYmO6gbf9yoWHKS5xcUy4APgHoIYOIvXRdgKam7mAHf7AlF9\n" +
            "ItgKbppbd9/w+kHsOdx1ymgHDB/qo0IwQDAOBgNVHQ8BAf8EBAMCAQYwDwYDVR0T\n" +
            "AQH/BAUwAwEB/zAdBgNVHQ4EFgQUfEKWrt5LSDv6kviejM9ti6lyN5UwCgYIKoZI\n" +
            "zj0EAwMDaAAwZQIwe3lORlCEwkSHRhtFcP9Ymd70/aTSVaYgLXTWNLxBo1BfASdW\n" +
            "tL4ndQavEi51mI38AjEAi/V3bNTIZargCyzuFJ0nN6T5U6VR5CmD1/iQMVtCnwr1\n" +
            "/q4AaOeMSQ+2b1tbFfLn\n" +
            "-----END CERTIFICATE-----"

    const val LETS_ENCRYPTS_ROOT_X2_CROSS_SIGN_CRT: String = "-----BEGIN CERTIFICATE-----\n" +
            "MIIEYDCCAkigAwIBAgIQB55JKIY3b9QISMI/xjHkYzANBgkqhkiG9w0BAQsFADBP\n" +
            "MQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJuZXQgU2VjdXJpdHkgUmVzZWFy\n" +
            "Y2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBYMTAeFw0yMDA5MDQwMDAwMDBa\n" +
            "Fw0yNTA5MTUxNjAwMDBaME8xCzAJBgNVBAYTAlVTMSkwJwYDVQQKEyBJbnRlcm5l\n" +
            "dCBTZWN1cml0eSBSZXNlYXJjaCBHcm91cDEVMBMGA1UEAxMMSVNSRyBSb290IFgy\n" +
            "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEzZvVn4CDCuwJSvMWSj5cz3es3mcFDR0H\n" +
            "ttwW+1qLFNvicWDEukWVEYmO6gbf9yoWHKS5xcUy4APgHoIYOIvXRdgKam7mAHf7\n" +
            "AlF9ItgKbppbd9/w+kHsOdx1ymgHDB/qo4HlMIHiMA4GA1UdDwEB/wQEAwIBBjAP\n" +
            "BgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBR8Qpau3ktIO/qS+J6Mz22LqXI3lTAf\n" +
            "BgNVHSMEGDAWgBR5tFnme7bl5AFzgAiIyBpY9umbbjAyBggrBgEFBQcBAQQmMCQw\n" +
            "IgYIKwYBBQUHMAKGFmh0dHA6Ly94MS5pLmxlbmNyLm9yZy8wJwYDVR0fBCAwHjAc\n" +
            "oBqgGIYWaHR0cDovL3gxLmMubGVuY3Iub3JnLzAiBgNVHSAEGzAZMAgGBmeBDAEC\n" +
            "ATANBgsrBgEEAYLfEwEBATANBgkqhkiG9w0BAQsFAAOCAgEAG38lK5B6CHYAdxjh\n" +
            "wy6KNkxBfr8XS+Mw11sMfpyWmG97sGjAJETM4vL80erb0p8B+RdNDJ1V/aWtbdIv\n" +
            "P0tywC6uc8clFlfCPhWt4DHRCoSEbGJ4QjEiRhrtekC/lxaBRHfKbHtdIVwH8hGR\n" +
            "Ib/hL8Lvbv0FIOS093nzLbs3KvDGsaysUfUfs1oeZs5YBxg4f3GpPIO617yCnpp2\n" +
            "D56wKf3L84kHSBv+q5MuFCENX6+Ot1SrXQ7UW0xx0JLqPaM2m3wf4DtVudhTU8yD\n" +
            "ZrtK3IEGABiL9LPXSLETQbnEtp7PLHeOQiALgH6fxatI27xvBI1sRikCDXCKHfES\n" +
            "c7ZGJEKeKhcY46zHmMJyzG0tdm3dLCsmlqXPIQgb5dovy++fc5Ou+DZfR4+XKM6r\n" +
            "4pgmmIv97igyIintTJUJxCD6B+GGLET2gUfA5GIy7R3YPEiIlsNekbave1mk7uOG\n" +
            "nMeIWMooKmZVm4WAuR3YQCvJHBM8qevemcIWQPb1pK4qJWxSuscETLQyu/w4XKAM\n" +
            "YXtX7HdOUM+vBqIPN4zhDtLTLxq9nHE+zOH40aijvQT2GcD5hq/1DhqqlWvvykdx\n" +
            "S2McTZbbVSMKnQ+BdaDmQPVkRgNuzvpqfQbspDQGdNpT2Lm4xiN9qfgqLaSCpi4t\n" +
            "EcrmzTFYeYXmchynn9NM0GbQp7s=\n" +
            "-----END CERTIFICATE-----"

    //wait 5 seconds to trigger an immediate upload in the case where multiple trigger-messages are logged
    //in quick succession.
    const val TRIGGER_MESSAGE_DELAY: Long = 5000

    //special session id for messages logged without a session
    const val NO_SESSION_ID: String = "NO-SESSION"
    const val REFERRER: String = "referrer"
    const val GOOGLE_GCM: String = "google"

    interface MethodName {
        companion object {
            const val METHOD_NAME: String = "\$MethodName"
            const val LOG_LTV: String = "LogLTVIncrease"
            const val LOG_ECOMMERCE: String = "LogEcommerceTransaction"

            const val LOG_ECOMMERCE_VIEW: String = "\$ProductView"
            const val LOG_ECOMMERCE_ADD_TO_CART: String = "\$AddedToCart"
            const val LOG_ECOMMERCE_REMOVE_FROM_CART: String = "\$RemovedFromCart"
            const val LOG_ECOMMERCE_ADD_TO_WISHLIST: String = "\$AddedToWishlist"
            const val LOG_ECOMMERCE_REMOVE_FROM_WISHLIST: String = "\$RemovedFromWishlist"
        }
    }

    interface MessageType {
        companion object {
            const val SESSION_START: String = "ss"
            const val SESSION_END: String = "se"
            const val EVENT: String = "e"
            const val SCREEN_VIEW: String = "v"
            const val COMMERCE_EVENT: String = "cm"
            const val OPT_OUT: String = "o"
            const val ERROR: String = "x"
            const val PUSH_REGISTRATION: String = "pr"
            const val REQUEST_HEADER: String = "h"
            const val FIRST_RUN: String = "fr"
            const val APP_STATE_TRANSITION: String = "ast"
            const val PUSH_RECEIVED: String = "pm"
            const val BREADCRUMB: String = "bc"
            const val NETWORK_PERFORMNACE: String = "npe"
            const val PROFILE: String = "pro"
            const val USER_ATTRIBUTE_CHANGE: String = "uac"
            const val USER_IDENTITY_CHANGE: String = "uic"
        }
    }

    interface MessageKey {
        companion object {
            // common
            const val TYPE: String = "dt"
            const val ID: String = "id"
            const val TIMESTAMP: String = "ct"
            const val SESSION_START_TIMESTAMP: String = "sct"
            const val SESSION_ID: String = "sid"
            const val SESSION_LENGTH: String = "sl"
            const val SESSION_LENGTH_TOTAL: String = "slx"
            const val ATTRIBUTES: String = "attrs"
            const val EVENT_TYPE: String = "et"
            const val NAME: String = "n"
            const val OPT_OUT_HEADER: String = "oo"
            const val PREVIOUS_SESSION_LENGTH: String = "psl"
            const val LTV: String = "ltv"
            const val SESSION_SPANNING_MPIDS: String = "smpids"
            const val DATA_PLAN_CONTEXT: String = "ctx"
            const val DATA_PLAN_KEY: String = "dpln"
            const val DATA_PLAN_ID: String = "id"
            const val DATA_PLAN_VERSION: String = "v"

            /**
             * mParticle cookies are a simplified form of real-cookies. The SDK server is stateless, so we need
             * a mechanism for server-state persistence.
             */
            const val COOKIES: String = "ck"

            // referrer
            // event timing
            const val EVENT_START_TIME: String = "est"
            const val EVENT_DURATION: String = "el"

            // location
            const val LOCATION: String = "lc"
            const val LATITUDE: String = "lat"
            const val LONGITUDE: String = "lng"
            const val ACCURACY: String = "acc"

            // batch details
            const val MPARTICLE_VERSION: String = "sdk"
            const val MPARTICLE_CONFIG_VERSION: String = "mpcv"
            const val APP_INFO: String = "ai"
            const val DEVICE_INFO: String = "di"
            const val USER_ATTRIBUTES: String = "ua"
            const val USER_IDENTITIES: String = "ui"
            const val DELETED_USER_ATTRIBUTES: String = "uad"
            const val PRODUCT_BAGS: String = "pb"

            // user identity
            const val IDENTITY_NAME: String = "n"
            const val IDENTITY_VALUE: String = "i"
            const val IDENTITY_DATE_FIRST_SEEN: String = "dfs"
            const val IDENTITY_FIRST_SEEN: String = "f"

            // application customAttributes
            const val APP_NAME: String = "an"
            const val APP_VERSION: String = "av"
            const val APP_PACKAGE_NAME: String = "apn"
            const val APP_INSTALLER_NAME: String = "ain"
            const val MPARTICLE_INSTALL_TIME: String = "ict"
            const val INSTALL_REFERRER: String = "ir"
            const val APP_VERSION_CODE: String = "abn"
            const val APP_DEBUG_SIGNING: String = "dbg"
            const val APP_PIRATED: String = "pir"
            const val UPGRADE_DATE: String = "ud"
            const val LAUNCH_COUNT: String = "lc"
            const val LAUNCH_COUNT_SINCE_UPGRADE: String = "lcu"
            const val LAST_USE_DATE: String = "lud"
            const val SIDELOADED_KITS_COUNT: String = "sideloaded_kits_count"

            // device customAttributes
            const val BUILD_ID: String = "bid"
            const val BRAND: String = "b"
            const val PRODUCT: String = "p"
            const val DEVICE: String = "dn"
            const val DEVICE_ID: String = "duid"
            const val MANUFACTURER: String = "dma"
            const val PLATFORM: String = "dp"
            const val OS_VERSION: String = "dosv"
            const val OS_VERSION_INT: String = "dosvi"
            const val MODEL: String = "dmdl"
            const val SCREEN_HEIGHT: String = "dsh"
            const val SCREEN_WIDTH: String = "dsw"
            const val SCREEN_DPI: String = "dpi"
            const val DEVICE_COUNTRY: String = "dc"
            const val DEVICE_LOCALE_COUNTRY: String = "dlc"
            const val DEVICE_LOCALE_LANGUAGE: String = "dll"
            const val DEVICE_ROOTED: String = "jb"
            const val DEVICE_ROOTED_CYDIA: String = "cydia"
            const val DEVICE_ANID: String = "anid"
            const val DEVICE_TIMEZONE_NAME: String = "tzn"
            const val DEVICE_OPEN_UDID: String = "ouid"
            const val DEVICE_IS_TABLET: String = "it"
            const val PUSH_SOUND_ENABLED: String = "se"
            const val PUSH_VIBRATION_ENABLED: String = "ve"
            const val DEVICE_SUPPORTS_TELEPHONY: String = "dst"
            const val DEVICE_SUPPORTS_NFC: String = "dsnfc"
            const val DEVICE_BLUETOOTH_ENABLED: String = "dbe"
            const val DEVICE_BLUETOOTH_VERSION: String = "dbv"
            const val AMAZON_ADV_ID: String = "faid"
            const val GOOGLE_ADV_ID: String = "gaid"
            const val LIMIT_AD_TRACKING: String = "lat"
            const val DEVICE_IMEI: String = "imei"

            // state customAttributes
            const val STATE_INFO_KEY: String = "cs"
            const val STATE_INFO_AVAILABLE_MEMORY: String = "sma"
            const val STATE_INFO_TOTAL_MEMORY: String = "tsm"
            const val STATE_INFO_BATTERY_LVL: String = "bl"
            const val STATE_INFO_TIME_SINCE_START: String = "tss"
            const val STATE_INFO_AVAILABLE_DISK: String = "fds"
            const val STATE_INFO_AVAILABLE_EXT_DISK: String = "efds"
            const val STATE_INFO_APP_MEMORY_AVAIL: String = "ama"
            const val STATE_INFO_APP_MEMORY_MAX: String = "amm"
            const val STATE_INFO_APP_MEMORY_USAGE: String = "amt"
            const val STATE_INFO_GPS: String = "gps"
            const val STATE_INFO_BAR_ORIENTATION: String = "sbo"
            const val STATE_INFO_DATA_CONNECTION: String = "dct"
            const val STATE_INFO_ORIENTATION: String = "so"
            const val STATE_INFO_MEMORY_LOW: String = "sml"
            const val STATE_INFO_MEMORY_THRESHOLD: String = "smt"

            //app init message special keys
            const val APP_INIT_CRASHED: String = "sc"
            const val APP_INIT_FIRST_RUN: String = "ifr"
            const val APP_INIT_UPGRADE: String = "iu"

            // network keys
            const val NETWORK_COUNTRY: String = "nc"
            const val NETWORK_CARRIER: String = "nca"
            const val MOBILE_NETWORK_CODE: String = "mnc"
            const val MOBILE_COUNTRY_CODE: String = "mcc"

            // errors
            const val ERROR_SEVERITY: String = "s"
            const val ERROR_CLASS: String = "c"
            const val ERROR_MESSAGE: String = "m"
            const val ERROR_STACK_TRACE: String = "st"
            const val ERROR_UNCAUGHT: String = "eh"
            const val ERROR_SESSION_COUNT: String = "sn"

            // uploading
            const val MESSAGES: String = "msgs"
            const val REPORTING: String = "fsr"
            const val URL: String = "u"
            const val METHOD: String = "m"
            const val POST: String = "d"
            const val HEADERS: String = "h"

            // config
            const val TIMEZONE: String = "tz"

            // opt-out
            const val OPT_OUT_STATUS: String = "s"

            // push-notifications
            const val PUSH_TOKEN: String = "to"
            const val PUSH_TOKEN_TYPE: String = "tot"
            const val PUSH_REGISTER_FLAG: String = "r"
            const val APP_STATE: String = "as"

            //state transition
            const val STATE_TRANSITION_TYPE: String = "t"
            const val CURRENT_ACTIVITY: String = "cn"
            const val PAYLOAD: String = "pay"
            const val PUSH_TYPE: String = "t"
            const val PUSH_BEHAVIOR: String = "bhv"
            const val PUSH_ACTION_TAKEN: String = "aid"
            const val PUSH_ACTION_NAME: String = "an"
            const val CONTENT_ID: String = "content_id"


            const val ST_LAUNCH_REFERRER: String = "lr"
            const val ST_LAUNCH_PARAMS: String = "lpr"
            const val ST_LAUNCH_SOURCE_PACKAGE: String = "srp"
            const val ST_LAUNCH_PRV_FORE_TIME: String = "pft"
            const val ST_LAUNCH_TIME_SUSPENDED: String = "tls"
            const val ST_INTERRUPTIONS: String = "nsi"

            //screen view
            const val SCREEN_STARTED: String = "t"
            const val BREADCRUMB_SESSION_COUNTER: String = "sn"
            const val BREADCRUMB_LABEL: String = "l"
            const val PROVIDER_PERSISTENCE: String = "cms"
            const val RELEASE_VERSION: String = "vr"

            //network performance monitoring
            const val NPE_METHOD: String = "v"
            const val NPE_URL: String = "url"
            const val NPE_LENGTH: String = "te"
            const val NPE_SENT: String = "bo"
            const val NPE_REC: String = "bi"
            const val NPE_POST_DATA: String = "d"
            const val CONFIG_SESSION_TIMEOUT: String = "stl"
            const val CONFIG_UPLOAD_INTERVAL: String = "uitl"

            //events
            const val EVENT_COUNTER: String = "en"
            const val EVENT_CATEGORY: String = "\$Category"

            //session start
            const val PREVIOUS_SESSION_ID: String = "pid"
            const val PREVIOUS_SESSION_START: String = "pss"

            // https://go.mparticle.com/work/SQDSDKS-6814
            //sandbox mode is deprecated as of > 1.6.3
            const val SANDBOX: String = "dbg"
            const val ENVIRONMENT: String = "env"
            const val RESERVED_KEY_LTV: String = "\$Amount"
            const val FIRST_SEEN_INSTALL: String = "fi"

            const val PUSH_CONTENT_ID: String = "cntid"
            const val PUSH_CAMPAIGN_HISTORY: String = "pch"
            const val PUSH_CAMPAIGN_HISTORY_TIMESTAMP: String = "ts"
            const val STATE_INFO_NETWORK_TYPE: String = "ant"

            //identity
            const val MPID: String = "mpid"
            const val EVENT_FLAGS: String = "flags"
            const val INTEGRATION_ATTRIBUTES: String = "ia"
            const val NEW_ATTRIBUTE_VALUE: String = "nv"
            const val OLD_ATTRIBUTE_VALUE: String = "ov"
            const val ATTRIBUTE_DELETED: String = "d"
            const val IS_NEW_ATTRIBUTE: String = "na"
            const val NEW_USER_IDENTITY: String = "ni"
            const val OLD_USER_IDENTITY: String = "oi"
            const val ECHO: String = "echo"
            const val DEVICE_IS_IN_DST: String = "idst"
            const val DEVICE_APPLICATION_STAMP: String = "das"

            //consent state
            const val CONSENT_STATE: String = "con"
            const val CONSENT_STATE_GDPR: String = "gdpr"
            const val CONSENT_STATE_CCPA: String = "ccpa"
            const val CONSENT_STATE_DOCUMENT: String = "d"
            const val CONSENT_STATE_CONSENTED: String = "c"
            const val CONSENT_STATE_TIMESTAMP: String = "ts"
            const val CONSENT_STATE_LOCATION: String = "l"
            const val CONSENT_STATE_HARDWARE_ID: String = "h"
            const val CCPA_CONSENT_KEY: String = "data_sale_opt_out"

            //alias request
            const val SOURCE_MPID: String = "source_mpid"
            const val DESTINATION_MPID: String = "destination_mpid"
            const val START_TIME: String = "start_unixtime_ms"
            const val END_TIME: String = "end_unixtime_ms"
            const val DEVICE_APPLICATION_STAMP_ALIAS: String = "device_application_stamp"
            const val ENVIRONMENT_ALIAS: String = "environment"
            const val REQUEST_ID: String = "request_id"
            const val REQUEST_TYPE: String = "request_type"
            const val API_KEY: String = "api_key"
            const val DATA: String = "data"
            const val ALIAS_REQUEST_TYPE: String = "alias"

            //batch was mutated
            const val MODIFIED_BATCH: String = "mb"

            //Sandbox mode for Rokt
            const val SANDBOX_MODE_ROKT: String = "sandbox"
        }
    }

    interface Commerce {
        companion object {
            const val SCREEN_NAME: String = "sn"
            const val NON_INTERACTION: String = "ni"
            const val CURRENCY: String = "cu"
            const val ATTRIBUTES: String = "attrs"
            const val PRODUCT_ACTION_OBJECT: String = "pd"
            const val PRODUCT_ACTION: String = "an"
            const val CHECKOUT_STEP: String = "cs"
            const val CHECKOUT_OPTIONS: String = "co"
            const val PRODUCT_LIST_NAME: String = "pal"
            const val PRODUCT_LIST_SOURCE: String = "pls"
            const val TRANSACTION_ID: String = "ti"
            const val TRANSACTION_AFFILIATION: String = "ta"
            const val TRANSACTION_REVENUE: String = "tr"
            const val TRANSACTION_TAX: String = "tt"
            const val TRANSACTION_SHIPPING: String = "ts"
            const val TRANSACTION_COUPON_CODE: String = "tcc"
            const val PRODUCT_LIST: String = "pl"
            const val PROMOTION_ACTION_OBJECT: String = "pm"
            const val PROMOTION_ACTION: String = "an"
            const val PROMOTION_LIST: String = "pl"
            const val IMPRESSION_OBJECT: String = "pi"
            const val IMPRESSION_LOCATION: String = "pil"
            const val IMPRESSION_PRODUCT_LIST: String = "pl"

            const val ATT_AFFILIATION: String = "Affiliation"

            const val ATT_TRANSACTION_COUPON_CODE: String = "Coupon Code"
            const val ATT_TOTAL: String = "Total Amount"
            const val ATT_SHIPPING: String = "Shipping Amount"
            const val ATT_TAX: String = "Tax Amount"
            const val ATT_TRANSACTION_ID: String = "Transaction Id"
            const val ATT_PRODUCT_QUANTITY: String = "Quantity"
            const val ATT_PRODUCT_POSITION: String = "Position"
            const val ATT_PRODUCT_VARIANT: String = "Variant"
            const val ATT_PRODUCT_ID: String = "Id"
            const val ATT_PRODUCT_NAME: String = "Name"
            const val ATT_PRODUCT_CATEGORY: String = "Category"
            const val ATT_PRODUCT_BRAND: String = "Brand"
            const val ATT_PRODUCT_COUPON_CODE: String = "Coupon Code"
            const val ATT_PRODUCT_PRICE: String = "Item Price"
            const val ATT_ACTION_PRODUCT_ACTION_LIST: String = "Product Action List"
            const val ATT_ACTION_PRODUCT_LIST_SOURCE: String = "Product List Source"
            const val ATT_ACTION_CHECKOUT_OPTIONS: String = "Checkout Options"
            const val ATT_ACTION_CHECKOUT_STEP: String = "Checkout Step"
            const val ATT_ACTION_CURRENCY_CODE: String = "Currency Code"
            const val ATT_SCREEN_NAME: String = "Screen Name"
            const val ATT_PROMOTION_ID: String = "Id"
            const val ATT_PROMOTION_POSITION: String = "Position"
            const val ATT_PROMOTION_NAME: String = "Name"
            const val ATT_PROMOTION_CREATIVE: String = "Creative"
            const val ATT_PRODUCT_TOTAL_AMOUNT: String = "Total Product Amount"

            /**
             * This is only set when required. Otherwise default to null.
             */
            const val DEFAULT_CURRENCY_CODE: String = "USD"

            const val EVENT_TYPE_ADD_TO_CART: Int = 10
            const val EVENT_TYPE_REMOVE_FROM_CART: Int = 11
            const val EVENT_TYPE_CHECKOUT: Int = 12
            const val EVENT_TYPE_CHECKOUT_OPTION: Int = 13
            const val EVENT_TYPE_CLICK: Int = 14
            const val EVENT_TYPE_VIEW_DETAIL: Int = 15
            const val EVENT_TYPE_PURCHASE: Int = 16
            const val EVENT_TYPE_REFUND: Int = 17
            const val EVENT_TYPE_PROMOTION_VIEW: Int = 18
            const val EVENT_TYPE_PROMOTION_CLICK: Int = 19
            const val EVENT_TYPE_ADD_TO_WISHLIST: Int = 20
            const val EVENT_TYPE_REMOVE_FROM_WISHLIST: Int = 21
            const val EVENT_TYPE_IMPRESSION: Int = 22

            const val EVENT_TYPE_STRING_ADD_TO_CART: String = "ProductAddToCart"
            const val EVENT_TYPE_STRING_REMOVE_FROM_CART: String = "ProductRemoveFromCart"
            const val EVENT_TYPE_STRING_CHECKOUT: String = "ProductCheckout"
            const val EVENT_TYPE_STRING_CHECKOUT_OPTION: String = "ProductCheckoutOption"
            const val EVENT_TYPE_STRING_CLICK: String = "ProductClick"
            const val EVENT_TYPE_STRING_VIEW_DETAIL: String = "ProductViewDetail"
            const val EVENT_TYPE_STRING_PURCHASE: String = "ProductPurchase"
            const val EVENT_TYPE_STRING_REFUND: String = "ProductRefund"
            const val EVENT_TYPE_STRING_PROMOTION_VIEW: String = "PromotionView"
            const val EVENT_TYPE_STRING_PROMOTION_CLICK: String = "PromotionClick"
            const val EVENT_TYPE_STRING_ADD_TO_WISHLIST: String = "ProductAddToWishlist"
            const val EVENT_TYPE_STRING_REMOVE_FROM_WISHLIST: String = "ProductRemoveFromWishlist"
            const val EVENT_TYPE_STRING_IMPRESSION: String = "ProductImpression"
            const val EVENT_TYPE_STRING_UNKNOWN: String = "Unknown"
        }
    }

    interface PrefKeys {
        companion object {
            // common
            const val INSTALL_TIME: String = "mp::ict"
            const val INSTALL_REFERRER: String = "mp::install_referrer"
            const val OPEN_UDID: String = "mp::openudid"
            const val DEVICE_RAMP_UDID: String = "mp::rampudid"

            // app-key specific (append appKey to the key)
            const val OPTOUT: String = "mp::optout::"
            const val DEPRECATED_USER_ATTRS: String = "mp::user_attrs::"

            const val FIRSTRUN_OBSELETE: String = "mp::firstrun::"
            const val FIRSTRUN_MESSAGE: String = "mp::firstrun::message"
            const val FIRSTRUN_AST: String = "mp::firstrun::ast"
            const val INITUPGRADE: String = "mp::initupgrade"
            const val PROPERTY_APP_VERSION: String = "mp::appversion"
            const val PROPERTY_OS_VERSION: String = "mp::osversion"
            const val PUSH_ENABLED: String = "mp::push_enabled"
            const val PUSH_SENDER_ID: String = "mp::push_sender_id"
            const val PUSH_INSTANCE_ID: String = "mp::push_reg_id"
            const val PUSH_INSTANCE_ID_BACKGROUND: String = "mp::push_reg_id_bckgrnd"

            const val PIRATED: String = "mp::pirated"
            const val PUSH_ENABLE_SOUND: String = "mp::push::sound"
            const val PUSH_ENABLE_VIBRATION: String = "mp::push::vibration"
            const val PUSH_ICON: String = "mp::push::icon"
            const val PUSH_TITLE: String = "mp::push::title"
            const val UPGRADE_DATE: String = "mp::upgrade_date"
            const val COUNTER_VERSION: String = "mp::version::counter"
            const val MPID: String = "mp::mpid::identity"
            const val CRASHED_IN_FOREGROUND: String = "mp::crashed_in_foreground"
            const val NEXT_REQUEST_TIME: String = "mp::next_valid_request_time"
            const val EVENT_COUNTER: String = "mp::events::counter"
            const val API_KEY: String = "mp::config::apikey"
            const val API_SECRET: String = "mp::config::apisecret"
            const val FIRST_RUN_INSTALL: String = "mp::firstrun::install"
            const val LOCATION_PROVIDER: String = "mp::location:provider"
            const val LOCATION_MINTIME: String = "mp::location:mintime"
            const val LOCATION_MINDISTANCE: String = "mp::location:mindistance"
            const val INTEGRATION_ATTRIBUTES: String = "mp::integrationattributes"
            const val ETAG: String = "mp::etag"
            const val IF_MODIFIED: String = "mp::ifmodified"
            const val IDENTITY_API_CONTEXT: String = "mp::identity::api::context"
            const val DEVICE_APPLICATION_STAMP: String = "mp::device-app-stamp"
            const val PREVIOUS_ANDROID_ID: String = "mp::previous::android::id"
            const val DISPLAY_PUSH_NOTIFICATIONS: String = "mp::displaypushnotifications"
            const val IDENTITY_CONNECTION_TIMEOUT: String = "mp::connection:timeout:identity"
            const val NETWORK_OPTIONS: String = "mp::network:options"
            const val UPLOAD_INTERVAL: String = "mp::uploadInterval"
            const val SESSION_TIMEOUT: String = "mp::sessionTimeout"
            const val REPORT_UNCAUGHT_EXCEPTIONS: String = "mp::reportUncaughtExceptions"
            const val ENVIRONMENT: String = "mp::environment"
        }
    }

    interface MiscStorageKeys {
        companion object {
            const val TOTAL_MEMORY: String = "mp::totalmem"
            const val MEMORY_THRESHOLD: String = "mp::memthreshold"
        }
    }

    interface Status {
        companion object {
            const val READY: Int = 1
            const val BATCH_READY: Int = 2
            const val UPLOADED: Int = 3
        }
    }

    interface StateTransitionType {
        companion object {
            const val STATE_TRANS_INIT: String = "app_init"
            const val STATE_TRANS_EXIT: String = "app_exit"
            const val STATE_TRANS_BG: String = "app_back"
            const val STATE_TRANS_FORE: String = "app_fore"
        }
    }

    interface Audience {
        companion object {
            const val ACTION_ADD: String = "add"
            const val API_AUDIENCE_LIST: String = "m"
            const val API_AUDIENCE_ID: String = "id"
            const val API_AUDIENCE_NAME: String = "n"
            const val API_AUDIENCE_ENDPOINTS: String = "s"
            const val API_AUDIENCE_MEMBERSHIPS: String = "c"
            const val API_AUDIENCE_ACTION: String = "a"
            const val API_AUDIENCE_MEMBERSHIP_TIMESTAMP: String = "ct"
        }
    }

    interface ProfileActions {
        companion object {
            const val LOGOUT: String = "logout"
            const val KEY: String = "t"
        }
    }

    interface External {
        companion object {
            const val APPLINK_KEY: String = "al_applink_data"
        }
    }

    interface Push {
        companion object {
            const val MESSAGE_TYPE_RECEIVED: String = "received"
            const val MESSAGE_TYPE_ACTION: String = "action"
        }
    }

    interface Platform {
        companion object {
            const val ANDROID: String = "Android"
            const val FIRE_OS: String = "FireTV"
        }
    }
}
