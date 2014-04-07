package com.mparticle;

/* package-private */class Constants {

    public final static String LOG_TAG = "mParticleAPI";

    public static final String MPARTICLE_VERSION = BuildConfig.VERSION_NAME;

    // maximum number of events per session
    public static final int EVENT_LIMIT = 1000;
    // delay (millis) before processing uploads to allow app to get started
    public static final long INITIAL_UPLOAD_DELAY = 5 * 1000; // 30 seconds
    // interval (millis) between uploads if not specified
    public static final long DEFAULT_UPLOAD_INTERVAL = 10 * 60 * 1000; // 10 minutes
    // internal (millis) between uploads when in debug mode
    public static final int DEBUG_UPLOAD_INTERVAL = 3 * 1000; // 3 seconds
    // preferences persistence
    public static final String PREFS_FILE = "mParticlePrefs";
    // misc persistence
    public static final String MISC_FILE = "mParticleMisc";

    public static final int LIMIT_ATTR_COUNT = 100;
    public static final int LIMIT_ATTR_NAME = 255;
    public static final int LIMIT_ATTR_VALUE = 255;
    public static final int LIMIT_NAME = 255;

    public static final int DB_CLEANUP_EXPIRATION = 3 * 24 * 60 * 60 * 1000; // 3 days old
    public static final long DB_CLEANUP_INTERVAL = 1 * 24 * 60 * 60 * 1000; // 1 day

    interface MethodName {
        public static final String METHOD_NAME = "$MethodName";
        public static final String LOG_LTV = "LogLTVIncrease";
        public static final String LOG_ECOMMERCE = "LogEcommerceTransaction";
    }

    interface MessageType {
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
    }

    interface MessageKey {
        // common
        public static final String TYPE = "dt";
        public static final String ID = "id";
        public static final String TIMESTAMP = "ct";
        public static final String SESSION_START_TIMESTAMP = "sct";
        public static final String SESSION_ID = "sid";
        public static final String SESSION_LENGTH = "sl";
        public static final String ATTRIBUTES = "attrs";
        public static final String EVENT_TYPE = "et";
        public static final String NAME = "n";
        public static final String DEBUG = "dbg";
        public static final String OPT_OUT_HEADER = "oo";
        public static final String PREVIOUS_SESSION_LENGTH = "psl";
        // referrer
        public static final String LAUNCH_REFERRER = "lr";
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
        // user identity
        public static final String IDENTITY_NAME = "n";
        public static final String IDENTITY_VALUE = "i";
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
        public static final String PUSH_REGISTER_FLAG = "r";
        public static final String APP_STATE = "as";
        //state transition
        public static final String STATE_TRANSITION_TYPE = "t";
        public static final String PAYLOAD = "pay";
        //screen view
        public static final String SCREEN_STARTED = "t";

        public static final String EVENT_CATEGORY = "$Category";
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
        public static final String CONFIG_SESSION_TIMEOUT = "stl";
        public static final String CONFIG_UPLOAD_INTERVAL = "uitl";
    }

    interface PrefKeys {
        // common
        public static final String INSTALL_TIME = "mp::ict";
        public static final String PUSH_REGISTRATION_ID = "mp::push_reg_id";
        public static final String INSTALL_REFERRER = "mp::install_referrer";
        public static final String OPEN_UDID = "mp::openudid";
        // app-key specific (append appKey to the key)
        public static final String OPTOUT = "mp::optout::";
        public static final String USER_ATTRS = "mp::user_attrs::";
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
    }

    interface MiscStorageKeys {
        public static final String TOTAL_MEMORY = "mp::totalmem";
        public static final String MEMORY_THRESHOLD = "mp::memthreshold";
    }

    interface Status {
        static final int UNKNOWN = -1;
        static final int READY = 1; // stream
        static final int BATCH_READY = 2;
        static final int UPLOADED = 3;
    }

    // keys used in the (optional) mparticle.properties config file
    interface ConfigKeys {
        // mParticleAPI
        public static final String API_KEY = "api_key";
        public static final String API_SECRET = "api_secret";
        public static final String DEBUG_MODE = "debug_mode";
        public static final String SESSION_TIMEOUT = "session_timeout";
        public static final String ENABLE_CRASH_REPORTING = "enable_crash_reporting";
        public static final String ENABLE_PUSH_NOTIFICATIONS = "enable_push_notifications";

        // MessageManager
        public static final String DEBUG_UPLOAD_INTERVAL = "debug_upload_interval";
        public static final String UPLOAD_INTERVAL = "upload_interval";
        public static final String ENABLE_SSL = "enable_secure_transport";
        public static final String PROXY_HOST = "proxy_host";
        public static final String PROXY_PORT = "proxy_port";
        public static final String ENABLE_COMPRESSION = "enable_compression";
    }

    // these keys are expected by the MPService for push notifications
    interface GCMNotificationKeys {
        public static final String TITLE = "mp::notification::title";
        public static final String TEXT = "mp::notification::text";
    }

    interface StateTransitionType {
        public static final String STATE_TRANS_INIT = "app_init";
        public static final String STATE_TRANS_EXIT = "app_exit";
        public static final String STATE_TRANS_BG = "app_back";
        public static final String STATE_TRANS_FORE = "app_fore";
    }

}
