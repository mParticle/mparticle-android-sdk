package com.mparticle;

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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.mparticle.MessageDatabase.CommandTable;
import com.mparticle.MessageDatabase.MessageTable;
import com.mparticle.MessageDatabase.UploadTable;

/* package-private */ final class UploadHandler extends Handler {

    private static final String TAG = Constants.LOG_TAG;

    private MessageDatabase mDB;
    private SharedPreferences mPreferences;
    private String mApiKey;
    private String mSecret;
    private long mUploadInterval;
    private boolean mDebugMode = false;

    private HttpContext mHttpContext;
    private HttpHost mProxyHost;
    private String mServiceScheme = SERVICE_SCHEME;
    private JSONObject mAppInfo;
    private JSONObject mDeviceInfo;
    private Proxy mProxy;
    private ConnectivityManager mConnectivyManager;

    public static final int UPLOAD_MESSAGES = 1;
    public static final int CLEANUP = 2;

    public static final String HEADER_APPKEY = "x-mp-appkey";
    public static final String HEADER_SIGNATURE = "x-mp-signature";

    public static final String SERVICE_SCHEME = "http";
    public static final String SERVICE_HOST = "api.dev.mparticle.com";
    public static final String SERVICE_VERSION = "v1";

    private static String mUploadMode = "batch";

    public UploadHandler(Context appContext, Looper looper, String apiKey, String secret, long uploadInterval) {
        super(looper);
        mApiKey = apiKey;
        mSecret = secret;
        mUploadInterval = uploadInterval;

        mDB = new MessageDatabase(appContext);
        mPreferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mHttpContext  = new BasicHttpContext();
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, new PersistentCookieStore(appContext));

        mConnectivyManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mAppInfo = DeviceAttributes.collectAppInfo(appContext);
        mDeviceInfo = DeviceAttributes.collectDeviceInfo(appContext);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
        case UPLOAD_MESSAGES:
            if (mDebugMode) {
                Log.d(TAG, "Performing " + (msg.arg1==0?"periodic":"manual") +" upload for " + mApiKey);
            }
            // execute all the upload steps
            if (mUploadInterval>0 || msg.arg1==1) {
                fetchConfig();
                prepareUploads();
                processUploads();
                processCommands();
            }
            // trigger another upload check unless configured for manual uploads
            if (mUploadInterval > 0 && msg.arg1==0) {
                this.sendEmptyMessageDelayed(UPLOAD_MESSAGES, mUploadInterval);
            }
        case CLEANUP:
            // delete completed uploads, messages, and sessions
            // TODO: cleanupDatabase();
            break;
        }
    }

    void fetchConfig() {
        if (!isNetworkAvailable()) {
            return;
        }
        try {
            HttpClient httpClient = setupHttpClient();
            HttpGet httpGet = new HttpGet(makeServiceUri("config"));
            httpGet.setHeader("Accept-Encoding", "gzip");
            addMessageSignature(httpGet, null);

            HttpResponse httpResponse = httpClient.execute(httpGet, mHttpContext);
            int responseCode = -1;
            if (null!=httpResponse.getStatusLine()) {
                responseCode = httpResponse.getStatusLine().getStatusCode();
            }
            if (responseCode >=200 && responseCode<300) {
                String response = extractResponseBody(httpResponse);
                JSONObject responseJSON = new JSONObject(response);
                if (responseJSON.has(MessageKey.SESSION_UPLOAD)) {
                    String sessionUploadMode = responseJSON.getString(MessageKey.SESSION_UPLOAD);
                    mUploadMode = ("batch".equalsIgnoreCase(sessionUploadMode)) ? "batch" : "stream";
                }
            }
        } catch (java.net.SocketTimeoutException e) {
            // this is caught separately from IOException to prevent this common exception from logging a stack trace
            Log.w(TAG, "Config request timed out");
        } catch (IOException e) {
            Log.w(TAG, "Config request failed with IO exception:", e);
        } catch (JSONException e) {
            Log.w(TAG, "Config request failed to process response message JSON");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error constructing config service URI", e);
        }
    }

    void prepareUploads() {
        try {
            // select messages ready to upload
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionArgs = new String[]{mApiKey, Integer.toString(Status.BATCH_READY)};
            String selection = MessageTable.API_KEY + "=? and (" + MessageTable.SESSION_ID + "='NO-SESSION' or ";
            if (("batch").equals(mUploadMode)) {
                selection += MessageTable.STATUS + "=?)";
            } else {
                selection += MessageTable.STATUS + "<=?)";
            }
            String[] selectionColumns = new String[]{"_id", MessageTable.MESSAGE, MessageTable.CREATED_AT};
            Cursor readyMessagesCursor = db.query(MessageTable.TABLE_NAME, selectionColumns, selection, selectionArgs, null, null, MessageTable.CREATED_AT+" , _id");
            if (readyMessagesCursor.getCount()>0) {
                if (mDebugMode) {
                    Log.i(TAG, "Processing " + readyMessagesCursor.getCount() + " events for upload");
                }
                JSONArray messagesArray = new JSONArray();
                int lastReadyMessage = 0;
                while (readyMessagesCursor.moveToNext()) {
                    lastReadyMessage = readyMessagesCursor.getInt(0);
                    JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(1));
                    messagesArray.put(msgObject);
                }
                // create upload message
                JSONObject uploadMessage = createUploadMessage(messagesArray);
                // store in uploads table
                dbInsertUpload(db, uploadMessage);
                // delete processed messages
                dbDeleteProcessedMessages(db, lastReadyMessage);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error preparing batch upload in mParticle DB", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error with upload JSON object", e);
        } finally {
            mDB.close();
        }
    }

    private JSONObject createUploadMessage(JSONArray messagesArray) throws JSONException {
        JSONObject uploadMessage= new JSONObject();
        uploadMessage.put(MessageKey.TYPE, MessageType.REQUEST_HEADER);
        uploadMessage.put(MessageKey.ID, UUID.randomUUID().toString());
        uploadMessage.put(MessageKey.TIMESTAMP, System.currentTimeMillis());
        uploadMessage.put(MessageKey.APPLICATION_KEY, mApiKey);
        uploadMessage.put(MessageKey.MPARTICLE_VERSION, Constants.MPARTICLE_VERSION);
        uploadMessage.put(MessageKey.APP_INFO, mAppInfo);
        uploadMessage.put(MessageKey.DEVICE_INFO, mDeviceInfo);

        String userAttrs = mPreferences.getString(PrefKeys.USER_ATTRS + mApiKey, null);
        if (null!=userAttrs) {
            uploadMessage.put(MessageKey.USER_ATTRIBUTES, new JSONObject(userAttrs));
        }

        uploadMessage.put(MessageKey.MESSAGES, messagesArray);

        return uploadMessage;
    }

    void processUploads() {
        if (!isNetworkAvailable()) {
            return;
        }
        try {
            HttpClient httpClient = setupHttpClient();

            // read batches ready to upload
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionColumns = new String[]{ "_id", UploadTable.MESSAGE };
            Cursor readyUploadsCursor = db.query(UploadTable.TABLE_NAME, selectionColumns,
                    null, null, null, null, UploadTable.CREATED_AT + " , _id");
            while (readyUploadsCursor.moveToNext()) {
                int id = readyUploadsCursor.getInt(0);
                String message = readyUploadsCursor.getString(1);
                // POST message to mParticle service
                HttpPost httpPost = new HttpPost(makeServiceUri("events"));
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setHeader("Accept-Encoding", "gzip");

                try {
                    byte[] messageBytes = message.getBytes();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(messageBytes.length);
                    GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
                    gzipOutputStream.write(messageBytes);
                    gzipOutputStream.finish();
                    gzipOutputStream.flush();
                    httpPost.setEntity(new ByteArrayEntity(byteArrayOutputStream.toByteArray()));
                    httpPost.setHeader("Content-Encoding", "gzip");
                } catch (IOException ioException) {
                    Log.w(TAG, "Failed to compress request. Sending uncompressed");
                    httpPost.setEntity(new ByteArrayEntity(message.getBytes()));
                }

                addMessageSignature(httpPost, message);

                String response = null;
                int responseCode = -1;
                try {
                    if (mDebugMode) {
                        Log.d(TAG, "Uploading data to mParticle server:");
                        Log.d(TAG, message);
                    }
                    HttpResponse httpResponse = httpClient.execute(httpPost, mHttpContext);
                    if (null!=httpResponse.getStatusLine()) {
                        responseCode = httpResponse.getStatusLine().getStatusCode();
                        response = extractResponseBody(httpResponse);
                    }
                } catch (IOException e) {
                    // IOExceptions (such as timeouts) will be retried
                    Log.w(TAG, "Upload failed with IO exception: " + e.getClass().getName());
                } finally {
                    if (202==responseCode || (responseCode>=400 && responseCode<500)) {
                        dbDeleteUpload(db, id);
                    } else {
                        dbUpdateUploadStatus(db, id, Status.READY);
                        if (mDebugMode) {
                            Log.d(TAG, "Upload failed and will be retried.");
                        }
                    }
                }

                if (responseCode != 202) {
                    // if *any* upload fails, stop trying and wait for the next cycle
                    break;
                } else {
                    try {
                        JSONObject responseJSON = new JSONObject(response);
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
        } catch (SQLiteException e) {
            Log.e(TAG, "Error processing batch uploads in mParticle DB", e);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error constructing upload service URI", e);
        } finally {
            mDB.close();
        }
    }

    // helper method to convert response body to a string whether or not it is compressed
    private String extractResponseBody(HttpResponse httpResponse) throws IllegalStateException, IOException {
        InputStream inputStream = httpResponse.getEntity().getContent();
        Header contentEncoding = httpResponse.getFirstHeader("Content-Encoding");
        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        // From Stack Overflow: http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void addMessageSignature(HttpUriRequest request, String message) {
        try {
            String method = request.getMethod();
            String dateHeader = DateUtils.formatDate(new Date());
            if (dateHeader.length()>DateUtils.PATTERN_RFC1123.length()) {
                // handle a problem on some devices where TZ offset is appended
                dateHeader = dateHeader.substring(0, DateUtils.PATTERN_RFC1123.length());
            }
            String path = request.getURI().getPath();
            String signatureMessage = method + "\n" + dateHeader + "\n" + path;
            if ("POST".equalsIgnoreCase(method) && null!=message) {
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

    void processCommands() {
        if (!isNetworkAvailable()) {
            return;
        }
        try {
            // read batches ready to upload
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionColumns = new String[] { "_id", CommandTable.URL, CommandTable.METHOD,
                    CommandTable.POST_DATA, CommandTable.HEADERS };
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
                    if (mDebugMode) {
                        Log.d(TAG, "Sending data to: " + commandUrl);
                    }
                    URL url = new URL(commandUrl);
                    HttpURLConnection urlConnection;
                    if (mProxy==null) {
                        urlConnection = (HttpURLConnection) url.openConnection();
                    } else {
                        urlConnection = (HttpURLConnection) url.openConnection(mProxy);
                    }
                    try {
                        JSONObject headersJSON = new JSONObject(headers);
                        for (Iterator<?> iter = headersJSON.keys(); iter.hasNext();) {
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
                    if (responseCode>-1) {
                        dbDeleteCommand(db, id);
                    } else if (mDebugMode) {
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

    private URI makeServiceUri(String method) throws URISyntaxException {
        return new URI(mServiceScheme, SERVICE_HOST, "/"+SERVICE_VERSION+"/"+mApiKey+"/"+method, null);
    }

    private void dbInsertUpload(SQLiteDatabase db, JSONObject message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTable.API_KEY, mApiKey);
        contentValues.put(UploadTable.UPLOAD_ID, message.getString(MessageKey.ID));
        contentValues.put(UploadTable.CREATED_AT, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(UploadTable.MESSAGE, message.toString());
        contentValues.put(UploadTable.STATUS, Status.READY);
        db.insert(UploadTable.TABLE_NAME, null, contentValues);
    }

    private void dbDeleteProcessedMessages(SQLiteDatabase db, int lastReadyMessage) {
        String[] whereArgs = { mApiKey, Long.toString(lastReadyMessage) };
        db.delete(MessageTable.TABLE_NAME, MessageTable.API_KEY + "=? and (_id<=? or " + MessageTable.SESSION_ID + "='NO-SESSION')", whereArgs);
    }

    private void dbUpdateUploadStatus(SQLiteDatabase db, int id, int status) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTable.STATUS, status);
        String[] whereArgs = { Long.toString(id) };
        db.update(UploadTable.TABLE_NAME, contentValues, "_id=?", whereArgs);
    }

    private void dbDeleteUpload(SQLiteDatabase db, int id) {
        String[] whereArgs = { Long.toString(id) };
        db.delete(UploadTable.TABLE_NAME, "_id=?", whereArgs);
    }

    private void dbDeleteCommand(SQLiteDatabase db, int id) {
        String[] whereArgs = { Long.toString(id) };
        db.delete(CommandTable.TABLE_NAME, "_id=?", whereArgs);
    }

    private void dbInsertCommand(SQLiteDatabase db, JSONObject command) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CommandTable.COMMAND_ID, command.getString(MessageKey.ID));
        contentValues.put(CommandTable.URL, command.getString(MessageKey.URL));
        contentValues.put(CommandTable.METHOD, command.getString(MessageKey.METHOD));
        // TODO: decide between null and empty string
        contentValues.put(CommandTable.POST_DATA, command.optString(MessageKey.POST));
        contentValues.put(CommandTable.CLEAR_HEADERS, command.optBoolean(MessageKey.CLEAR_HEADERS,false));
        contentValues.put(CommandTable.HEADERS, command.optString(MessageKey.HEADERS));
        contentValues.put(CommandTable.STATUS, Status.READY);
        contentValues.put(CommandTable.CREATED_AT, System.currentTimeMillis());
        db.insert(CommandTable.TABLE_NAME, null, contentValues);
    }

    private boolean isNetworkAvailable() {
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

        HttpConnectionParams.setConnectionTimeout(params, 10*1000);
        HttpConnectionParams.setSoTimeout(params, 10*1000);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        if (null!=mProxyHost) {
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

    public void setConnectionScheme(String scheme) {
        mServiceScheme = scheme;
    }

    // From Stack Overflow: http://stackoverflow.com/questions/7124735/hmac-sha256-algorithm-for-signature-calculation
     private static String hmacSha256Encode(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return asHex(sha256_HMAC.doFinal(data.getBytes()));
     }

     // From Stack Overflow: http://stackoverflow.com/questions/923863/converting-a-string-to-hexadecimal-in-java
     private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
     private static String asHex(byte[] buf) {
         char[] chars = new char[2 * buf.length];
         for (int i = 0; i < buf.length; ++i) {
             chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
             chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
         }
         return new String(chars);
     }

    public void setDebugMode(boolean debugMode) {
        mDebugMode = debugMode;
    }

}
