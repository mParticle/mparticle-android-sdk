package com.mparticle;

import java.io.IOException;
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
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.protocol.BasicHttpContext;
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
import android.net.http.AndroidHttpClient;
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
    private Context mContext;
    private String mApiKey;
    private String mSecret;
    private long mUploadInterval;

    private AndroidHttpClient mHttpClient;
    private HttpContext mHttpContext;
    private CookieStore mCookieStore;
    private JSONObject mAppInfo;
    private JSONObject mDeviceInfo;
    private Proxy mProxy;

    public static final int PREPARE_UPLOADS = 0;
    public static final int PROCESS_UPLOADS = 1;
    public static final int PROCESS_COMMANDS = 2;
    public static final int CLEANUP = 3;
    public static final int FETCH_CONFIG = 4;
    public static final int PERIODIC_UPLOAD = 5;

    public static final String HEADER_APPKEY = "x-mp-appkey";
    public static final String HEADER_SIGNATURE = "x-mp-signature";

    public static String SERVICE_SCHEME = "http";
    public static String SERVICE_HOST = "api.dev.mparticle.com";
    public static String SERVICE_VERSION = "v1";

    private static String mUploadMode = "batch";

    public UploadHandler(Context context, Looper looper, String apiKey, String secret, long uploadInterval) {
        super(looper);
        mContext = context;
        mApiKey = apiKey;
        mSecret = secret;
        mUploadInterval = uploadInterval;

        mDB = new MessageDatabase(mContext);
        mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mHttpClient = AndroidHttpClient.newInstance("mParticleSDK", mContext);
        mHttpContext  = new BasicHttpContext();
        mCookieStore = new PersistentCookieStore(context);
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);

        mAppInfo = MParticleAPI.collectAppInfo(context);
        mDeviceInfo = MParticleAPI.collectDeviceInfo(context);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
        case PREPARE_UPLOADS:
            // create uploads records for batches of messages and mark the messages as processed
            prepareUploads();
            // TODO: restore this break - for development only
            // break;
        case PROCESS_UPLOADS:
            // post upload messages to server and mark the uploads as processed. store response commands to be processed.
            processUploads();
            // TODO: restore this break - for development only
            // break;
        case PROCESS_COMMANDS:
            // post commands to vendor services
            processCommands();
            break;
        case CLEANUP:
            // delete completed uploads, messages, and sessions
            // TODO: cleanupDatabase();
            break;
        case FETCH_CONFIG:
            // get the application configuration and process it
            fetchConfig();
            break;
        case PERIODIC_UPLOAD:
            // do all the upload steps and trigger another upload check unless configured for manual uploads
            Log.d(TAG, "Doing periodic upload check");
            if (mUploadInterval>0) {
                fetchConfig();
                prepareUploads();
                processUploads();
                processCommands();
                this.sendEmptyMessageDelayed(PERIODIC_UPLOAD, mUploadInterval);
            }
        }
    }

    void prepareUploads() {
        try {
            // select messages ready to upload (limited number)
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionArgs = new String[]{Integer.toString(Status.BATCH_READY)};
            String selection;
            if (("batch").equals(mUploadMode)) {
                selection = MessageTable.STATUS + "=?";
            } else {
                selection = MessageTable.STATUS + "<=?";
            }
            String[] selectionColumns = new String[]{MessageTable.UUID, MessageTable.MESSAGE, MessageTable.STATUS, MessageTable.MESSAGE_TIME, "_id"};
            Cursor readyMessagesCursor = db.query(MessageTable.TABLE_NAME, selectionColumns, selection, selectionArgs, null, null, MessageTable.MESSAGE_TIME+" , _id");
            if (readyMessagesCursor.getCount()>0) {
                JSONArray messagesArray = new JSONArray();
                int lastReadyMessage = 0;
                while (readyMessagesCursor.moveToNext()) {
                    // NOTE: this could be simpler if we ignore PENDING status on start-session message
                    if (!readyMessagesCursor.isLast() || Status.PENDING!=readyMessagesCursor.getInt(2)) {
                        JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(1));
                        messagesArray.put(msgObject);
                        lastReadyMessage = readyMessagesCursor.getInt(4);
                    }
                }
                if (messagesArray.length() > 0) {
                    // create upload message
                    JSONObject uploadMessage = createUploadMessage(messagesArray);
                    // store in uploads table
                    dbInsertUpload(db, uploadMessage);
                    // delete processed messages
                    dbDeleteProcessedMessages(db, lastReadyMessage);
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

    private JSONObject createUploadMessage(JSONArray messagesArray) throws JSONException {
        JSONObject uploadMessage= new JSONObject();
        uploadMessage.put(MessageKey.TYPE, MessageType.REQUEST_HEADER);
        uploadMessage.put(MessageKey.ID, UUID.randomUUID().toString());
        uploadMessage.put(MessageKey.TIMESTAMP, System.currentTimeMillis());

        uploadMessage.put(MessageKey.APPLICATION_KEY, mApiKey);
        uploadMessage.put(MessageKey.MPARTICLE_VERSION, Constants.MPARTICLE_VERSION);

        uploadMessage.put(MessageKey.APP_INFO, mAppInfo);
        uploadMessage.put(MessageKey.DEVICE_INFO, mDeviceInfo);

        String userAttrs = mPreferences.getString(PrefKeys.USER_ATTRS+mApiKey, null);
        if(null!=userAttrs) {
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
            // read batches ready to upload
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionArgs = new String[]{Integer.toString(Status.PROCESSED)};
            String[] selectionColumns = new String[]{ "_id", UploadTable.MESSAGE };
            Cursor readyUploadsCursor = db.query(UploadTable.TABLE_NAME, selectionColumns,
                    UploadTable.STATUS + "!=?", selectionArgs, null, null, UploadTable.MESSAGE_TIME + " , _id");
            while (readyUploadsCursor.moveToNext()) {
                int id = readyUploadsCursor.getInt(0);
                String message = readyUploadsCursor.getString(1);
                // POST message to mParticle service
                HttpPost httpPost = new HttpPost(makeServiceUri("events"));
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setEntity(new ByteArrayEntity(message.getBytes()));

                addMessageSignature(httpPost, message);

                String response = null;
                int responseCode = -1;
                try {
                    Log.d(TAG, "Sending message to mParticle server:");
                    Log.d(TAG, message);
                    response = mHttpClient.execute(httpPost, new BasicResponseHandler(), mHttpContext);
                    responseCode = 202;
                    Log.d(TAG, "Message upload successuful");
                } catch (HttpResponseException httpResponseException) {
                    responseCode = httpResponseException.getStatusCode();
                    Log.w(TAG, "Message upload failed with response code:" + responseCode);
                } catch (IOException e) {
                    // IOExceptions (such as timeouts) will be retried
                    Log.w(TAG, "Message upload failed with IO exception");
                } catch (Throwable t) {
                    // Some other exception occurred.
                    Log.e(TAG, "Message upload failed with IO exception",t);
                } finally {
                    if (202==responseCode || (responseCode>=400 && responseCode<500)) {
                        dbDeleteUpload(db, id);
                    } else {
                        dbUpdateUploadStatus(db, id, Status.READY);
                    }
                }

                if (responseCode != 202) {
                    // if any upload fails, stop trying and wait for the next cycle
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

    private void addMessageSignature(HttpUriRequest request, String message) {
        try {
            String method = request.getMethod();
            String dateHeader = DateUtils.formatDate(new Date());
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
            String[] selectionArgs = new String[] { Integer.toString(Status.PROCESSED) };
            String[] selectionColumns = new String[] { "_id", CommandTable.URL, CommandTable.METHOD,
                    CommandTable.POST_DATA, CommandTable.HEADERS };
            Cursor readyComandsCursor = db.query(CommandTable.TABLE_NAME, selectionColumns,
                    CommandTable.STATUS + "!=?", selectionArgs, null, null, "_id");
            while (readyComandsCursor.moveToNext()) {
                int id = readyComandsCursor.getInt(0);
                String commandUrl = readyComandsCursor.getString(1);
                String method = readyComandsCursor.getString(2);
                String postData = readyComandsCursor.getString(3);
                String headers = readyComandsCursor.getString(4);

                int responseCode = -1;
                try {
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
                        Log.d(TAG, "Got opening connection for : "+ commandUrl);
                        responseCode = urlConnection.getResponseCode();
                        Log.d(TAG, "Got response: "+ responseCode);
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Command processing failed:" + t.getMessage());
                } finally {
                    if (responseCode>-1) {
                        dbDeleteCommand(db, id);
                    }
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error processing command uploads in mParticle DB", e);
        } finally {
            mDB.close();
        }
    }

    void fetchConfig() {
        if (!isNetworkAvailable()) {
            return;
        }
        try {
            HttpGet httpGet = new HttpGet(makeServiceUri("config"));
            addMessageSignature(httpGet, null);
            String response = mHttpClient.execute(httpGet, new BasicResponseHandler(), mHttpContext);

            JSONObject responseJSON = new JSONObject(response);
            if (responseJSON.has(MessageKey.SESSION_UPLOAD)) {
                String sessionUploadMode = responseJSON.getString(MessageKey.SESSION_UPLOAD);
                if ("batch".equalsIgnoreCase(sessionUploadMode)) {
                    mUploadMode = "batch";
                } else {
                    mUploadMode = "stream";
                }
            }
        } catch (HttpResponseException e) {
            Log.w(TAG, "Config request failed:" + e.getMessage());
        } catch (IOException e) {
            Log.w(TAG, "Config request failed w/ IO:" + e.getMessage());
        } catch (JSONException e) {
            Log.w(TAG, "Config request failed to process response message JSON");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error constructing config service URI", e);
        }
    }

    private URI makeServiceUri(String method) throws URISyntaxException {
        return new URI(SERVICE_SCHEME, SERVICE_HOST, "/"+SERVICE_VERSION+"/"+mApiKey+"/"+method, null);
    }

    private void dbInsertUpload(SQLiteDatabase db, JSONObject message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTable.UPLOAD_ID, message.getString(MessageKey.ID));
        contentValues.put(UploadTable.MESSAGE_TIME, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(UploadTable.MESSAGE, message.toString());
        contentValues.put(UploadTable.STATUS, Status.PENDING);
        db.insert(UploadTable.TABLE_NAME, null, contentValues);
    }

    private void dbDeleteProcessedMessages(SQLiteDatabase db, int lastReadyMessage) {
        String[] whereArgs = { Long.toString(lastReadyMessage) };
        db.delete(MessageTable.TABLE_NAME, "_id<=?", whereArgs);
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
        contentValues.put(CommandTable.STATUS, Status.PENDING);
        db.insert(CommandTable.TABLE_NAME, null, contentValues);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivyManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivyManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnectedOrConnecting();
        }
        return false;
    }

    /* Possibly for development only */
    public void setConnectionProxy(String host, int port) {
        HttpHost proxy = new HttpHost(host, port);
        mHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        mProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
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
         for (int i = 0; i < buf.length; ++i)
         {
             chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
             chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
         }
         return new String(chars);
     }

}
