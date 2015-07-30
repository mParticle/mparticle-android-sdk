package com.mparticle.internal;

import com.mparticle.BuildConfig;

/**
 * @serial @exclude
 */
public class Constants {

    public final static String LOG_TAG = "mParticle SDK";

    public static final String MPARTICLE_VERSION = BuildConfig.VERSION_NAME;

    // maximum number of events per session
    public static final int EVENT_LIMIT = 1000;
    // maximum messages to be sent in a single non-session history batch
    public static final int BATCH_LIMIT = 50;
    // delay (millis) before processing uploads to allow app to get started
    public static final long INITIAL_UPLOAD_DELAY = 10 * 1000;

    // preferences persistence
    public static final String PREFS_FILE = "mParticlePrefs";
    public static final String CART_PREFS_FILE = "mParticlePrefs_cart";

    public static final int LIMIT_ATTR_COUNT = 100;
    public static final int LIMIT_ATTR_NAME = 255;
    public static final int LIMIT_ATTR_VALUE = 255;
    public static final int LIMIT_NAME = 255;
    public static final byte[] LICENSE_CHECK_SALT = new byte[]{
            -46, 65, 30, -128, -103, -57, 74, 10, 51, 88, -95, -45, -43, -117, -36, 99, -11, 32, -64,
            89
    };


    public final static String GODADDY_INTERMEDIATE_CRT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIE0DCCA7igAwIBAgIBBzANBgkqhkiG9w0BAQsFADCBgzELMAkGA1UEBhMCVVMx\n" +
            "EDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxGjAYBgNVBAoT\n" +
            "EUdvRGFkZHkuY29tLCBJbmMuMTEwLwYDVQQDEyhHbyBEYWRkeSBSb290IENlcnRp\n" +
            "ZmljYXRlIEF1dGhvcml0eSAtIEcyMB4XDTExMDUwMzA3MDAwMFoXDTMxMDUwMzA3\n" +
            "MDAwMFowgbQxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQH\n" +
            "EwpTY290dHNkYWxlMRowGAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjEtMCsGA1UE\n" +
            "CxMkaHR0cDovL2NlcnRzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkvMTMwMQYDVQQD\n" +
            "EypHbyBEYWRkeSBTZWN1cmUgQ2VydGlmaWNhdGUgQXV0aG9yaXR5IC0gRzIwggEi\n" +
            "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC54MsQ1K92vdSTYuswZLiBCGzD\n" +
            "BNliF44v/z5lz4/OYuY8UhzaFkVLVat4a2ODYpDOD2lsmcgaFItMzEUz6ojcnqOv\n" +
            "K/6AYZ15V8TPLvQ/MDxdR/yaFrzDN5ZBUY4RS1T4KL7QjL7wMDge87Am+GZHY23e\n" +
            "cSZHjzhHU9FGHbTj3ADqRay9vHHZqm8A29vNMDp5T19MR/gd71vCxJ1gO7GyQ5HY\n" +
            "pDNO6rPWJ0+tJYqlxvTV0KaudAVkV4i1RFXULSo6Pvi4vekyCgKUZMQWOlDxSq7n\n" +
            "eTOvDCAHf+jfBDnCaQJsY1L6d8EbyHSHyLmTGFBUNUtpTrw700kuH9zB0lL7AgMB\n" +
            "AAGjggEaMIIBFjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNV\n" +
            "HQ4EFgQUQMK9J47MNIMwojPX+2yz8LQsgM4wHwYDVR0jBBgwFoAUOpqFBxBnKLbv\n" +
            "9r0FQW4gwZTaD94wNAYIKwYBBQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8v\n" +
            "b2NzcC5nb2RhZGR5LmNvbS8wNQYDVR0fBC4wLDAqoCigJoYkaHR0cDovL2NybC5n\n" +
            "b2RhZGR5LmNvbS9nZHJvb3QtZzIuY3JsMEYGA1UdIAQ/MD0wOwYEVR0gADAzMDEG\n" +
            "CCsGAQUFBwIBFiVodHRwczovL2NlcnRzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkv\n" +
            "MA0GCSqGSIb3DQEBCwUAA4IBAQAIfmyTEMg4uJapkEv/oV9PBO9sPpyIBslQj6Zz\n" +
            "91cxG7685C/b+LrTW+C05+Z5Yg4MotdqY3MxtfWoSKQ7CC2iXZDXtHwlTxFWMMS2\n" +
            "RJ17LJ3lXubvDGGqv+QqG+6EnriDfcFDzkSnE3ANkR/0yBOtg2DZ2HKocyQetawi\n" +
            "DsoXiWJYRBuriSUBAA/NxBti21G00w9RKpv0vHP8ds42pM3Z2Czqrpv1KrKQ0U11\n" +
            "GIo/ikGQI31bS/6kA1ibRrLDYGCD+H1QQc7CoZDDu+8CL9IVVO5EFdkKrqeKM+2x\n" +
            "LXY2JtwE65/3YR8V3Idv7kaWKK2hJn0KCacuBKONvPi8BDAB\n" +
            "-----END CERTIFICATE-----\n";

    public final static String GODADDY_ROOT_CRT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIE0DCCA7igAwIBAgIBBzANBgkqhkiG9w0BAQsFADCBgzELMAkGA1UEBhMCVVMx\n" +
            "EDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxGjAYBgNVBAoT\n" +
            "EUdvRGFkZHkuY29tLCBJbmMuMTEwLwYDVQQDEyhHbyBEYWRkeSBSb290IENlcnRp\n" +
            "ZmljYXRlIEF1dGhvcml0eSAtIEcyMB4XDTExMDUwMzA3MDAwMFoXDTMxMDUwMzA3\n" +
            "MDAwMFowgbQxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQH\n" +
            "EwpTY290dHNkYWxlMRowGAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjEtMCsGA1UE\n" +
            "CxMkaHR0cDovL2NlcnRzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkvMTMwMQYDVQQD\n" +
            "EypHbyBEYWRkeSBTZWN1cmUgQ2VydGlmaWNhdGUgQXV0aG9yaXR5IC0gRzIwggEi\n" +
            "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC54MsQ1K92vdSTYuswZLiBCGzD\n" +
            "BNliF44v/z5lz4/OYuY8UhzaFkVLVat4a2ODYpDOD2lsmcgaFItMzEUz6ojcnqOv\n" +
            "K/6AYZ15V8TPLvQ/MDxdR/yaFrzDN5ZBUY4RS1T4KL7QjL7wMDge87Am+GZHY23e\n" +
            "cSZHjzhHU9FGHbTj3ADqRay9vHHZqm8A29vNMDp5T19MR/gd71vCxJ1gO7GyQ5HY\n" +
            "pDNO6rPWJ0+tJYqlxvTV0KaudAVkV4i1RFXULSo6Pvi4vekyCgKUZMQWOlDxSq7n\n" +
            "eTOvDCAHf+jfBDnCaQJsY1L6d8EbyHSHyLmTGFBUNUtpTrw700kuH9zB0lL7AgMB\n" +
            "AAGjggEaMIIBFjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNV\n" +
            "HQ4EFgQUQMK9J47MNIMwojPX+2yz8LQsgM4wHwYDVR0jBBgwFoAUOpqFBxBnKLbv\n" +
            "9r0FQW4gwZTaD94wNAYIKwYBBQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8v\n" +
            "b2NzcC5nb2RhZGR5LmNvbS8wNQYDVR0fBC4wLDAqoCigJoYkaHR0cDovL2NybC5n\n" +
            "b2RhZGR5LmNvbS9nZHJvb3QtZzIuY3JsMEYGA1UdIAQ/MD0wOwYEVR0gADAzMDEG\n" +
            "CCsGAQUFBwIBFiVodHRwczovL2NlcnRzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkv\n" +
            "MA0GCSqGSIb3DQEBCwUAA4IBAQAIfmyTEMg4uJapkEv/oV9PBO9sPpyIBslQj6Zz\n" +
            "91cxG7685C/b+LrTW+C05+Z5Yg4MotdqY3MxtfWoSKQ7CC2iXZDXtHwlTxFWMMS2\n" +
            "RJ17LJ3lXubvDGGqv+QqG+6EnriDfcFDzkSnE3ANkR/0yBOtg2DZ2HKocyQetawi\n" +
            "DsoXiWJYRBuriSUBAA/NxBti21G00w9RKpv0vHP8ds42pM3Z2Czqrpv1KrKQ0U11\n" +
            "GIo/ikGQI31bS/6kA1ibRrLDYGCD+H1QQc7CoZDDu+8CL9IVVO5EFdkKrqeKM+2x\n" +
            "LXY2JtwE65/3YR8V3Idv7kaWKK2hJn0KCacuBKONvPi8BDAB\n" +
            "-----END CERTIFICATE-----";

    public final static String FIDDLER_ROOT_CRT = "-----BEGIN CERTIFICATE-----\n" +
            "MIICnjCCAgegAwIBAgIQAOlcuB4VA5KNHpx2RQcMrzANBgkqhkiG9w0BAQUFADBq\n" +
            "MSswKQYDVQQLDCJDcmVhdGVkIGJ5IGh0dHA6Ly93d3cuZmlkZGxlcjIuY29tMRgw\n" +
            "FgYDVQQKDA9ET19OT1RfVFJVU1RfQkMxITAfBgNVBAMMGERPX05PVF9UUlVTVF9G\n" +
            "aWRkbGVyUm9vdDAeFw0xMzEyMTIwMDAwMDBaFw0yMzEyMTkxMTI1NTRaMGoxKzAp\n" +
            "BgNVBAsMIkNyZWF0ZWQgYnkgaHR0cDovL3d3dy5maWRkbGVyMi5jb20xGDAWBgNV\n" +
            "BAoMD0RPX05PVF9UUlVTVF9CQzEhMB8GA1UEAwwYRE9fTk9UX1RSVVNUX0ZpZGRs\n" +
            "ZXJSb290MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC6P47ffxB2xJFlYVEZ\n" +
            "L4KSTORmxI21pUIb6jqkAEGYOeO+In5egCmroZuXbem1YYzTmgkmCelt6OTr0OLa\n" +
            "ePCkdnxteUDMBs0DpcWutdJW9/9MNE90BfJ2WX1CA4zQx4zFZ9FRpYHntaIE8kf4\n" +
            "bcts1+CE+VnI1fOPo0PsF6yudQIDAQABo0UwQzASBgNVHRMBAf8ECDAGAQH/AgEB\n" +
            "MA4GA1UdDwEB/wQEAwICBDAdBgNVHQ4EFgQUouuoWsFXoOzyyW94lTD/apHuos8w\n" +
            "DQYJKoZIhvcNAQEFBQADgYEAjOW9psxS4AeYgUcIhvNR5pd1BkuEwbdtgd8S0zgf\n" +
            "jOmkkQNKHPikfOeJurA3jityX3+z9d2zSvtbLU7MYArb7hs5cibAyxalI6NlWSsg\n" +
            "QGKwfeATxe0gReGYACTf2WIBa3ceQFhAYhyEUYJpDiZsJi8mZkeQMWH/ZanBnL/Q\n" +
            "gZ4=\n" +
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
        String APP_INFO = "ai";
        String DEVICE_INFO = "di";
        String USER_ATTRIBUTES = "ua";
        String USER_IDENTITIES = "ui";
        String DELETED_USER_ATTRIBUTES = "uad";
        // user identity
        String IDENTITY_NAME = "n";
        String IDENTITY_VALUE = "i";
        String IDENTITY_DATE_FIRST_SEEN = "dfs";
        String IDENTITY_FIRST_SEEN = "f";

        // application info
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
        // device info
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
        String GOOGLE_ADV_ID = "gaid";
        String LIMIT_AD_TRACKING = "lat";

        // state info
        String STATE_INFO_KEY = "cs";
        String STATE_INFO_CPU = "cpu";
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
        String HISTORY = "sh";
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
        
        String MPID = "mpid";
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

        int ENTITY_PRODUCT = 1;
        int ENTITY_PROMOTION = 2;

    }

    public interface PrefKeys {
        // common
        String INSTALL_TIME = "mp::ict";
        String PUSH_REGISTRATION_ID = "mp::push_reg_id";
        String INSTALL_REFERRER = "mp::install_referrer";
        String OPEN_UDID = "mp::openudid";
        // app-key specific (append appKey to the key)
        String OPTOUT = "mp::optout::";
        String USER_ATTRS = "mp::user_attrs::";
        String DELETED_USER_ATTRS = "mp::deleted_user_attrs::";
        String USER_IDENTITIES = "mp::user_ids::";
        String FIRSTRUN = "mp::firstrun::";
        String INITUPGRADE = "mp::initupgrade";
        String PROPERTY_APP_VERSION = "mp::appversion";
        String PROPERTY_OS_VERSION = "mp::osversion";
        String PUSH_ENABLED = "mp::push_enabled";
        String PUSH_SENDER_ID = "mp::push_sender_id";
        String PIRATED = "mp::pirated";
        String PUSH_ENABLE_SOUND = "mp::push::sound";
        String PUSH_ENABLE_VIBRATION = "mp::push::vibration";
        String PUSH_ICON = "mp::push::icon";
        String PUSH_TITLE = "mp::push::title";
        String SESSION_COUNTER = "mp::breadcrumbs::sessioncount";
        String BREADCRUMB_LIMIT = "mp::breadcrumbs::limit";
        String TOTAL_SINCE_UPGRADE = "mp::launch_since_upgrade";
        String UPGRADE_DATE = "mp::upgrade_date";
        String COUNTER_VERSION = "mp::version::counter";
        String LAST_USE = "mp::lastusedate";
        String TOTAL_RUNS = "mp::totalruns";
        String Cookies = "mp::cookies";
        String Mpid = "mp::mpid";
        String CRASHED_IN_FOREGROUND = "mp::crashed_in_foreground";
        String LTV = "mp::ltv";
        String PREVIOUS_SESSION_FOREGROUND = "mp::time_in_fg";
        String NEXT_REQUEST_TIME = "mp::next_valid_request_time";
        String EVENT_COUNTER = "mp::events::counter";
        String PREVIOUS_SESSION_ID = "mp::session::previous_id";
        String PREVIOUS_SESSION_START = "mp::session::previous_start";
        String API_KEY = "mp::config::apikey";
        String API_SECRET = "mp::config::apisecret";
        String FIRST_RUN_INSTALL = "mp::firstrun::install";
        String LOCATION_PROVIDER = "mp::location:provider";
        String LOCATION_MINTIME = "mp::location:mintime";
        String LOCATION_MINDISTANCE = "mp::location:mindistance";

        String CART = "mp::cart";
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
}
