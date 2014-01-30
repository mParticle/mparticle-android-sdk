package com.mparticle;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.PrefKeys;
import com.mparticle.Constants.Status;
import com.mparticle.MParticleDatabase.CommandTable;
import com.mparticle.MParticleDatabase.MessageTable;
import com.mparticle.MParticleDatabase.SessionTable;
import com.mparticle.MParticleDatabase.UploadTable;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/* package-private */final class UploadHandler extends Handler {

    private static final String TAG = Constants.LOG_TAG;

    private final MParticleDatabase mDB;
    private final SharedPreferences mPreferences;

    private final Context mContext;
    private final String mApiKey;
    private final String mSecret;

    private HttpContext mHttpContext;
    private HttpHost mProxyHost;
    private JSONObject mAppInfo;
    private JSONObject mDeviceInfo;
    private Proxy mProxy;
    private final ConnectivityManager mConnectivyManager;

    private boolean mAccessNetworkStateAvailable = true;

    public static final int UPLOAD_MESSAGES = 1;
    public static final int CLEANUP = 2;
    public static final int UPLOAD_HISTORY = 3;
    public static final int UPDATE_CONFIG = 4;

    public static final String HEADER_SIGNATURE = "x-mp-signature";

    public static final String SECURE_SERVICE_SCHEME = "https";
    public static final String SECURE_SERVICE_HOST = "nativesdk.mparticle.com";
    public static final String DEBUG_SERVICE_SCHEME = "http";
    public static final String DEBUG_SERVICE_HOST = "api-qa.mparticle.com";

    public static final String SERVICE_VERSION = "v1";

    private ConfigManager mConfigManager;

    public static final String SQL_DELETABLE_MESSAGES = String.format(
            "%s=? and (%s='NO-SESSION')",
            MessageTable.API_KEY,
            MessageTable.SESSION_ID);


    public UploadHandler(Context context, Looper looper, ConfigManager configManager) {
        super(looper);
        mConfigManager = configManager;

        mContext = context.getApplicationContext();
        mApiKey = mConfigManager.getApiKey();
        mSecret = mConfigManager.getApiSecret();

        mDB = new MParticleDatabase(mContext);
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mHttpContext = new BasicHttpContext();
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, new PersistentCookieStore(mContext));

        mConnectivyManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mAppInfo = DeviceAttributes.collectAppInfo(mContext);
        mDeviceInfo = DeviceAttributes.collectDeviceInfo(mContext);

        if (PackageManager.PERMISSION_GRANTED != mContext
                .checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)) {
            Log.w(TAG, "Application manifest should require ACCESS_NETWORK_STATE permission");
            mAccessNetworkStateAvailable = false;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case UPDATE_CONFIG:
                fetchConfig();
                break;
            case UPLOAD_MESSAGES:
                if (mConfigManager.isDebug()) {
                    Log.d(TAG, "Performing " + (msg.arg1 == 0 ? "periodic" : "manual") + " upload for " + mApiKey);
                }
                boolean needsHistory = false;
                // execute all the upload steps
                long uploadInterval = mConfigManager.getUploadInterval();
                if (uploadInterval > 0 || msg.arg1 == 1) {
                    prepareUploads(false);
                    needsHistory = processUploads(false);
                    processCommands();
                    if (needsHistory) {
                        this.sendEmptyMessage(UPLOAD_HISTORY);
                    }
                }
                // trigger another upload check unless configured for manual uploads
                if (uploadInterval > 0 && msg.arg1 == 0) {
                    this.sendEmptyMessageDelayed(UPLOAD_MESSAGES, uploadInterval);
                }
                break;
            case UPLOAD_HISTORY:
                if (mConfigManager.isDebug()) {
                    Log.d(TAG, "Performing history upload for " + mApiKey);
                }
                // if the uploads table is empty (no old uploads)
                //  and the messages table has messages that are not from the current session,
                //  or there is no current session
                //   then create a history upload and send it
                SQLiteDatabase db = mDB.getWritableDatabase();
                Cursor isempty = db.rawQuery("select * from " + UploadTable.TABLE_NAME, null);
                if ((isempty == null) || (isempty.getCount() == 0)) {

                    this.removeMessages(UPLOAD_HISTORY);
                    // execute all the upload steps
                    prepareUploads(true);
                    processUploads(true);
//            processCommands();
                } else {
                    // the previous upload is not done, try again in 30 seconds
                    this.sendEmptyMessageDelayed(UPLOAD_HISTORY, 30 * 1000);
                }
                break;
            case CLEANUP:
                // delete stale commands, uploads, messages, and sessions
                cleanupDatabase(Constants.DB_CLEANUP_EXPIRATION);
                this.sendEmptyMessageDelayed(CLEANUP, Constants.DB_CLEANUP_INTERVAL);
                break;
        }
    }

    private void fetchConfig() {
        try {
            if (!isNetworkAvailable()) {
                mConfigManager.restoreFromCache();
                return;
            }
            HttpClient httpClient = setupHttpClient();
            HttpGet httpGet = new HttpGet(makeServiceUri("config"));
            if (mConfigManager.isCompressionEnabled()) {
                httpGet.setHeader("Accept-Encoding", "gzip");
            }
            addMessageSignature(httpGet, null);

            HttpResponse httpResponse = httpClient.execute(httpGet, mHttpContext);
            int responseCode = -1;
            if (null != httpResponse.getStatusLine()) {
                responseCode = httpResponse.getStatusLine().getStatusCode();
            }
            if (responseCode >= 200 && responseCode < 300) {
                String response = extractResponseBody(httpResponse);
                JSONObject responseJSON = new JSONObject(response);
                mConfigManager.updateConfig(responseJSON);

            }
        } catch (java.net.SocketTimeoutException e) {
            // this is caught separately from IOException to prevent this common
            // exception from logging a stack trace
            Log.w(TAG, "Config request timed out");
        } catch (IOException e) {
            Log.w(TAG, "Config request failed with IO exception:", e);
        } catch (JSONException e) {
            Log.w(TAG, "Config request failed to process response message JSON");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error constructing config service URI", e);
        }
    }

    public static final String SQL_UPLOADABLE_MESSAGES = String.format(
            "%s=? and ((%s='NO-SESSION') or ((%s>=?) and (%s!=%d)))",
            MessageTable.API_KEY,
            MessageTable.SESSION_ID,
            MessageTable.STATUS,
            MessageTable.STATUS,
            Status.UPLOADED);

    public static final String SQL_HISTORY_MESSAGES = String.format(
            "%s=? and ((%s='NO-SESSION') or ((%s>=?) and (%s=%d) and (%s != ?)))",
            MessageTable.API_KEY,
            MessageTable.SESSION_ID,
            MessageTable.STATUS,
            MessageTable.STATUS,
            Status.UPLOADED,
            MessageTable.SESSION_ID);

    public static final String SQL_FINISHED_HISTORY_MESSAGES = String.format(
            "%s=? and ((%s='NO-SESSION') or ((%s>=?) and (%s=%d) and (%s=?)))",
            MessageTable.API_KEY,
            MessageTable.SESSION_ID,
            MessageTable.STATUS,
            MessageTable.STATUS,
            Status.UPLOADED,
            MessageTable.SESSION_ID);

    /* package-private */void prepareUploads(boolean history) {
        try {
            // select messages ready to upload
            SQLiteDatabase db = mDB.getWritableDatabase();

            String selection;
            String[] selectionArgs;
            if (history) {
                selection = SQL_HISTORY_MESSAGES;
                selectionArgs = new String[]{mApiKey, Integer.toString(Status.READY), MParticle.getInstance().mSessionID};
            } else {
                selection = SQL_UPLOADABLE_MESSAGES;
                selectionArgs = new String[]{mApiKey, Integer.toString(Status.READY)};
            }
            String[] selectionColumns = new String[]{"_id", MessageTable.MESSAGE, MessageTable.CREATED_AT, MessageTable.STATUS, MessageTable.SESSION_ID};

            Cursor readyMessagesCursor = db.query(
                    MessageTable.TABLE_NAME,
                    selectionColumns,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    MessageTable.CREATED_AT + ", " + MessageTable.SESSION_ID + " , _id asc");

            if (readyMessagesCursor.getCount() > 0) {
                fetchConfig();
                if (mConfigManager.isDebug()) {
                    Log.i(TAG, "Preparing " + readyMessagesCursor.getCount() + " events for upload");
                }
                if (history) {
                    String currentSessionId;
                    int sessionIndex = readyMessagesCursor.getColumnIndex(MessageTable.SESSION_ID);
                    JSONArray messagesArray = new JSONArray();
                    boolean sessionEndFound = false;
                    while (readyMessagesCursor.moveToNext()) {
                        currentSessionId = readyMessagesCursor.getString(sessionIndex);
                        JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(1));

                        if (msgObject.getString(MessageKey.TYPE).equals(MessageType.SESSION_END)) {
                            sessionEndFound = true;
                        }
                        messagesArray.put(msgObject);

                        if (readyMessagesCursor.isLast()) {
                            JSONObject uploadMessage = createUploadMessage(messagesArray, history);
                            // store in uploads table
                            if (sessionEndFound) {
                                dbInsertUpload(db, uploadMessage);
                                dbDeleteProcessedMessages(db, currentSessionId);
                            }
                        } else {
                            if (readyMessagesCursor.moveToNext() && !readyMessagesCursor.getString(sessionIndex).equals(currentSessionId)) {
                                JSONObject uploadMessage = createUploadMessage(messagesArray, history);
                                // store in uploads table
                                if (uploadMessage != null) {
                                    dbInsertUpload(db, uploadMessage);
                                    dbDeleteProcessedMessages(db, currentSessionId);
                                }
                                messagesArray = new JSONArray();
                            }
                            readyMessagesCursor.moveToPrevious();
                        }
                    }
                } else {
                    JSONArray messagesArray = new JSONArray();
                    int lastMessageId = 0;
                    while (readyMessagesCursor.moveToNext()) {
                        JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(1));
                        messagesArray.put(msgObject);
                        lastMessageId = readyMessagesCursor.getInt(0);
                    }
                    JSONObject uploadMessage = createUploadMessage(messagesArray, history);
                    // store in uploads table
                    dbInsertUpload(db, uploadMessage);
                    dbMarkAsUploadedMessage(db, lastMessageId);

                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error preparing batch upload in mParticle DB", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error with upload JSON object", e);
        } finally {
            mDB.close();
        }
    }

    private JSONObject createUploadMessage(JSONArray messagesArray, boolean history) throws JSONException {
        JSONObject uploadMessage = new JSONObject();

        uploadMessage.put(MessageKey.TYPE, MessageType.REQUEST_HEADER);
        uploadMessage.put(MessageKey.ID, UUID.randomUUID().toString());
        uploadMessage.put(MessageKey.TIMESTAMP, System.currentTimeMillis());
        uploadMessage.put(MessageKey.MPARTICLE_VERSION, Constants.MPARTICLE_VERSION);
        if (BuildConfig.ECHO){
            uploadMessage.put("echo", true);
        }
        uploadMessage.put(MessageKey.OPT_OUT_HEADER, mConfigManager.getOptedOut());

        mAppInfo.put(MessageKey.INSTALL_REFERRER, mPreferences.getString(PrefKeys.INSTALL_REFERRER, null));
        uploadMessage.put(MessageKey.APP_INFO, mAppInfo);
        // if there is notification key then include it
        String regId = PushRegistrationHelper.getRegistrationId(mContext);
        if ((regId != null) && (regId.length() > 0)) {
            mDeviceInfo.put(MessageKey.PUSH_TOKEN, regId);
        } else {
            mDeviceInfo.remove(MessageKey.PUSH_TOKEN);
        }

        uploadMessage.put(MessageKey.DEVICE_INFO, mDeviceInfo);
        uploadMessage.put(MessageKey.DEBUG, mConfigManager.getSandboxMode());

        String userAttrs = mPreferences.getString(PrefKeys.USER_ATTRS + mApiKey, null);
        if (null != userAttrs) {
            uploadMessage.put(MessageKey.USER_ATTRIBUTES, new JSONObject(userAttrs));
        }else{
            uploadMessage.put(MessageKey.USER_ATTRIBUTES, new JSONObject());
        }

        String userIds = mPreferences.getString(PrefKeys.USER_IDENTITIES + mApiKey, null);
        if (null != userIds) {
            uploadMessage.put(MessageKey.USER_IDENTITIES, new JSONArray(userIds));
        }else{
            uploadMessage.put(MessageKey.USER_IDENTITIES, new JSONArray());
        }

        if (history) {
            uploadMessage.put(MessageKey.HISTORY, messagesArray);
            uploadMessage.put(MessageKey.MESSAGES, new JSONArray());
        } else {
            uploadMessage.put(MessageKey.MESSAGES, messagesArray);
            uploadMessage.put(MessageKey.HISTORY, new JSONArray());
        }

        return uploadMessage;
    }

    private boolean processUploads(boolean history) {
        boolean processingSessionEnd = false;
        if (!isNetworkAvailable()) {
            return false;
        }

        try {
            // read batches ready to upload
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionArgs = new String[]{mApiKey};
            String[] selectionColumns = new String[]{"_id", UploadTable.MESSAGE};
            Cursor readyUploadsCursor = db.query(UploadTable.TABLE_NAME, selectionColumns,
                    UploadTable.API_KEY + "=?", selectionArgs, null, null, UploadTable.CREATED_AT);

            if (readyUploadsCursor.getCount() > 0) {

                HttpClient httpClient = setupHttpClient();

                while (readyUploadsCursor.moveToNext()) {
                    int id = readyUploadsCursor.getInt(0);
                    String message = readyUploadsCursor.getString(1);
                    if (!history) {
                        // if message is the MessageType.SESSION_END, then remember so the session history can be triggered
                        if (message.contains("\"" + MessageKey.TYPE + "\":\"" + MessageType.SESSION_END + "\"")) {
                            processingSessionEnd = true;
                        }
                    }
                    // POST message to mParticle service
                    HttpPost httpPost = new HttpPost(makeServiceUri("events"));
                    httpPost.setHeader("Content-type", "application/json");

                    ByteArrayEntity postEntity = null;
                    byte[] messageBytes = message.getBytes();
                    if (mConfigManager.isCompressionEnabled()) {
                        httpPost.setHeader("Accept-Encoding", "gzip");
                        try {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(messageBytes.length);
                            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
                            gzipOutputStream.write(messageBytes);
                            gzipOutputStream.finish();

                            //https://code.google.com/p/android/issues/detail?id=62589
                            // gzipOutputStream.flush();
                            postEntity = new ByteArrayEntity(byteArrayOutputStream.toByteArray());
                            httpPost.setHeader("Content-Encoding", "gzip");
                        } catch (IOException ioException) {
                            Log.w(TAG, "Failed to compress request. Sending uncompressed");
                        }
                    }
                    if (null == postEntity) {
                        postEntity = new ByteArrayEntity(messageBytes);
                    }
                    httpPost.setEntity(postEntity);

                    addMessageSignature(httpPost, message);

                    String response = null;
                    int responseCode = -1;
                    try {
                        if (mConfigManager.isDebug()) {
                            Log.d(TAG, "Uploading data to mParticle server:");
                            try {
                                JSONObject messageJson = new JSONObject(message);
                             //   Log.d(TAG, messageJson.toString(4));
                                if (messageJson.has(MessageKey.MESSAGES)) {
                                    JSONArray messages = messageJson.getJSONArray(MessageKey.MESSAGES);
                                    Log.d(TAG, "SENDING MESSAGES");
                                    for (int i = 0; i < messages.length(); i++) {
                                        Log.d(TAG, "Message type: " + ((JSONObject) messages.get(i)).getString(MessageKey.TYPE));
                                    }
                                } else if (messageJson.has(MessageKey.HISTORY)) {
                                    JSONArray messages = messageJson.getJSONArray(MessageKey.HISTORY);
                                    Log.d(TAG, "SENDING HISTORY");
                                    for (int i = 0; i < messages.length(); i++) {

                                        Log.d(TAG, "Message type: " + ((JSONObject) messages.get(i)).getString(MessageKey.TYPE) + " SID: " + ((JSONObject) messages.get(i)).optString(MessageKey.SESSION_ID));
                                        // Log.d(TAG, ((JSONObject)messages.get(i)).toString(4));
                                    }
                                }
                            } catch (JSONException jse) {

                            }
                        }
                        HttpResponse httpResponse = httpClient.execute(httpPost, mHttpContext);
                        if (null != httpResponse.getStatusLine()) {
                            responseCode = httpResponse.getStatusLine().getStatusCode();
                            response = extractResponseBody(httpResponse);
                        }
                    } catch (IOException e) {
                        // IOExceptions (such as timeouts) will be retried
                        Log.w(TAG, "Upload failed with IO exception: " + e.getClass().getName());
                    } finally {
                        if (202 == responseCode || (responseCode >= 400 && responseCode < 500)) {
                            dbDeleteUpload(db, id);
                        } else {
                            if (mConfigManager.isDebug()) {
                                Log.d(TAG, "Upload failed and will be retried.");
                            }
                        }
                    }

                    if (responseCode != 202) {
                        // if *any* upload fails, stop trying and wait for the next cycle
                        processingSessionEnd = false;
                        break;
                    } else {
                        try {
                            JSONObject responseJSON = new JSONObject(response);
                            if (responseJSON.has("echo")){
                                try{
                                    JSONObject messageObj = new JSONObject(message);
                                    boolean equal = MPUtility.jsonObjsAreEqual(responseJSON.getJSONObject("echo"), messageObj);
                                    if (!equal){
                                        Log.e(TAG, "Echo response did not match request!");
                                        Log.d(TAG, "Request: " + messageObj.toString(4));
                                        Log.d(TAG, "Echo response: " + responseJSON.getJSONObject("echo").toString(4));
                                    }
                                }catch (Exception e){
                                    Log.d(TAG, "Exception while comparing Echo response: " + e.getMessage());
                                }
                            }
                            if (responseJSON.has(MessageKey.MESSAGES)) {
                                JSONArray responseCommands = responseJSON.getJSONArray(MessageKey.MESSAGES);
                                for (int i = 0; i < responseCommands.length(); i++) {
                                    JSONObject commandObject = responseCommands.getJSONObject(i);
                                    dbInsertCommand(db, commandObject);
                                }
                            }
                        } catch (JSONException e) {
                            // ignore problems parsing response commands
                        }
                    }
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error processing batch uploads in mParticle DB", e);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error constructing upload service URI", e);
        } finally {
            mDB.close();
        }
        return processingSessionEnd;
    }

    // helper method to convert response body to a string whether or not it is
    // compressed
    private String extractResponseBody(HttpResponse httpResponse) throws IllegalStateException, IOException {
        InputStream inputStream = httpResponse.getEntity().getContent();
        Header contentEncoding = httpResponse.getFirstHeader("Content-Encoding");
        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        // From Stack Overflow:
        // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void addMessageSignature(HttpUriRequest request, String message) {
        try {
            String method = request.getMethod();
            String dateHeader = DateUtils.formatDate(new Date());
            if (dateHeader.length() > DateUtils.PATTERN_RFC1123.length()) {
                // handle a problem on some devices where TZ offset is appended
                dateHeader = dateHeader.substring(0, DateUtils.PATTERN_RFC1123.length());
            }
            String path = request.getURI().getPath();
            String signatureMessage = method + "\n" + dateHeader + "\n" + path;
            if ("POST".equalsIgnoreCase(method) && null != message) {
                signatureMessage += message;
            }
            request.setHeader("Date", dateHeader);
            request.setHeader(HEADER_SIGNATURE, hmacSha256Encode(mSecret, signatureMessage));
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Error signing message", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error signing message", e);
        }
    }

    private void processCommands() {
        if (!isNetworkAvailable()) {
            return;
        }
        try {
            // read batches ready to upload
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionColumns = new String[]{"_id", CommandTable.URL, CommandTable.METHOD,
                    CommandTable.POST_DATA, CommandTable.HEADERS};
            Cursor commandsCursor = db.query(CommandTable.TABLE_NAME, selectionColumns,
                    null, null, null, null, "_id");
            while (commandsCursor.moveToNext()) {
                int id = commandsCursor.getInt(0);
                String commandUrl = commandsCursor.getString(1);
                String method = commandsCursor.getString(2);
                String postData = commandsCursor.getString(3);
                String headers = commandsCursor.getString(4);

                int responseCode = -1;
                try {
                    if (mConfigManager.isDebug()) {
                        Log.d(TAG, "Sending data to: " + commandUrl);
                    }
                    URL url = new URL(commandUrl);
                    HttpURLConnection urlConnection;
                    if (mProxy == null) {
                        urlConnection = (HttpURLConnection) url.openConnection();
                    } else {
                        urlConnection = (HttpURLConnection) url.openConnection(mProxy);
                    }
                    try {
                        JSONObject headersJSON = new JSONObject(headers);
                        for (Iterator<?> iter = headersJSON.keys(); iter.hasNext(); ) {
                            String headerName = (String) iter.next();
                            String headerValue = headersJSON.getString(headerName);
                            urlConnection.setRequestProperty(headerName, headerValue);
                        }
                        if ("POST".equalsIgnoreCase(method)) {
                            urlConnection.setDoOutput(true);
                            byte[] postDataBytes = Base64.decode(postData.getBytes());
                            urlConnection.setFixedLengthStreamingMode(postDataBytes.length);
                            urlConnection.getOutputStream().write(postDataBytes);
                        }
                        responseCode = urlConnection.getResponseCode();
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (Throwable t) {
                    // fail silently. a message will be logged if debug mode is enabled
                } finally {
                    if (responseCode > -1) {
                        dbDeleteCommand(db, id);
                    } else if (mConfigManager.isDebug()) {
                        Log.w(TAG, "Partner processing failed and will be retried.");
                    }
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error processing partner uploads in mParticle DB", e);
        } finally {
            mDB.close();
        }
    }

    private void cleanupDatabase(int expirationPeriod) {
        SQLiteDatabase db = mDB.getWritableDatabase();
        String[] whereArgs = {Long.toString(System.currentTimeMillis() - expirationPeriod)};
        db.delete(CommandTable.TABLE_NAME, CommandTable.CREATED_AT + "<?", whereArgs);
        db.delete(UploadTable.TABLE_NAME, UploadTable.CREATED_AT + "<?", whereArgs);
        db.delete(MessageTable.TABLE_NAME, MessageTable.CREATED_AT + "<?", whereArgs);
        db.delete(SessionTable.TABLE_NAME, SessionTable.END_TIME + "<?", whereArgs);
    }

    private URI makeServiceUri(String method) throws URISyntaxException {
        return new URI("https", DEBUG_SERVICE_HOST, "/" + SERVICE_VERSION + "/" + mApiKey + "/" + method, null);
    }

    private void dbInsertUpload(SQLiteDatabase db, JSONObject message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTable.API_KEY, mApiKey);
        contentValues.put(UploadTable.CREATED_AT, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(UploadTable.MESSAGE, message.toString());
        db.insert(UploadTable.TABLE_NAME, null, contentValues);
    }

    private void dbDeleteProcessedMessages(SQLiteDatabase db, String sessionId) {
        String[] whereArgs = new String[]{mApiKey, Integer.toString(Status.UPLOADED), sessionId};
        int rowsdeleted = db.delete(MessageTable.TABLE_NAME, SQL_FINISHED_HISTORY_MESSAGES, whereArgs);
        Log.d("mParticle DB", "Deleted " + rowsdeleted);
    }

    private void dbMarkAsUploadedMessage(SQLiteDatabase db, int lastMessageId) {
        //non-session messages can be deleted, they're not part of session history
        String[] whereArgs = new String[]{mApiKey, Long.toString(lastMessageId)};
        String whereClause = SQL_DELETABLE_MESSAGES + " and (_id<=?)";
        int rowsdeleted = db.delete(MessageTable.TABLE_NAME, whereClause, whereArgs);

        whereArgs = new String[]{mApiKey, Integer.toString(Status.READY), Long.toString(lastMessageId)};
        whereClause = SQL_UPLOADABLE_MESSAGES + " and (_id<=?)";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTable.STATUS, Status.UPLOADED);
        int rowsupdated = db.update(MessageTable.TABLE_NAME, contentValues, whereClause, whereArgs);
    }

    private void dbDeleteUpload(SQLiteDatabase db, int id) {
        String[] whereArgs = {Long.toString(id)};
        int rowsdeleted = db.delete(UploadTable.TABLE_NAME, "_id=?", whereArgs);
        Log.d("mParticle DB", "Deleted " + rowsdeleted);
    }

    private void dbDeleteCommand(SQLiteDatabase db, int id) {
        String[] whereArgs = {Long.toString(id)};
        db.delete(CommandTable.TABLE_NAME, "_id=?", whereArgs);
    }

    private void dbInsertCommand(SQLiteDatabase db, JSONObject command) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CommandTable.URL, command.getString(MessageKey.URL));
        contentValues.put(CommandTable.METHOD, command.getString(MessageKey.METHOD));
        contentValues.put(CommandTable.POST_DATA, command.optString(MessageKey.POST));
        contentValues.put(CommandTable.HEADERS, command.optString(MessageKey.HEADERS));
        contentValues.put(CommandTable.CREATED_AT, System.currentTimeMillis());
        db.insert(CommandTable.TABLE_NAME, null, contentValues);
    }

    private boolean isNetworkAvailable() {
        if (!mAccessNetworkStateAvailable) {
            return true;
        }

        NetworkInfo networkInfo = mConnectivyManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnectedOrConnecting();
        }
        return false;
    }

    private HttpClient setupHttpClient() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);

        HttpConnectionParams.setConnectionTimeout(params, 10 * 1000);
        HttpConnectionParams.setSoTimeout(params, 10 * 1000);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        if (null != mProxyHost) {
            ConnRouteParams.setDefaultProxy(params, mProxyHost);
        }
        HttpClient client = new DefaultHttpClient(params);
        return client;
    }

    public void setConnectionProxy(String host, int port) {
        // HttpClient and UrlConnection use separate proxies
        mProxyHost = new HttpHost(host, port);
        mProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    // From Stack Overflow:
    // http://stackoverflow.com/questions/7124735/hmac-sha256-algorithm-for-signature-calculation
    private static String hmacSha256Encode(String key, String data) throws NoSuchAlgorithmException,
            InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return asHex(sha256_HMAC.doFinal(data.getBytes()));
    }

    // From Stack Overflow:
    // http://stackoverflow.com/questions/923863/converting-a-string-to-hexadecimal-in-java
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static String asHex(byte[] buf) {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

}
