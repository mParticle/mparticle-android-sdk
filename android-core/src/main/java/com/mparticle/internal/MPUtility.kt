package com.mparticle.internal

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.WorkerThread
import com.google.android.instantapps.InstantApps
import com.mparticle.MParticle
import com.mparticle.internal.JellybeanHelper.getAvailableMemory
import com.mparticle.internal.MPUtility.AdIdInfo.Advertiser
import com.mparticle.networking.MPConnection
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.math.BigInteger
import java.net.HttpURLConnection
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Mixin utility class responsible for generating all sorts of device information, mostly
 * used by the DeviceInfo and AppInfo dictionaries within batch messages.
 */
object MPUtility {
    const val NO_BLUETOOTH: String = "none"
    private var sOpenUDID: String? = null
    private val HEX_CHARS = "0123456789abcdef".toCharArray()
    private val TAG = MPUtility::class.java.toString()
    private var adInfoId: AdIdInfo? = null

    @JvmStatic
    fun getAvailableMemory(context: Context): Long {
        val mi = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        return mi.availMem
    }

    @JvmStatic
    fun isSystemMemoryLow(context: Context): Boolean {
        val mi = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        return mi.lowMemory
    }

    @JvmStatic
    fun getSystemMemoryThreshold(context: Context): Long {
        val mi = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        return mi.threshold
    }

    @JvmStatic
    val remainingHeapInBytes: Long
        get() {
            val runtime = Runtime.getRuntime()
            val usedMemBytes = (runtime.totalMemory() - runtime.freeMemory())
            val maxHeapSizeInBytes = runtime.maxMemory()
            return maxHeapSizeInBytes - usedMemBytes
        }

    @JvmStatic
    fun isEmpty(str: CharSequence?): Boolean {
        return str.isNullOrEmpty()
    }

    @JvmStatic
    fun isEmpty(jsonArray: JSONArray?): Boolean {
        return jsonArray == null || jsonArray.length() == 0
    }

    @JvmStatic
    fun isEmpty(jsonObject: JSONObject?): Boolean {
        return jsonObject == null || jsonObject.length() == 0
    }

    @JvmStatic
    fun isEmpty(map: Map<*, *>?): Boolean {
        return map.isNullOrEmpty()
    }

    @JvmStatic
    fun isEmpty(collection: Collection<*>?): Boolean {
        return collection.isNullOrEmpty()
    }

    @JvmStatic
    @WorkerThread
    fun getAdIdInfo(context: Context): AdIdInfo? {
        adInfoId?.let {
            return it
        }

        val packageName = context.packageName
        val packageManager = context.packageManager
        val installerName = packageManager.getInstallerPackageName(packageName)
        if ((installerName != null && installerName.contains("com.amazon.venezia")) ||
            "Amazon" == Build.MANUFACTURER
        ) {
            adInfoId = getAmazonAdIdInfo(context)
            if (adInfoId == null) {
                return getGoogleAdIdInfo(context)
            }
            return adInfoId
        } else {
            adInfoId = getGoogleAdIdInfo(context)
            if (adInfoId == null) {
                return getAmazonAdIdInfo(context)
            }
            return adInfoId
        }
    }

    private fun getGoogleAdIdInfo(context: Context): AdIdInfo? {
        try {
            val AdvertisingIdClient = Class
                .forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
            val getAdvertisingInfo = AdvertisingIdClient.getMethod(
                "getAdvertisingIdInfo",
                Context::class.java
            )
            val advertisingInfo = getAdvertisingInfo.invoke(null, context)
            val isLimitAdTrackingEnabled = advertisingInfo?.javaClass?.getMethod(
                "isLimitAdTrackingEnabled"
            )
            val limitAdTrackingEnabled = isLimitAdTrackingEnabled
                ?.invoke(advertisingInfo) as Boolean
            val getId = advertisingInfo.javaClass.getMethod("getId")
            val advertisingId = getId.invoke(advertisingInfo) as String
            return AdIdInfo(advertisingId, limitAdTrackingEnabled, Advertiser.GOOGLE)
        } catch (e: Exception) {
            Logger.info(TAG, "Could not locate Google Play Ads Identifier library")
        }
        return null
    }

    private fun getAmazonAdIdInfo(context: Context): AdIdInfo? {
        // https://developer.amazon.com/public/solutions/platforms/fire-os/docs/fire-os-advertising-id
        // https://forums.developer.amazon.com/articles/18194/using-the-advertising-id-in-your-app.html
        var advertisingID: String? = ""
        val limitAdTracking: Boolean
        try {
            val cr = context.contentResolver
            limitAdTracking = if ((Settings.Secure.getInt(cr, "limit_ad_tracking", 0) == 0)) false else true
            advertisingID = Settings.Secure.getString(cr, "advertising_id")
            advertisingID?.let {
                return AdIdInfo(advertisingID, limitAdTracking, Advertiser.AMAZON)
            }
        } catch (e: Exception) {
            Logger.info(TAG, "Could not locate Amazon ID on device: " + e.message)
        }
        return null
    }

    @JvmStatic
    val isInDaylightSavings: Boolean
        get() = TimeZone.getDefault().inDaylightTime(Date())

    @JvmStatic
    fun isEqual(field: Any?, field1: Any?): Boolean {
        return field == field1 || (field != null && field == field1)
    }

    @JvmStatic
    fun getGpsEnabled(context: Context): String? {
        if (PackageManager.PERMISSION_GRANTED == context
                .checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER).toString()
        } else {
            return null
        }
    }

    @JvmStatic
    @SuppressLint("MissingPermission")
    fun getNetworkType(context: Context, telephonyManager: TelephonyManager?): Int? {
        return if (telephonyManager != null && checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            telephonyManager.networkType
        } else {
            null
        }
    }

    @JvmStatic
    fun getAvailableInternalDisk(context: Context): Long {
        val path = Environment.getDataDirectory()
        return getDiskSpace(context, path)
    }

    @JvmStatic
    fun getAvailableExternalDisk(context: Context): Long {
        val path = context.getExternalFilesDir(null) ?: return 0
        return getDiskSpace(context, path)
    }

    @JvmStatic
    fun getAppVersionName(context: Context): String? {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionName
        } catch (e: Exception) {
            // ignore missing data
        }
        return "unknown"
    }

    @JvmStatic
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, UnsupportedEncodingException::class)
    fun hmacSha256Encode(key: String, data: String): String {
        val sha256_HMAC = Mac.getInstance("HmacSHA256")
        val secret_key = SecretKeySpec(key.toByteArray(charset("utf-8")), "HmacSHA256")
        sha256_HMAC.init(secret_key)
        return asHex(sha256_HMAC.doFinal(data.toByteArray(charset("utf-8"))))
    }

    private fun asHex(buf: ByteArray): String {
        val chars = CharArray(2 * buf.size)
        for (i in buf.indices) {
            chars[2 * i] = HEX_CHARS[(buf[i].toInt() and 0xF0) ushr 4]
            chars[2 * i + 1] = HEX_CHARS[buf[i].toInt() and 0x0F]
        }
        return String(chars)
    }

  @JvmStatic
    fun getJsonResponse(connection: MPConnection): JSONObject? {
        return try {
            getJsonResponse(connection.inputStream)
        } catch (ex: IOException) {
            getJsonResponse(connection.errorStream)
        }
    }

    fun getJsonResponse(connection: HttpURLConnection): JSONObject? {
        return try {
            getJsonResponse(connection.inputStream)
        } catch (ex: IOException) {
            getJsonResponse(connection.errorStream)
        }
    }

    fun getJsonResponse(inputStream: InputStream?): JSONObject? {
        return try {
            val responseBuilder = StringBuilder()
            inputStream?.let {
                val reader = BufferedReader(InputStreamReader(it))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    responseBuilder.append(line).append('\n')
                }
                reader.close()
            } ?: return null

            JSONObject(responseBuilder.toString())
        } catch (ex: IOException) {
            null
        } catch (jse: JSONException) {
            null
        }catch (e: Exception){
            null
        }
    }

    fun getDiskSpace(context: Context, path: File): Long {
        if (isInstantApp(context)) {
            return 0L
        }
        var availableSpace = -1L
        val stat = StatFs(path.path)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            availableSpace = getAvailableMemory(stat)
        }
        if (availableSpace == 0L) {
            availableSpace = stat.availableBlocks.toLong() * stat.blockSize.toLong()
        }
        return availableSpace
    }

    fun getErrorMessage(connection: HttpURLConnection): String? {
        val `is` = connection.errorStream ?: return null
        val responseBuilder = StringBuilder()
        val `in` = BufferedReader(InputStreamReader(`is`))
        var line: String
        try {
            while ((`in`.readLine().also { line = it }) != null) {
                responseBuilder.append(line + '\n')
            }
            `in`.close()
            return responseBuilder.toString()
        } catch (e: Exception) {
            return e.message
        }
    }

    @JvmStatic
    fun millitime(): Long {
        return TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS)
    }

    @JvmStatic
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    fun getAndroidID(context: Context): String? {
        return if (MParticle.isAndroidIdEnabled()) {
            Settings.Secure.getString(context.contentResolver, "android_id")
        } else {
            null
        }
    }

    @JvmStatic
    val timeZone: String?
        get() {
            try {
                //Some Android 8 devices crash here for no clear reason.
                return TimeZone.getDefault().getDisplayName(false, 0)
            } catch (ignored: Exception) {
            } catch (e: AssertionError) {
            }
            return null
        }

    @JvmStatic
    fun getOrientation(context: Context?): Int {
        var orientation = Configuration.ORIENTATION_UNDEFINED
       context?.let {
            val displayMetrics = context.resources.displayMetrics
            orientation = if (displayMetrics.widthPixels == displayMetrics.heightPixels) {
                Configuration.ORIENTATION_SQUARE
            } else {
                if (displayMetrics.widthPixels < displayMetrics.heightPixels) {
                    Configuration.ORIENTATION_PORTRAIT
                } else {
                    Configuration.ORIENTATION_LANDSCAPE
                }
            }
        }
        return orientation
    }

    @JvmStatic
    fun getTotalMemory(context: Context): Long {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            getTotalMemoryJB(context)
        } else {
            totalMemoryPreJB
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun getTotalMemoryJB(context: Context): Long {
        val mi = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        return mi.totalMem
    }

    val totalMemoryPreJB: Long
        get() {
            val str1 = "/proc/meminfo"
            val str2: String
            val arrayOfString: Array<String>
            var initial_memory: Long = 0
            try {
                val localFileReader = FileReader(str1)
                val localBufferedReader = BufferedReader(localFileReader, 8192)
                str2 = localBufferedReader.readLine() //meminfo
                arrayOfString = str2.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                initial_memory = (arrayOfString[1].toInt() * 1024).toLong()
                localBufferedReader.close()
                return initial_memory
            } catch (e: IOException) {
                return -1
            }
        }

    @JvmStatic
    fun getOpenUDID(context: Context): String? {
        if (sOpenUDID == null) {
            val sharedPrefs = context.getSharedPreferences(
                Constants.PREFS_FILE, Context.MODE_PRIVATE
            )
            sOpenUDID = sharedPrefs.getString(Constants.PrefKeys.OPEN_UDID, null)
            if (sOpenUDID == null) {
                sOpenUDID = getAndroidID(context)
                if (sOpenUDID == null) sOpenUDID = generatedUdid

                val editor = sharedPrefs.edit()
                editor.putString(Constants.PrefKeys.OPEN_UDID, sOpenUDID)
                editor.apply()
            }
        }
        return sOpenUDID
    }

    @JvmStatic
    fun getRampUdid(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(
            Constants.PREFS_FILE, Context.MODE_PRIVATE
        )
        var rampUdid = sharedPrefs.getString(Constants.PrefKeys.DEVICE_RAMP_UDID, null)
        if (rampUdid == null) {
            rampUdid = generatedUdid
            val editor = sharedPrefs.edit()
            editor.putString(Constants.PrefKeys.DEVICE_RAMP_UDID, rampUdid)
            editor.apply()
        }
        return rampUdid
    }

    val generatedUdid: String
        get() {
            val localSecureRandom = SecureRandom()
            return BigInteger(64, localSecureRandom).toString(16)
        }

    @JvmStatic
    fun getBuildUUID(versionCode: String?): String {
        var versionCode = versionCode
        if (versionCode == null) {
            versionCode = DeviceAttributes.UNKNOWN
        }
        return try {
            UUID.nameUUIDFromBytes(versionCode.toByteArray()).toString()
        } catch (e: AssertionError) {

            //Some devices do not have MD5 and will throw a NoSuchAlgorithmException.
            DeviceAttributes.UNKNOWN
        }
    }

    @JvmStatic
    fun isTablet(context: Context): Boolean {
        return ((context.resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE)
    }

    @JvmStatic
    fun hasNfc(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    }

    @JvmStatic
    fun getBluetoothVersion(context: Context): String {
        var bluetoothVersion = NO_BLUETOOTH
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) && (context.packageManager.hasSystemFeature("android.hardware.bluetooth_le"))) {
            bluetoothVersion = "ble"
        } else if (context.packageManager.hasSystemFeature("android.hardware.bluetooth")) {
            bluetoothVersion = "classic"
        }
        return bluetoothVersion
    }

    @JvmStatic
    val isPhoneRooted: Boolean
        get() {
            // Get from build customAttributes

            val buildTags = Build.TAGS
            if (buildTags != null && buildTags.contains("test-keys")) {
                return true
            }

            var bool = false
            val arrayOfString1 = arrayOf(
                "/sbin/",
                "/system/bin/",
                "/system/xbin/",
                "/data/local/xbin/",
                "/data/local/bin/",
                "/system/sd/xbin/",
                "/system/bin/failsafe/",
                "/data/local/"
            )
            for (str in arrayOfString1) {
                val localFile = File(str + "su")
                if (localFile.exists()) {
                    bool = true
                    break
                }
            }
            return bool
        }

    @JvmStatic
    fun mpHash(input: String?): Int {
        var hash = 0

        if (input.isNullOrEmpty()) return hash

        val chars = input.lowercase(Locale.getDefault()).toCharArray()

        for (c in chars) {
            hash = ((hash shl 5) - hash) + c.code
        }

        return hash
    }

    @JvmStatic
    fun hasTelephony(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    @JvmStatic
    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(context: Context): Boolean {
        if (checkPermission(context, Manifest.permission.BLUETOOTH)) {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter != null) {
                try {
                    return mBluetoothAdapter.isEnabled
                } catch (se: SecurityException) {
                }
            }
        }
        return false
    }

    @JvmStatic
    fun checkPermission(context: Context, permission: String): Boolean {
        val res = context.checkCallingOrSelfPermission(permission)
        return (res == PackageManager.PERMISSION_GRANTED)
    }

    val isGmsAdIdAvailable: Boolean
        get() {
            try {
                Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
                return true
            } catch (ignored: ClassNotFoundException) {

            }
            return false
        }

    val isSupportLibAvailable: Boolean
        get() {
            try {
                Class.forName("android.support.v4.app.FragmentActivity")
                return true
            } catch (ignored: ClassNotFoundException) {
            }
            return false
        }

    @JvmStatic
    val isFirebaseAvailable: Boolean
        get() = if (isFirebaseAvailablePostV21 || isFirebaseAvailablePreV21) {
            true
        } else {
            false
        }

    @JvmStatic
    val isFirebaseAvailablePostV21: Boolean
        get() {
            try {
                Class.forName("com.google.firebase.messaging.FirebaseMessaging")
                return true
            } catch (ignored: ClassNotFoundException) {
            }
            return false
        }

    @JvmStatic
    val isFirebaseAvailablePreV21: Boolean
        get() {
            try {
                Class.forName("com.google.firebase.iid.FirebaseInstanceId")
                return true
            } catch (ignored: ClassNotFoundException) {
            }
            return false
        }

    @JvmStatic
    val isInstallRefApiAvailable: Boolean
        get() {
            try {
                Class.forName("com.android.installreferrer.api.InstallReferrerStateListener")
                return true
            } catch (ignored: Exception) {
            }
            return false
        }

    @JvmStatic
    fun hashFnv1A(data: ByteArray): BigInteger {
        val INIT64 = BigInteger("cbf29ce484222325", 16)
        val PRIME64 = BigInteger("100000001b3", 16)
        val MOD64 = BigInteger("2").pow(64)

        var hash = INIT64

        for (b in data) {
            hash = hash.xor(BigInteger.valueOf((b.toInt() and 0xff).toLong()))
            hash = hash.multiply(PRIME64).mod(MOD64)
        }

        return hash
    }

    @JvmStatic
    fun isServiceAvailable(context: Context, service: Class<*>?): Boolean {
        val packageManager = context.packageManager
        val intent = Intent(context, service)
        val resolveInfo: List<*> =
            packageManager.queryIntentServices(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolveInfo.size > 0
    }

    fun wrapExtras(extras: Bundle?): JSONObject? {
        try {
            if (extras != null && !extras.isEmpty) {
                val parameters = JSONObject()
                for (key in extras.keySet()) {
                    var value: Any?
                    if ((extras.getBundle(key).also { value = it }) != null) {
                        try {
                            parameters.put(key, wrapExtras(value as Bundle))
                        } catch (ignored: JSONException) {
                        }
                    } else if ((extras[key].also { value = it }) != null) {
                        val stringVal = value.toString()
                        if ((stringVal.length < 500)) {
                            try {
                                parameters.put(key, stringVal)
                            } catch (ignored: JSONException) {
                            }
                        }
                    }
                }
                return parameters
            } else {
                return null
            }
        }catch (e:Exception){
        }
        return null
    }

    @JvmStatic
    fun mapToJson(map: Map<String, *>?): JSONObject? {
        if (map == null) {
            return null
        }
        val attrs = JSONObject()
        for ((key, value) in map) {
            try {
                if (value is List<*>) {
                    val array = JSONArray()
                    for (v in value) {
                        array.put(v)
                    }
                    attrs.put(key, array)
                } else if (value != null) {
                    attrs.put(key, value.toString())
                } else {
                    attrs.put(key, value)
                }
            } catch (ignore: JSONException) {
            }
        }
        return attrs
    }

    @JvmStatic
    fun isAppDebuggable(context: Context): Boolean {
        return (0 != (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE))
    }

    @JvmStatic
    fun isDevEnv(): Boolean {
        return ConfigManager.getEnvironment() == MParticle.Environment.Development
    }

    /**
     * This method makes sure the constraints on event attributes are enforced. A JSONObject version
     * of the attributes is return with data that exceeds the limits removed.
     * NOTE: Non-string attributes are not converted to strings, currently.
     *
     * @param attributes the user-provided JSONObject
     * @return a cleansed copy of the JSONObject
     */
    @JvmStatic
    fun enforceAttributeConstraints(attributes: Map<String, String>?): JSONObject? {
        if (null == attributes) {
            return null
        }
        val checkedAttributes = JSONObject()
        for ((key, value) in attributes) {
            setCheckedAttribute(checkedAttributes, key, value, false, false)
        }
        return checkedAttributes
    }

    @JvmStatic
    fun setCheckedAttribute(attributes: JSONObject?, key: String?, value: Any?, increment: Boolean, userAttribute: Boolean): Boolean {
        return setCheckedAttribute(attributes, key, value, false, increment, userAttribute)
    }

    fun setCheckedAttribute(
        attributes: JSONObject?,
        key: String?,
        value: Any?,
        caseInsensitive: Boolean,
        increment: Boolean,
        userAttribute: Boolean
    ): Boolean {
        var key = key
        var value = value
        if (null == attributes || null == key) {
            return false
        }
        try {
            if (caseInsensitive) {
                key = findCaseInsensitiveKey(attributes, key)
            }
            if (value != null) {
                val stringValue = value.toString()
                if (stringValue.length > Constants.LIMIT_ATTR_VALUE) {
                    Logger.error("Attribute value length exceeds limit. Discarding attribute: $key")
                    return false
                }
            }
            if ((key?.length ?: 0) > Constants.LIMIT_ATTR_KEY) {
                Logger.error("Attribute name length exceeds limit. Discarding attribute: $key")
                return false
            }
            if (value == null) {
                value = JSONObject.NULL
            }
            if (increment) {
                val oldValue = attributes.optString(key, "0")
                val oldInt = oldValue.toInt()
                value = (value as Int + oldInt).toString()
            }
            attributes.put(key, value)
        } catch (e: JSONException) {
            Logger.error("JSON error processing attributes. Discarding attribute: $key")
            return false
        } catch (nfe: NumberFormatException) {
            Logger.error("Attempted to increment a key that could not be parsed as an integer: $key")
            return false
        } catch (e: Exception) {
            Logger.error("Failed to add attribute: " + e.message)
            return false
        }
        return true
    }

    fun findCaseInsensitiveKey(jsonObject: JSONObject, key: String): String {
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val currentKey = keys.next()
            if (currentKey.equals(key, ignoreCase = true)) {
                return currentKey
            }
        }
        return key
    }

    fun isInstantApp(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.packageManager.isInstantApp
        }
        try {
            Class.forName("com.google.android.instantapps.InstantApps")
            return object : SyncRunnable<Boolean> {
                override fun run(): Boolean {
                    return InstantApps.isInstantApp(context)
                }
            }.run()
        } catch (ignored: ClassNotFoundException) {
            try {
                Class.forName("com.google.android.instantapps.supervisor.InstantAppsRuntime")
                return true
            } catch (a: ClassNotFoundException) {
                return false
            }
        }
    }

    @JvmStatic
    fun containsNullKey(map: Map<*, *>): Boolean {
        try {
            return map.containsKey(null)
        } catch (ignore: RuntimeException) {
            //At this point we should be able to conclude that the implementation of the map does
            //not allow for null keys, if you get an exception when you check for a null key, but
            //there is no guarantee in the Map documentation, so we still have to check by hand.
            for ((key) in ArrayList(map.entries)) {
                if (key == null) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun getProp(key: String): String? {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java)
            return getMethod.invoke(systemProperties, key) as? String
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    fun addNumbers(number1: Number, number2: Number): Number {
        return if (number1 is Double || number2 is Double) {
            number1.toDouble() + number2.toDouble()
        } else if (number1 is Float || number2 is Float) {
            number1.toFloat() + number2.toFloat()
        } else if (number1 is Long || number2 is Long) {
            number1.toLong() + number2.toLong()
        } else {
            number1.toInt() + number2.toInt()
        }
    }

    @JvmStatic
    fun toNumberOrString(stringValue: String?): Any? {
        if (stringValue == null) {
            return null
        }
        for (c in stringValue.toCharArray()) {
            if (!Character.isDigit(c) && c != '.' && c != '-') {
                return stringValue
            }
        }
        try {
            return stringValue.toInt()
        } catch (ignored: NumberFormatException) {

        }
        try {
            return stringValue.toDouble()
        } catch (ignored: NumberFormatException) {

        }

        return stringValue
    }

    class AdIdInfo(@JvmField val id: String, @JvmField val isLimitAdTrackingEnabled: Boolean, @JvmField val advertiser: Advertiser) {
        enum class Advertiser(@JvmField var descriptiveName: String) {
            AMAZON("Amazon"),
            GOOGLE("Google Play Store")
        }
    }

    private interface SyncRunnable<T> {
        fun run(): T
    }
}