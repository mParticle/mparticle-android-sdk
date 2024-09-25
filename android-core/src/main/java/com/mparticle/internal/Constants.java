package com.mparticle.internal;

import androidx.annotation.VisibleForTesting;

import com.mparticle.BuildConfig;

/**
 *
 */
public class Constants {

    private static int MAX_MESSAGE_PER_BATCH = 100;

    public static int getMaxMessagePerBatch() {
        return MAX_MESSAGE_PER_BATCH;
    }

    @VisibleForTesting
    public static void setMaxMessagePerBatch(int max) {
        if (max > 1) {
            MAX_MESSAGE_PER_BATCH = max;
        } else {
            MAX_MESSAGE_PER_BATCH = 100;
        }
    }

    public final static String LOG_TAG = "mParticle";

    public final static Long TEMPORARY_MPID = 0L;

    public static final String MPARTICLE_VERSION = BuildConfig.VERSION_NAME;

    // delay (millis) before processing uploads to allow app to get started
    public static final long INITIAL_UPLOAD_DELAY = 10 * 1000;

    // preferences persistence
    public static final String PREFS_FILE = "mParticlePrefs";

    public static final int LIMIT_ATTR_KEY = 256;
    public static final int LIMIT_ATTR_VALUE = 4096;
    public static final int LIMIT_MAX_MESSAGE_SIZE = 100 * 1024;
    public static final int LIMIT_MAX_UPLOAD_SIZE = 2 * LIMIT_MAX_MESSAGE_SIZE;

    public final static String GODADDY_CLASS_2_ROOT_CRT = "-----BEGIN CERTIFICATE-----\n" +
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
            "-----END CERTIFICATE-----";

    public final static String GODADDY_ROOT_G2_CRT = "-----BEGIN CERTIFICATE-----\n" +
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
            "-----END CERTIFICATE-----";

    public final static String LETS_ENCRYPTS_ROOT_X1_CRT = "-----BEGIN CERTIFICATE-----\n" +
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
            "-----END CERTIFICATE-----";

    public final static String LETS_ENCRYPTS_ROOT_X2_SELF_SIGN_CRT = "-----BEGIN CERTIFICATE-----\n" +
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
            "-----END CERTIFICATE-----";

    public final static String LETS_ENCRYPTS_ROOT_X2_CROSS_SIGN_CRT = "-----BEGIN CERTIFICATE-----\n" +
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
            "-----END CERTIFICATE-----";

    //wait 5 seconds to trigger an immediate upload in the case where multiple trigger-messages are logged
    //in quick succession.
    public static final long TRIGGER_MESSAGE_DELAY = 5000;
    //special session id for messages logged without a session
    public static final String NO_SESSION_ID = "NO-SESSION";
    public static final String REFERRER = "referrer";
    public static final String GOOGLE_GCM = "google";

    public interface MethodName {
        String METHOD_NAME = "$MethodName";
        String LOG_LTV = "LogLTVIncrease";
        String LOG_ECOMMERCE = "LogEcommerceTransaction";

        String LOG_ECOMMERCE_VIEW = "$ProductView";
        String LOG_ECOMMERCE_ADD_TO_CART = "$AddedToCart";
        String LOG_ECOMMERCE_REMOVE_FROM_CART = "$RemovedFromCart";
        String LOG_ECOMMERCE_ADD_TO_WISHLIST = "$AddedToWishlist";
        String LOG_ECOMMERCE_REMOVE_FROM_WISHLIST = "$RemovedFromWishlist";
    }

    public interface MessageType {
        String SESSION_START = "ss";
        String SESSION_END = "se";
        String EVENT = "e";
        String SCREEN_VIEW = "v";
        String COMMERCE_EVENT = "cm";
        String OPT_OUT = "o";
        String ERROR = "x";
        String PUSH_REGISTRATION = "pr";
        String REQUEST_HEADER = "h";
        String FIRST_RUN = "fr";
        String APP_STATE_TRANSITION = "ast";
        String PUSH_RECEIVED = "pm";
        String BREADCRUMB = "bc";
        String NETWORK_PERFORMNACE = "npe";
        String PROFILE = "pro";
        String USER_ATTRIBUTE_CHANGE = "uac";
        String USER_IDENTITY_CHANGE = "uic";
    }

    public interface MessageKey {
        // common
        String TYPE = "dt";
        String ID = "id";
        String TIMESTAMP = "ct";
        String SESSION_START_TIMESTAMP = "sct";
        String SESSION_ID = "sid";
        String SESSION_LENGTH = "sl";
        String SESSION_LENGTH_TOTAL = "slx";
        String ATTRIBUTES = "attrs";
        String EVENT_TYPE = "et";
        String NAME = "n";
        String OPT_OUT_HEADER = "oo";
        String PREVIOUS_SESSION_LENGTH = "psl";
        String LTV = "ltv";
        String SESSION_SPANNING_MPIDS = "smpids";
        String DATA_PLAN_CONTEXT = "ctx";
        String DATA_PLAN_KEY = "dpln";
        String DATA_PLAN_ID = "id";
        String DATA_PLAN_VERSION = "v";
        /**
         * mParticle cookies are a simplified form of real-cookies. The SDK server is stateless, so we need
         * a mechanism for server-state persistence.
         */
        String COOKIES = "ck";
        // referrer

        // event timing
        String EVENT_START_TIME = "est";
        String EVENT_DURATION = "el";
        // location
        String LOCATION = "lc";
        String LATITUDE = "lat";
        String LONGITUDE = "lng";
        String ACCURACY = "acc";
        // batch details
        String MPARTICLE_VERSION = "sdk";
        String MPARTICLE_CONFIG_VERSION = "mpcv";
        String APP_INFO = "ai";
        String DEVICE_INFO = "di";
        String USER_ATTRIBUTES = "ua";
        String USER_IDENTITIES = "ui";
        String DELETED_USER_ATTRIBUTES = "uad";
        String PRODUCT_BAGS = "pb";
        // user identity
        String IDENTITY_NAME = "n";
        String IDENTITY_VALUE = "i";
        String IDENTITY_DATE_FIRST_SEEN = "dfs";
        String IDENTITY_FIRST_SEEN = "f";

        // application customAttributes
        String APP_NAME = "an";
        String APP_VERSION = "av";
        String APP_PACKAGE_NAME = "apn";
        String APP_INSTALLER_NAME = "ain";
        String MPARTICLE_INSTALL_TIME = "ict";
        String INSTALL_REFERRER = "ir";
        String APP_VERSION_CODE = "abn";
        String APP_DEBUG_SIGNING = "dbg";
        String APP_PIRATED = "pir";
        String UPGRADE_DATE = "ud";
        String LAUNCH_COUNT = "lc";
        String LAUNCH_COUNT_SINCE_UPGRADE = "lcu";
        String LAST_USE_DATE = "lud";
        String SIDELOADED_KITS_COUNT = "sideloaded_kits_count";
        // device customAttributes
        String BUILD_ID = "bid";
        String BRAND = "b";
        String PRODUCT = "p";
        String DEVICE = "dn";
        String DEVICE_ID = "duid";
        String MANUFACTURER = "dma";
        String PLATFORM = "dp";
        String OS_VERSION = "dosv";
        String OS_VERSION_INT = "dosvi";
        String MODEL = "dmdl";
        String SCREEN_HEIGHT = "dsh";
        String SCREEN_WIDTH = "dsw";
        String SCREEN_DPI = "dpi";
        String DEVICE_COUNTRY = "dc";
        String DEVICE_LOCALE_COUNTRY = "dlc";
        String DEVICE_LOCALE_LANGUAGE = "dll";
        String DEVICE_ROOTED = "jb";
        String DEVICE_ROOTED_CYDIA = "cydia";
        String DEVICE_ANID = "anid";
        String DEVICE_TIMEZONE_NAME = "tzn";
        String DEVICE_OPEN_UDID = "ouid";
        String DEVICE_IS_TABLET = "it";
        String PUSH_SOUND_ENABLED = "se";
        String PUSH_VIBRATION_ENABLED = "ve";
        String DEVICE_SUPPORTS_TELEPHONY = "dst";
        String DEVICE_SUPPORTS_NFC = "dsnfc";
        String DEVICE_BLUETOOTH_ENABLED = "dbe";
        String DEVICE_BLUETOOTH_VERSION = "dbv";
        String AMAZON_ADV_ID = "faid";
        String GOOGLE_ADV_ID = "gaid";
        String LIMIT_AD_TRACKING = "lat";
        String DEVICE_IMEI = "imei";

        // state customAttributes
        String STATE_INFO_KEY = "cs";
        String STATE_INFO_AVAILABLE_MEMORY = "sma";
        String STATE_INFO_TOTAL_MEMORY = "tsm";
        String STATE_INFO_BATTERY_LVL = "bl";
        String STATE_INFO_TIME_SINCE_START = "tss";
        String STATE_INFO_AVAILABLE_DISK = "fds";
        String STATE_INFO_AVAILABLE_EXT_DISK = "efds";
        String STATE_INFO_APP_MEMORY_AVAIL = "ama";
        String STATE_INFO_APP_MEMORY_MAX = "amm";
        String STATE_INFO_APP_MEMORY_USAGE = "amt";
        String STATE_INFO_GPS = "gps";
        String STATE_INFO_BAR_ORIENTATION = "sbo";
        String STATE_INFO_DATA_CONNECTION = "dct";
        String STATE_INFO_ORIENTATION = "so";
        String STATE_INFO_MEMORY_LOW = "sml";
        String STATE_INFO_MEMORY_THRESHOLD = "smt";
        //app init message special keys
        String APP_INIT_CRASHED = "sc";
        String APP_INIT_FIRST_RUN = "ifr";
        String APP_INIT_UPGRADE = "iu";
        // network keys
        String NETWORK_COUNTRY = "nc";
        String NETWORK_CARRIER = "nca";
        String MOBILE_NETWORK_CODE = "mnc";
        String MOBILE_COUNTRY_CODE = "mcc";
        // errors
        String ERROR_SEVERITY = "s";
        String ERROR_CLASS = "c";
        String ERROR_MESSAGE = "m";
        String ERROR_STACK_TRACE = "st";
        String ERROR_UNCAUGHT = "eh";
        String ERROR_SESSION_COUNT = "sn";
        // uploading
        String MESSAGES = "msgs";
        String REPORTING = "fsr";
        String URL = "u";
        String METHOD = "m";
        String POST = "d";
        String HEADERS = "h";
        // config
        String TIMEZONE = "tz";
        // opt-out
        String OPT_OUT_STATUS = "s";
        // push-notifications
        String PUSH_TOKEN = "to";
        String PUSH_TOKEN_TYPE = "tot";
        String PUSH_REGISTER_FLAG = "r";
        String APP_STATE = "as";
        //state transition
        String STATE_TRANSITION_TYPE = "t";
        String CURRENT_ACTIVITY = "cn";
        String PAYLOAD = "pay";
        String PUSH_TYPE = "t";
        String PUSH_BEHAVIOR = "bhv";
        String PUSH_ACTION_TAKEN = "aid";
        String PUSH_ACTION_NAME = "an";
        String CONTENT_ID = "content_id";


        String ST_LAUNCH_REFERRER = "lr";
        String ST_LAUNCH_PARAMS = "lpr";
        String ST_LAUNCH_SOURCE_PACKAGE = "srp";
        String ST_LAUNCH_PRV_FORE_TIME = "pft";
        String ST_LAUNCH_TIME_SUSPENDED = "tls";
        String ST_INTERRUPTIONS = "nsi";

        //screen view
        String SCREEN_STARTED = "t";
        String BREADCRUMB_SESSION_COUNTER = "sn";
        String BREADCRUMB_LABEL = "l";
        String PROVIDER_PERSISTENCE = "cms";
        String RELEASE_VERSION = "vr";
        //network performance monitoring
        String NPE_METHOD = "v";
        String NPE_URL = "url";
        String NPE_LENGTH = "te";
        String NPE_SENT = "bo";
        String NPE_REC = "bi";
        String NPE_POST_DATA = "d";
        String CONFIG_SESSION_TIMEOUT = "stl";
        String CONFIG_UPLOAD_INTERVAL = "uitl";
        //events
        String EVENT_COUNTER = "en";
        String EVENT_CATEGORY = "$Category";
        //session start
        String PREVIOUS_SESSION_ID = "pid";
        String PREVIOUS_SESSION_START = "pss";
        //sandbox mode is deprecated as of > 1.6.3
        String SANDBOX = "dbg";
        String ENVIRONMENT = "env";
        String RESERVED_KEY_LTV = "$Amount";
        String FIRST_SEEN_INSTALL = "fi";

        String PUSH_CONTENT_ID = "cntid";
        String PUSH_CAMPAIGN_HISTORY = "pch";
        String PUSH_CAMPAIGN_HISTORY_TIMESTAMP = "ts";
        String STATE_INFO_NETWORK_TYPE = "ant";

        //identity
        String MPID = "mpid";
        String EVENT_FLAGS = "flags";
        String INTEGRATION_ATTRIBUTES = "ia";
        String NEW_ATTRIBUTE_VALUE = "nv";
        String OLD_ATTRIBUTE_VALUE = "ov";
        String ATTRIBUTE_DELETED = "d";
        String IS_NEW_ATTRIBUTE = "na";
        String NEW_USER_IDENTITY = "ni";
        String OLD_USER_IDENTITY = "oi";
        String ECHO = "echo";
        String DEVICE_IS_IN_DST = "idst";
        String DEVICE_APPLICATION_STAMP = "das";

        //consent state
        String CONSENT_STATE = "con";
        String CONSENT_STATE_GDPR = "gdpr";
        String CONSENT_STATE_CCPA = "ccpa";
        String CONSENT_STATE_DOCUMENT = "d";
        String CONSENT_STATE_CONSENTED = "c";
        String CONSENT_STATE_TIMESTAMP = "ts";
        String CONSENT_STATE_LOCATION = "l";
        String CONSENT_STATE_HARDWARE_ID = "h";
        String CCPA_CONSENT_KEY = "data_sale_opt_out";

        //alias request
        String SOURCE_MPID = "source_mpid";
        String DESTINATION_MPID = "destination_mpid";
        String START_TIME = "start_unixtime_ms";
        String END_TIME = "end_unixtime_ms";
        String DEVICE_APPLICATION_STAMP_ALIAS = "device_application_stamp";
        String ENVIRONMENT_ALIAS = "environment";
        String REQUEST_ID = "request_id";
        String REQUEST_TYPE = "request_type";
        String API_KEY = "api_key";
        String DATA = "data";
        String ALIAS_REQUEST_TYPE = "alias";

        //batch was mutated
        String MODIFIED_BATCH = "mb";
    }

    public interface Commerce {
        String SCREEN_NAME = "sn";
        String NON_INTERACTION = "ni";
        String CURRENCY = "cu";
        String ATTRIBUTES = "attrs";
        String PRODUCT_ACTION_OBJECT = "pd";
        String PRODUCT_ACTION = "an";
        String CHECKOUT_STEP = "cs";
        String CHECKOUT_OPTIONS = "co";
        String PRODUCT_LIST_NAME = "pal";
        String PRODUCT_LIST_SOURCE = "pls";
        String TRANSACTION_ID = "ti";
        String TRANSACTION_AFFILIATION = "ta";
        String TRANSACTION_REVENUE = "tr";
        String TRANSACTION_TAX = "tt";
        String TRANSACTION_SHIPPING = "ts";
        String TRANSACTION_COUPON_CODE = "tcc";
        String PRODUCT_LIST = "pl";
        String PROMOTION_ACTION_OBJECT = "pm";
        String PROMOTION_ACTION = "an";
        String PROMOTION_LIST = "pl";
        String IMPRESSION_OBJECT = "pi";
        String IMPRESSION_LOCATION = "pil";
        String IMPRESSION_PRODUCT_LIST = "pl";

        String ATT_AFFILIATION = "Affiliation";

        String ATT_TRANSACTION_COUPON_CODE = "Coupon Code";
        String ATT_TOTAL = "Total Amount";
        String ATT_SHIPPING = "Shipping Amount";
        String ATT_TAX = "Tax Amount";
        String ATT_TRANSACTION_ID = "Transaction Id";
        String ATT_PRODUCT_QUANTITY = "Quantity";
        String ATT_PRODUCT_POSITION = "Position";
        String ATT_PRODUCT_VARIANT = "Variant";
        String ATT_PRODUCT_ID = "Id";
        String ATT_PRODUCT_NAME = "Name";
        String ATT_PRODUCT_CATEGORY = "Category";
        String ATT_PRODUCT_BRAND = "Brand";
        String ATT_PRODUCT_COUPON_CODE = "Coupon Code";
        String ATT_PRODUCT_PRICE = "Item Price";
        String ATT_ACTION_PRODUCT_ACTION_LIST = "Product Action List";
        String ATT_ACTION_PRODUCT_LIST_SOURCE = "Product List Source";
        String ATT_ACTION_CHECKOUT_OPTIONS = "Checkout Options";
        String ATT_ACTION_CHECKOUT_STEP = "Checkout Step";
        String ATT_ACTION_CURRENCY_CODE = "Currency Code";
        String ATT_SCREEN_NAME = "Screen Name";
        String ATT_PROMOTION_ID = "Id";
        String ATT_PROMOTION_POSITION = "Position";
        String ATT_PROMOTION_NAME = "Name";
        String ATT_PROMOTION_CREATIVE = "Creative";
        String ATT_PRODUCT_TOTAL_AMOUNT = "Total Product Amount";

        /**
         * This is only set when required. Otherwise default to null.
         */
        String DEFAULT_CURRENCY_CODE = "USD";

        int EVENT_TYPE_ADD_TO_CART = 10;
        int EVENT_TYPE_REMOVE_FROM_CART = 11;
        int EVENT_TYPE_CHECKOUT = 12;
        int EVENT_TYPE_CHECKOUT_OPTION = 13;
        int EVENT_TYPE_CLICK = 14;
        int EVENT_TYPE_VIEW_DETAIL = 15;
        int EVENT_TYPE_PURCHASE = 16;
        int EVENT_TYPE_REFUND = 17;
        int EVENT_TYPE_PROMOTION_VIEW = 18;
        int EVENT_TYPE_PROMOTION_CLICK = 19;
        int EVENT_TYPE_ADD_TO_WISHLIST = 20;
        int EVENT_TYPE_REMOVE_FROM_WISHLIST = 21;
        int EVENT_TYPE_IMPRESSION = 22;

        String EVENT_TYPE_STRING_ADD_TO_CART = "ProductAddToCart";
        String EVENT_TYPE_STRING_REMOVE_FROM_CART = "ProductRemoveFromCart";
        String EVENT_TYPE_STRING_CHECKOUT = "ProductCheckout";
        String EVENT_TYPE_STRING_CHECKOUT_OPTION = "ProductCheckoutOption";
        String EVENT_TYPE_STRING_CLICK = "ProductClick";
        String EVENT_TYPE_STRING_VIEW_DETAIL = "ProductViewDetail";
        String EVENT_TYPE_STRING_PURCHASE = "ProductPurchase";
        String EVENT_TYPE_STRING_REFUND = "ProductRefund";
        String EVENT_TYPE_STRING_PROMOTION_VIEW = "PromotionView";
        String EVENT_TYPE_STRING_PROMOTION_CLICK = "PromotionClick";
        String EVENT_TYPE_STRING_ADD_TO_WISHLIST = "ProductAddToWishlist";
        String EVENT_TYPE_STRING_REMOVE_FROM_WISHLIST = "ProductRemoveFromWishlist";
        String EVENT_TYPE_STRING_IMPRESSION = "ProductImpression";
        String EVENT_TYPE_STRING_UNKNOWN = "Unknown";


    }

    public interface PrefKeys {
        // common
        String INSTALL_TIME = "mp::ict";
        String INSTALL_REFERRER = "mp::install_referrer";
        String OPEN_UDID = "mp::openudid";
        String DEVICE_RAMP_UDID = "mp::rampudid";
        // app-key specific (append appKey to the key)
        String OPTOUT = "mp::optout::";
        String DEPRECATED_USER_ATTRS = "mp::user_attrs::";

        String FIRSTRUN_OBSELETE = "mp::firstrun::";
        String FIRSTRUN_MESSAGE = "mp::firstrun::message";
        String FIRSTRUN_AST = "mp::firstrun::ast";
        String INITUPGRADE = "mp::initupgrade";
        String PROPERTY_APP_VERSION = "mp::appversion";
        String PROPERTY_OS_VERSION = "mp::osversion";
        String PUSH_ENABLED = "mp::push_enabled";
        String PUSH_SENDER_ID = "mp::push_sender_id";
        String PUSH_INSTANCE_ID = "mp::push_reg_id";
        String PUSH_INSTANCE_ID_BACKGROUND = "mp::push_reg_id_bckgrnd";

        String PIRATED = "mp::pirated";
        String PUSH_ENABLE_SOUND = "mp::push::sound";
        String PUSH_ENABLE_VIBRATION = "mp::push::vibration";
        String PUSH_ICON = "mp::push::icon";
        String PUSH_TITLE = "mp::push::title";
        String UPGRADE_DATE = "mp::upgrade_date";
        String COUNTER_VERSION = "mp::version::counter";
        String MPID = "mp::mpid::identity";
        String CRASHED_IN_FOREGROUND = "mp::crashed_in_foreground";
        String NEXT_REQUEST_TIME = "mp::next_valid_request_time";
        String EVENT_COUNTER = "mp::events::counter";
        String API_KEY = "mp::config::apikey";
        String API_SECRET = "mp::config::apisecret";
        String FIRST_RUN_INSTALL = "mp::firstrun::install";
        String LOCATION_PROVIDER = "mp::location:provider";
        String LOCATION_MINTIME = "mp::location:mintime";
        String LOCATION_MINDISTANCE = "mp::location:mindistance";
        String INTEGRATION_ATTRIBUTES = "mp::integrationattributes";
        String ETAG = "mp::etag";
        String IF_MODIFIED = "mp::ifmodified";
        String IDENTITY_API_CONTEXT = "mp::identity::api::context";
        String DEVICE_APPLICATION_STAMP = "mp::device-app-stamp";
        String PREVIOUS_ANDROID_ID = "mp::previous::android::id";
        String DISPLAY_PUSH_NOTIFICATIONS = "mp::displaypushnotifications";
        String IDENTITY_CONNECTION_TIMEOUT = "mp::connection:timeout:identity";
        String NETWORK_OPTIONS = "mp::network:options";
        String UPLOAD_INTERVAL = "mp::uploadInterval";
        String SESSION_TIMEOUT = "mp::sessionTimeout";
        String REPORT_UNCAUGHT_EXCEPTIONS = "mp::reportUncaughtExceptions";
        String ENVIRONMENT = "mp::environment";
        String IDENTITY_API_REQUEST ="mp::identity::api::request";
        String IDENTITY_API_CACHE_TIME ="mp::identity::cache::time";
        String IDENTITY_MAX_AGE ="mp::max:age::time";
    }

    public interface MiscStorageKeys {
        String TOTAL_MEMORY = "mp::totalmem";
        String MEMORY_THRESHOLD = "mp::memthreshold";
    }

    public interface Status {
        int READY = 1;
        int BATCH_READY = 2;
        int UPLOADED = 3;
    }

    public interface StateTransitionType {
        String STATE_TRANS_INIT = "app_init";
        String STATE_TRANS_EXIT = "app_exit";
        String STATE_TRANS_BG = "app_back";
        String STATE_TRANS_FORE = "app_fore";
    }

    public interface Audience {
        String ACTION_ADD = "add";
        String API_AUDIENCE_LIST = "m";
        String API_AUDIENCE_ID = "id";
        String API_AUDIENCE_NAME = "n";
        String API_AUDIENCE_ENDPOINTS = "s";
        String API_AUDIENCE_MEMBERSHIPS = "c";
        String API_AUDIENCE_ACTION = "a";
        String API_AUDIENCE_MEMBERSHIP_TIMESTAMP = "ct";
    }

    public interface ProfileActions {
        String LOGOUT = "logout";
        String KEY = "t";
    }

    public interface External {
        String APPLINK_KEY = "al_applink_data";
    }

    public interface Push {
        String MESSAGE_TYPE_RECEIVED = "received";
        String MESSAGE_TYPE_ACTION = "action";
    }

    public interface Platform {
        String ANDROID = "Android";
        String FIRE_OS = "FireTV";
    }
}
