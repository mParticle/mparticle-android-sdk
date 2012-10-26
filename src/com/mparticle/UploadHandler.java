package com.mparticle;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.UploadStatus;
import com.mparticle.MessageDatabase.MessageTable;
import com.mparticle.MessageDatabase.UploadTable;

/* package-private */ final class UploadHandler extends Handler {

    private static final String TAG = "mParticleAPI";

    private MessageDatabase mDB;
    private Context mContext;
    private AndroidHttpClient mHttpClient;
    private HttpContext mHttpContext;
    private BasicCookieStore mCookieStore;

    public static final int PREPARE_BATCHES = 0;
    public static final int PROCESS_BATCHES = 1;
    public static final int PROCESS_RESULTS = 2;
    public static final int CLEANUP = 3;

    public static final String HEADER_APPKEY = "x-mp-appkey";
    public static final String HEADER_SIGNATURE = "x-mp-signature";

    public static int BATCH_SIZE = 10;
    public static String SERVICE_ENDPOINT = "http://api.dev.mparticle.com/v1/events";

    public UploadHandler(Context context, Looper looper) {
        super(looper);
        mContext = context;
        mDB = new MessageDatabase(mContext);
        mHttpClient = AndroidHttpClient.newInstance("mParticleSDK", mContext);
        mHttpContext  = new BasicHttpContext();
        mCookieStore = new BasicCookieStore();
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);

        // TODO: temporary - for development only
        HttpHost proxy = new HttpHost("192.168.1.100", 8080);
        mHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
        case PREPARE_BATCHES:
            // create uploads records for batches of messages and mark the messages as processed
            prepareBatches();
            // TODO: restore this break - for development only
            // break;
        case PROCESS_BATCHES:
            // post upload messages to server and mark the uploads as processed. store response commands to be processed.
            processBatches();
            break;
        case PROCESS_RESULTS:
            // read responses with upload messages
            // post response to server
            // handle cookies
            // update response process status
            // TODO: processResults();
            break;
        case CLEANUP:
            // delete completed uploads, messages, and sessions
            // TODO: cleanupDatabase();
            break;
        }
    }

    void prepareBatches() {
        try {
            // select messages ready to upload (limited number)
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionArgs = new String[]{Integer.toString(UploadStatus.PROCESSED)};
            String[] selectionColumns = new String[]{MessageTable.UUID, MessageTable.MESSAGE, MessageTable.UPLOAD_STATUS, MessageTable.MESSAGE_TIME, "_id"};
            Cursor readyMessagesCursor = db.query("messages", selectionColumns, MessageTable.UPLOAD_STATUS+"!=?", selectionArgs, null, null, MessageTable.MESSAGE_TIME+" , _id");
            if (readyMessagesCursor.getCount()>0) {
                JSONArray messagesArray = new JSONArray();
                int lastReadyMessage = 0;
                while (readyMessagesCursor.moveToNext()) {
                    // NOTE: this could be simpler if we ignore PENDING status on start-session message
                    if (!readyMessagesCursor.isLast() || UploadStatus.PENDING!=readyMessagesCursor.getInt(2)) {
                        JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(1));
                        messagesArray.put(msgObject);
                        lastReadyMessage = readyMessagesCursor.getInt(4);
                    }
                    if( messagesArray.length()>=BATCH_SIZE || (readyMessagesCursor.isLast() && messagesArray.length()>0)) {
                        // create upload message
                        JSONObject uploadMessage =  new JSONObject();
                        uploadMessage.put(MessageKey.TYPE, MessageType.REQUEST_HEADER);
                        uploadMessage.put(MessageKey.ID, UUID.randomUUID().toString());
                        uploadMessage.put(MessageKey.TIMESTAMP, System.currentTimeMillis()/1000);
                        uploadMessage.put(MessageKey.MESSAGES, messagesArray);
                        // TODO: add additional attributes for device
                        // store in uploads table
                        dbInsertUpload(db, uploadMessage);
                        // update message processed status
                        dbUpdateMessagesStatus(db, lastReadyMessage);
                        messagesArray = new JSONArray();
                    }
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error preparing batch upload in mParticle DB", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error with JSON object", e);
        } finally {
            mDB.close();
        }
    }

    void processBatches() {
        try {
            // read batches ready to upload
            SQLiteDatabase db = mDB.getWritableDatabase();
            String[] selectionArgs = new String[]{Integer.toString(UploadStatus.PROCESSED)};
            String[] selectionColumns = new String[]{ "_id", UploadTable.MESSAGE };
            Cursor readyUploadsCursor = db.query("uploads", selectionColumns, UploadTable.UPLOAD_STATUS+"!=?", selectionArgs, null, null, UploadTable.MESSAGE_TIME+" , _id");
            if (readyUploadsCursor.getCount()>0) {
                while (readyUploadsCursor.moveToNext()) {
                    int uploadId=  readyUploadsCursor.getInt(0);
                    String message = readyUploadsCursor.getString(1);
                    // prepare cookies
                    // TODO: verify cookies are setup correctly
                    // post message to mParticle service
                    HttpPost httpPost = new HttpPost(SERVICE_ENDPOINT);
                    httpPost.setHeader("Content-type", "application/json");
                    // StringEntity messageEntity = new StringEntity(message, "UTF8");
                    byte[] messageBytes = message.getBytes();
                    ByteArrayEntity messageEntity = new ByteArrayEntity(messageBytes);
                    httpPost.setEntity(messageEntity);

                    httpPost.setHeader(HEADER_APPKEY, "TestAppKey");
                    httpPost.setHeader(HEADER_SIGNATURE, hmacSha256Encode("secret", message));
                    // TODO: remove - debug mode only
                    httpPost.setHeader("x-mp-debugmode", "true");

                    try {

                        String response = mHttpClient.execute(httpPost, new BasicResponseHandler(), mHttpContext);
                        Log.d(TAG, "Got response 2xx with body string:" + response);
                        List<Cookie> cookies = mCookieStore.getCookies();
                        Log.d(TAG, "Got cookies:" + cookies);
                        // store responses in DB
                        // TODO: parse responses
                        // temporarily strip extra BOM (3 bytes) from response body
                        String cleanResponse = response.substring(response.indexOf('{'));
                        JSONObject responseJSON = new JSONObject(cleanResponse);
                        Log.d(TAG, "Got responseJSON:" + responseJSON.toString());
                        if (responseJSON.has(MessageKey.MESSAGES)) {
                            Log.d(TAG, "Response has messages to be processed");
                            // TODO: store and process command messages
                        }

                    } catch (HttpResponseException e) {
                        Log.d(TAG, "Request failed:" + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Request failed:" + e.getMessage());
                    } catch (JSONException e) {
                        Log.d(TAG, "Failed to process response message JSON");
                    } finally {
                        // TODO: process failures differently
                        dbUpdateUploadStatus(db, uploadId);
                    }
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error preparing batch upload in mParticle DB", e);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Error signing message", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error signing message", e);
        } finally {
            mDB.close();
        }
    }

    private void dbInsertUpload(SQLiteDatabase db, JSONObject message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTable.UPLOAD_ID, message.getString(MessageKey.ID));
        contentValues.put(UploadTable.MESSAGE_TIME, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(UploadTable.MESSAGE, message.toString());
        contentValues.put(UploadTable.UPLOAD_STATUS, UploadStatus.PENDING);
        db.insert("uploads", null, contentValues);
    }

    private void dbUpdateMessagesStatus(SQLiteDatabase db, int lastReadyMessage) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTable.UPLOAD_STATUS, UploadStatus.PROCESSED);
        String[] whereArgs = { Long.toString(lastReadyMessage) };
        db.update("messages", contentValues, "_id<=?", whereArgs);
    }

    private void dbUpdateUploadStatus(SQLiteDatabase db, int uploadId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTable.UPLOAD_STATUS, UploadStatus.PROCESSED);
        String[] whereArgs = { Long.toString(uploadId) };
        db.update("uploads", contentValues, "_id=?", whereArgs);
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
