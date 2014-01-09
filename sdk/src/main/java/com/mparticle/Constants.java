package com.mparticle;

/* package-private */class Constants {

    public final static String LOG_TAG = "mParticleAPI";

    public static final String MPARTICLE_VERSION = "0.1";

    // maximum number of events per session
    public static final int EVENT_LIMIT = 1000;
    // delay (millis) before processing uploads to allow app to get started
    public static final long INITIAL_UPLOAD_DELAY = 30 * 1000; // 30 seconds
    // interval (millis) between uploads if not specified
    public static final long DEFAULT_UPLOAD_INTERVAL = 10 * 60 * 1000; // 10 minutes
    // internal (millis) between uploads when in debug mode
    public static final int DEBUG_UPLOAD_INTERVAL = 3 * 1000; // 3 seconds
    // name of the preferences file
    public static final String PREFS_FILE = "mParticlePrefs";
    // misc persistence
    public static final String MISC_FILE = "mParticleMisc";

    public static final int LIMIT_ATTR_COUNT = 100;
    public static final int LIMIT_ATTR_NAME = 255;
    public static final int LIMIT_ATTR_VALUE = 255;
    public static final int LIMIT_NAME = 255;

    public static final int DB_CLEANUP_EXPIRATION = 3 * 24 * 60 * 60 * 1000; // 3 days old
    public static final long DB_CLEANUP_INTERVAL = 1 * 24 * 60 * 60 * 1000; // 1 day

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
    }

    public interface MessageKey {
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
        public static final String APPLICATION_KEY = "a";
        public static final String MPARTICLE_VERSION = "sdk";
        public static final String APP_INFO = "ai";
        public static final String DEVICE_INFO = "di";
        public static final String USER_ATTRIBUTES = "ua";
        public static final String USER_IDENTITIES = "ui";
        // user identity
        public static final String IDENTITY_NAME = "n";
        public static final String IDENTITY_VALUE= "i";
        // application info
        public static final String APP_NAME = "an";
        public static final String APP_VERSION = "av";
        public static final String APP_PACKAGE_NAME = "apn";
        public static final String APP_INSTALLER_NAME = "ain";
        public static final String MPARTICLE_INSTALL_TIME = "ict";
        public static final String INSTALL_REFERRER = "ir";
        // device info
        public static final String BUILD_ID = "bid";
        public static final String BRAND = "b";
        public static final String PRODUCT = "p";
        public static final String DEVICE = "dn";
        public static final String DEVICE_ID = "duid";
        public static final String MANUFACTURER = "dma";
        public static final String PLATFORM = "dp";
        public static final String OS_VERSION = "dosv";
        public static final String MODEL = "dmdl";
        public static final String SCREEN_HEIGHT = "dsh";
        public static final String SCREEN_WIDTH = "dsw";
        public static final String SCREEN_DPI = "dpi";
        public static final String DEVICE_COUNTRY = "dc";
        public static final String DEVICE_LOCALE_COUNTRY = "dlc";
        public static final String DEVICE_LOCALE_LANGUAGE = "dll";
        public static final String DEVICE_ROOTED = "jb";
        // state info
        public static final String STATE_INFO_KEY = "cs";
        public static final String STATE_INFO_CPU = "cpu";
        public static final String STATE_INFO_AVAILABLE_MEMORY = "sma";
        public static final String STATE_INFO_TOTAL_MEMORY = "tsm";
        public static final String STATE_INFO_BATTERY_LVL = "bl";
        public static final String STATE_INFO_TIME_SINCE_START = "tss";
        public static final String STATE_INFO_AVAILABLE_DISK = "fds";
        public static final String STATE_INFO_APP_MEMORY = "amt";
        public static final String STATE_INFO_GPS = "gps";
        public static final String STATE_INFO_BAR_ORIENTATION = "sbo";
        public static final String STATE_INFO_DATA_CONNECTION = "dct";
        public static final String STATE_INFO_ORIENTATION = "so";
        public static final String STATE_INFO_MEMORY_LOW = "sml";

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

    }

    public interface PrefKeys {
        // common
        public static final String INSTALL_TIME = "mp::ict";
        public static final String PUSH_REGISTRATION_ID = "mp::push_reg_id";
        public static final String INSTALL_REFERRER = "mp::install_referrer";
        // app-key specific (append appKey to the key)
        public static final String OPTOUT = "mp::optout::";
        public static final String USER_ATTRS = "mp::user_attrs::";
        public static final String USER_IDENTITIES = "mp::user_ids::";
        public static final String FIRSTRUN = "mp::firstrun::";
        public static final String UPLOAD_MODE = "mp::batch";
    }

    public interface MiscStorageKeys {
        public static final String TOTAL_MEMORY = "mp::totalmem";
    }

    public interface Status {
    	static final int UNKNOWN = -1;
        static final int READY = 1; // stream
        static final int BATCH_READY = 2;
        static final int UPLOADED = 3;
    }

    // keys used in the (optional) mparticle.properties config file
    public interface ConfigKeys {
        // mParticleAPI
        public static final String API_KEY = "api_key";
        public static final String API_SECRET = "api_secret";
        public static final String DEBUG_MODE = "debug_mode";
        public static final String SESSION_TIMEOUT = "session_timeout";
        public static final String ENABLE_CRASH_REPORTING = "enable_crash_reporting";
        public static final String ENABLE_PUSH_NOTIFICATIONS = "enable_push_notifications";
        public static final String PUSH_NOTIFICATION_SENDER_ID = "push_notification_sender_id";
        // MessageManager
        public static final String DEBUG_UPLOAD_INTERVAL = "debug_upload_interval";
        public static final String UPLOAD_INTERVAL = "upload_interval";
        public static final String ENABLE_SSL = "enable_secure_transport";
        public static final String PROXY_HOST = "proxy_host";
        public static final String PROXY_PORT = "proxy_port";
        public static final String ENABLE_COMPRESSION = "enable_compression";
    }

    // these keys are expected by the GCMIntentService for push notifications
    public interface GCMNotificationKeys {
        public static final String TITLE = "mp::notification::title";
        public static final String TEXT = "mp::notification::text";
    }

}
