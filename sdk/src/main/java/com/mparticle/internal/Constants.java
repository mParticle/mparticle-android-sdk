package com.mparticle.internal;

import com.mparticle.BuildConfig;

public class Constants {

    public final static String LOG_TAG = "mParticle SDK";

    public static final String MPARTICLE_VERSION = BuildConfig.VERSION_NAME;

    // maximum number of events per session
    public static final int EVENT_LIMIT = 1000;
    // delay (millis) before processing uploads to allow app to get started
    public static final long INITIAL_UPLOAD_DELAY = 5 * 1000; // 30 seconds

    // preferences persistence
    public static final String PREFS_FILE = "mParticlePrefs";

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

    public interface MethodName {
        public static final String METHOD_NAME = "$MethodName";
        public static final String LOG_LTV = "LogLTVIncrease";
        public static final String LOG_ECOMMERCE = "LogEcommerceTransaction";

        public static final String LOG_ECOMMERCE_VIEW = "$ProductView";
        public static final String LOG_ECOMMERCE_ADD_TO_CART = "$AddedToCart";
        public static final String LOG_ECOMMERCE_REMOVE_FROM_CART = "$RemovedFromCart";
        public static final String LOG_ECOMMERCE_ADD_TO_WISHLIST = "$AddedToWishlist";
        public static final String LOG_ECOMMERCE_REMOVE_FROM_WISHLIST = "$RemovedFromWishlist";

    }

    public interface MessageType {
        public static final String SESSION_START = "ss";
        public static final String SESSION_END = "se";
        public static final String EVENT = "e";
        public static final String SCREEN_VIEW = "v";
        public static final String OPT_OUT = "o";
        public static final String ERROR = "x";
        public static final String PUSH_REGISTRATION = "pr";
        public static final String REQUEST_HEADER = "h";
        public static final String RESPONSE_HEADER = "rh";
        public static final String HTTP_COMMAND = "hc";
        public static final String FIRST_RUN = "fr";
        public static final String APP_STATE_TRANSITION = "ast";
        public static final String PUSH_RECEIVED = "pm";
        public static final String BREADCRUMB = "bc";
        public static final String NETWORK_PERFORMNACE = "npe";
        public static final String PROFILE = "pro";
    }

    public interface MessageKey {
        // common
        public static final String TYPE = "dt";
        public static final String ID = "id";
        public static final String TIMESTAMP = "ct";
        public static final String SESSION_START_TIMESTAMP = "sct";
        public static final String SESSION_ID = "sid";
        public static final String SESSION_LENGTH = "sl";
        public static final String SESSION_LENGTH_TOTAL = "slx";
        public static final String ATTRIBUTES = "attrs";
        public static final String EVENT_TYPE = "et";
        public static final String NAME = "n";
        public static final String OPT_OUT_HEADER = "oo";
        public static final String PREVIOUS_SESSION_LENGTH = "psl";
        public static final String LTV = "ltv";
        // referrer

        // event timing
        public static final String EVENT_START_TIME = "est";
        public static final String EVENT_DURATION = "el";
        // location
        public static final String LOCATION = "lc";
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "lng";
        public static final String ACCURACY = "acc";
        // batch details
        public static final String MPARTICLE_VERSION = "sdk";
        public static final String APP_INFO = "ai";
        public static final String DEVICE_INFO = "di";
        public static final String USER_ATTRIBUTES = "ua";
        public static final String USER_IDENTITIES = "ui";
        public static final String DELETED_USER_ATTRIBUTES = "uad";
        // user identity
        public static final String IDENTITY_NAME = "n";
        public static final String IDENTITY_VALUE = "i";
        public static final String IDENTITY_DATE_FIRST_SEEN = "dfs";
        public static final String IDENTITY_FIRST_SEEN = "f";

        // application info
        public static final String APP_NAME = "an";
        public static final String APP_VERSION = "av";
        public static final String APP_PACKAGE_NAME = "apn";
        public static final String APP_INSTALLER_NAME = "ain";
        public static final String MPARTICLE_INSTALL_TIME = "ict";
        public static final String INSTALL_REFERRER = "ir";
        public static final String APP_VERSION_CODE = "abn";
        public static final String APP_DEBUG_SIGNING = "dbg";
        public static final String APP_PIRATED = "pir";
        public static final String UPGRADE_DATE = "ud";
        public static final String LAUNCH_COUNT = "lc";
        public static final String LAUNCH_COUNT_SINCE_UPGRADE = "lcu";
        public static final String LAST_USE_DATE = "lud";
        // device info
        public static final String BUILD_ID = "bid";
        public static final String BRAND = "b";
        public static final String PRODUCT = "p";
        public static final String DEVICE = "dn";
        public static final String DEVICE_ID = "duid";
        public static final String MANUFACTURER = "dma";
        public static final String PLATFORM = "dp";
        public static final String OS_VERSION = "dosv";
        public static final String OS_VERSION_INT = "dosvi";
        public static final String MODEL = "dmdl";
        public static final String SCREEN_HEIGHT = "dsh";
        public static final String SCREEN_WIDTH = "dsw";
        public static final String SCREEN_DPI = "dpi";
        public static final String DEVICE_COUNTRY = "dc";
        public static final String DEVICE_LOCALE_COUNTRY = "dlc";
        public static final String DEVICE_LOCALE_LANGUAGE = "dll";
        public static final String DEVICE_ROOTED = "jb";
        public static final String DEVICE_ROOTED_CYDIA = "cydia";
        public static final String DEVICE_MAC_WIFI = "wmac";
        public static final String DEVICE_MAC_BLUETOOTH = "bmac";
        public static final String DEVICE_IMEI = "imei";
        public static final String DEVICE_ANID = "anid";
        public static final String DEVICE_TIMEZONE_NAME = "tzn";
        public static final String DEVICE_OPEN_UDID = "ouid";
        public static final String DEVICE_PUSH_TOKEN = "to";
        public static final String DEVICE_IS_TABLET = "it";
        public static final String PUSH_SOUND_ENABLED = "se";
        public static final String PUSH_VIBRATION_ENABLED = "ve";
        public static final String DEVICE_SUPPORTS_TELEPHONY = "dst";
        public static final String DEVICE_SUPPORTS_NFC = "dsnfc";
        public static final String DEVICE_BLUETOOTH_ENABLED = "dbe";
        public static final String DEVICE_BLUETOOTH_VERSION = "dbv";
        public static final String GOOGLE_ADV_ID = "gaid";
        public static final String LIMIT_AD_TRACKING = "lat";
        public static final String ADTRUTH_ID = "atp";

        // state info
        public static final String STATE_INFO_KEY = "cs";
        public static final String STATE_INFO_CPU = "cpu";
        public static final String STATE_INFO_AVAILABLE_MEMORY = "sma";
        public static final String STATE_INFO_TOTAL_MEMORY = "tsm";
        public static final String STATE_INFO_BATTERY_LVL = "bl";
        public static final String STATE_INFO_TIME_SINCE_START = "tss";
        public static final String STATE_INFO_AVAILABLE_DISK = "fds";
        public static final String STATE_INFO_AVAILABLE_EXT_DISK = "efds";
        public static final String STATE_INFO_APP_MEMORY_AVAIL = "ama";
        public static final String STATE_INFO_APP_MEMORY_MAX = "amm";
        public static final String STATE_INFO_APP_MEMORY_USAGE = "amt";
        public static final String STATE_INFO_GPS = "gps";
        public static final String STATE_INFO_BAR_ORIENTATION = "sbo";
        public static final String STATE_INFO_DATA_CONNECTION = "dct";
        public static final String STATE_INFO_ORIENTATION = "so";
        public static final String STATE_INFO_MEMORY_LOW = "sml";
        public static final String STATE_INFO_MEMORY_THRESHOLD = "smt";
        //app init message special keys
        public static final String APP_INIT_CRASHED = "sc";
        public static final String APP_INIT_FIRST_RUN = "ifr";
        public static final String APP_INIT_UPGRADE = "iu";
        // network keys
        public static final String NETWORK_COUNTRY = "nc";
        public static final String NETWORK_CARRIER = "nca";
        public static final String MOBILE_NETWORK_CODE = "mnc";
        public static final String MOBILE_COUNTRY_CODE = "mcc";
        // errors
        public static final String ERROR_SEVERITY = "s";
        public static final String ERROR_CLASS = "c";
        public static final String ERROR_MESSAGE = "m";
        public static final String ERROR_STACK_TRACE = "st";
        public static final String ERROR_UNCAUGHT = "eh";
        public static final String ERROR_SESSION_COUNT = "sn";
        // uploading
        public static final String MESSAGES = "msgs";
        public static final String HISTORY = "sh";
        public static final String URL = "u";
        public static final String METHOD = "m";
        public static final String POST = "d";
        public static final String HEADERS = "h";
        // config
        public static final String SESSION_UPLOAD = "su";
        public static final String TIMEZONE = "tz";
        // opt-out
        public static final String OPT_OUT_STATUS = "s";
        // push-notifications
        public static final String PUSH_TOKEN = "to";
        public static final String PUSH_TOKEN_TYPE = "tot";
        public static final String PUSH_REGISTER_FLAG = "r";
        public static final String APP_STATE = "as";
        //state transition
        public static final String STATE_TRANSITION_TYPE = "t";
        public static final String CURRENT_ACTIVITY = "cn";
        public static final String PAYLOAD = "pay";
        public static final String PUSH_TYPE = "t";
        public static final String PUSH_BEHAVIOR = "bhv";
        public static final String PUSH_ACTION_TAKEN = "aid";
        public static final String PUSH_ACTION_NAME = "an";


        public static final String ST_LAUNCH_REFERRER = "lr";
        public static final String ST_LAUNCH_PARAMS = "lpr";
        public static final String ST_LAUNCH_SOURCE_PACKAGE = "srp";
        public static final String ST_LAUNCH_PRV_FORE_TIME = "pft";
        public static final String ST_LAUNCH_TIME_SUSPENDED = "tls";
        public static final String ST_INTERRUPTIONS = "nsi";

        //screen view
        public static final String SCREEN_STARTED = "t";
        public static final String BREADCRUMB_SESSION_COUNTER = "sn";
        public static final String BREADCRUMB_LABEL = "l";
        public static final String PROVIDER_PERSISTENCE = "cms";
        public static final String RELEASE_VERSION = "vr";
        //network performance monitoring
        public static final String NPE_METHOD = "v";
        public static final String NPE_URL = "url";
        public static final String NPE_LENGTH = "te";
        public static final String NPE_SENT = "bo";
        public static final String NPE_REC = "bi";
        public static final String NPE_POST_DATA = "d";
        public static final String CONFIG_SESSION_TIMEOUT = "stl";
        public static final String CONFIG_UPLOAD_INTERVAL = "uitl";
        //events
        public static final String EVENT_COUNTER = "en";
        public static final String EVENT_CATEGORY = "$Category";
        //session start
        public static final String PREVIOUS_SESSION_ID = "pid";
        public static final String PREVIOUS_SESSION_START = "pss";
        //sandbox mode is deprecated as of > 1.6.3
        public static final String SANDBOX = "dbg";
        public static final String ENVIRONMENT = "env";
        String RESERVED_KEY_LTV = "$Amount";
        public static final String FIRST_SEEN_INSTALL = "fi";

        public static final String PUSH_CONTENT_ID = "cntid";
        public static final String PUSH_CAMPAIGN_HISTORY = "pch";
        public static final String PUSH_CAMPAIGN_HISTORY_TIMESTAMP = "ts";

        public static final String STATE_INFO_NETWORK_TYPE = "ant";
    }

    public interface PrefKeys {
        // common
        public static final String INSTALL_TIME = "mp::ict";
        public static final String PUSH_REGISTRATION_ID = "mp::push_reg_id";
        public static final String INSTALL_REFERRER = "mp::install_referrer";
        public static final String OPEN_UDID = "mp::openudid";
        // app-key specific (append appKey to the key)
        public static final String OPTOUT = "mp::optout::";
        public static final String USER_ATTRS = "mp::user_attrs::";
        public static final String DELETED_USER_ATTRS = "mp::deleted_user_attrs::";
        public static final String USER_IDENTITIES = "mp::user_ids::";
        public static final String FIRSTRUN = "mp::firstrun::";
        public static final String FIRSTINIT = "mp::firstinit";
        public static final String INITUPGRADE = "mp::initupgrade";
        public static final String UPLOAD_MODE = "mp::batch";
        public static final String PROPERTY_APP_VERSION = "mp::appversion";
        public static final String PROPERTY_OS_VERSION = "mp::osversion";
        public static final String PUSH_ENABLED = "mp::push_enabled";
        public static final String PUSH_SENDER_ID = "mp::push_sender_id";
        public static final String PIRATED = "mp::pirated";
        public static final String PUSH_ENABLE_SOUND = "mp::push::sound";
        public static final String PUSH_ENABLE_VIBRATION = "mp::push::vibration";
        public static final String PUSH_ICON = "mp::push::icon";
        public static final String PUSH_TITLE = "mp::push::title";
        public static final String SESSION_COUNTER = "mp::breadcrumbs::sessioncount";
        public static final String BREADCRUMB_LIMIT = "mp::breadcrumbs::limit";
        //MAT embedded
        public static final String MAT_EXISTING_USER = "mp::embedded::mat::existinguser";
        public static final String INSTALL_DATE = "mp::installdate";
        public static final String TOTAL_SINCE_UPGRADE = "mp::launch_since_upgrade";
        public static final String UPGRADE_DATE = "mp::upgrade_date";
        public static final String COUNTER_VERSION = "mp::version::counter";
        public static final String LAST_USE = "mp::lastusedate";
        public static final String TOTAL_RUNS = "mp::totalruns";
        public static final String Cookies = "mp::cookies";
        public static final String Mpid = "mp::mpid";
        public static final String CRASHED_IN_FOREGROUND = "mp::crashed_in_foreground";
        public static final String LTV = "mp::ltv";
        public static final String TIME_IN_BG = "mp::time_in_bg";
        public static final String PREVIOUS_SESSION_FOREGROUND = "mp::time_in_fg";
        public static final String NEXT_REQUEST_TIME = "mp::next_valid_request_time";
        public static final String ADTRUTH_PAYLOAD = "mp::adtruth::payload";
        public static final String ADTRUTH_LAST_TIMESTAMP = "mp::adtruth::timestamp";
        public static final String EVENT_COUNTER = "mp::events::counter";
        public static final String PREVIOUS_SESSION_ID = "mp::session::previous_id";
        public static final String PREVIOUS_SESSION_START = "mp::session::previous_start";
        public static final String API_KEY = "mp::config::apikey";
        public static final String API_SECRET = "mp::config::apisecret";
        public static final String MACRO_GN = "mp:macros::gn";
        public static final String MACRO_OAID = "mp:macros::oaid";
        public static final String MACRO_G = "mp:macros::g";
        public static final String MACRO_TS = "mp:macros::ts";
        public static final String MACRO_GLSB = "mp:macros::glsb";
        public static final String FIRST_RUN_INSTALL = "mp::firstrun::install";
    }

    public interface MiscStorageKeys {
        public static final String TOTAL_MEMORY = "mp::totalmem";
        public static final String MEMORY_THRESHOLD = "mp::memthreshold";
    }

    public interface Status {
        static final int UNKNOWN = -1;
        static final int READY = 1; // stream
        static final int BATCH_READY = 2;
        static final int UPLOADED = 3;
    }

    public interface StateTransitionType {
        public static final String STATE_TRANS_INIT = "app_init";
        public static final String STATE_TRANS_EXIT = "app_exit";
        public static final String STATE_TRANS_BG = "app_back";
        public static final String STATE_TRANS_FORE = "app_fore";
    }

    public interface Audience {
        public static final String ACTION_ADD = "add";
        public static final String API_AUDIENCE_LIST = "m";
        public static final String API_AUDIENCE_ID = "id";
        public static final String API_AUDIENCE_NAME = "n";
        public static final String API_AUDIENCE_ENDPOINTS = "s";
        public static final String API_AUDIENCE_MEMBERSHIPS = "c";
        public static final String API_AUDIENCE_ACTION = "a";
        public static final String API_AUDIENCE_MEMBERSHIP_TIMESTAMP = "ct";
    }

    public interface ProfileActions {
        public static final String LOGOUT = "logout";
        public static final String KEY = "t";
    }

    public interface External {
        public static final String APPLINK_KEY = "al_applink_data";
    }

    public interface Push {
        public static final String MESSAGE_TYPE_RECEIVED = "received";
        public static final String MESSAGE_TYPE_ACTION = "action";
    }
}
