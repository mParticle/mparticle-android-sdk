package com.mparticle;

/* package-private */ class Constants {

    /* package-private */ final static String LOG_TAG = "mParticleAPI";

    /* package-private */ static final String MPARTICLE_VERSION = "0.1";


    // maximum number of events per session
    public static final int EVENT_LIMIT = 1000;
    // delay (millis) before processing uploads to allow app to get started
    public static final long INITIAL_UPLOAD_DELAY = 1000 * 30;  // 30 seconds
    // interval (seconds) between uploads if not specified
    public static final long DEFAULT_UPLOAD_INTERVAL = 60 * 10; // 10 minutes
    // name of the preferences file
    public static final String PREFS_FILE = "mParticlePrefs";

    public static final int LIMIT_ATTR_COUNT=10;
    public static final int LIMIT_ATTR_NAME=255;
    public static final int LIMIT_ATTR_VALUE=255;
    public static final int LIMIT_NAME=255;

    public interface MessageType {
        public static final String SESSION_START = "ss";
        public static final String SESSION_END = "se";
        public static final String CUSTOM_EVENT = "e";
        public static final String SCREEN_VIEW = "v";
        public static final String OPT_OUT = "o";
        public static final String ERROR = "x";
        public static final String PUSH_REGISTRATION = "pr";
        public static final String REQUEST_HEADER = "h";
        public static final String RESPONSE_HEADER = "rh";
        public static final String HTTP_COMMAND = "hc";
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
        public static final String NAME = "n";
        // event timing
        public static final String EVENT_START_TIME = "est";
        public static final String EVENT_DURATION = "el";
        // location
        public static final String LOCATION = "lc";
        public static final String DATA_CONNECTION = "dct";
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "lng";
        public static final String ACCURACY = "acc";
        // batch details
        public static final String APPLICATION_KEY = "a";
        public static final String MPARTICLE_VERSION = "sdk";
        public static final String APP_INFO = "ai";
        public static final String DEVICE_INFO = "di";
        public static final String USER_ATTRIBUTES = "ua";
        // application info
        public static final String APP_NAME = "an";
        public static final String APP_VERSION = "av";
        public static final String APP_PACKAGE_NAME = "apn";
        public static final String APP_INSTALLER_NAME = "ain";
        public static final String MPARTICLE_INSTALL_TIME = "ict";
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
        public static final String DEVICE_COUNTRY = "dc";
        public static final String DEVICE_LOCALE_COUNTRY = "dlc";
        public static final String DEVICE_LOCALE_LANGUAGE = "dll";
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
        // uploading
        public static final String MESSAGES = "msgs";
        public static final String URL = "u";
        public static final String METHOD = "m";
        public static final String POST = "d";
        public static final String CLEAR_HEADERS = "c";
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
        // app-key specific (append appKey to the key)
        public static final String OPTOUT = "mp::optout::";
        public static final String USER_ATTRS = "mp::user_attrs::";
    }

    public interface Status {
        static final int READY = 1;
        static final int BATCH_READY = 2;
    }

}
