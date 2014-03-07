package com.mparticle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

/**
 * Created by sdozor on 3/4/14.
 *
 * This a decorator around the given UrlConnection that handle parsing requests/responses in the
 * many methods that a developer can initiate a request.
 */
final class MPHttpUrlConnection extends HttpURLConnection {
    private final MeasuredRequest request;
    private HttpURLConnection delegateConnection = null;

    private MPInputStream inputStream;
    private MPOutputStream outputStream;

    public MPHttpUrlConnection(HttpURLConnection connection) {
        super(connection.getURL());
        request = new MeasuredRequest();
        request.setParseHeaders(false);
        delegateConnection = connection;
    }

    @Override
    public void disconnect() {
        delegateConnection.disconnect();
    }

    @Override
    public InputStream getErrorStream() {
        return delegateConnection.getErrorStream();
    }

    @Override
    public Permission getPermission() throws IOException {
        return delegateConnection.getPermission();
    }

    @Override
    public String getRequestMethod() {
        return delegateConnection.getRequestMethod();
    }

    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        delegateConnection.setRequestMethod(method);
    }

    @Override
    public int getResponseCode() throws IOException {
        request.startTiming();
        int code = delegateConnection.getResponseCode();
        request.parseUrlResponse(delegateConnection);
        return code;
    }

    @Override
    public String getResponseMessage() throws IOException {
        request.startTiming();
        String message = delegateConnection.getResponseMessage();
        request.parseUrlResponse(delegateConnection);
        return message;
    }

    @Override
    public boolean usingProxy() {
        return delegateConnection.usingProxy();
    }

    @Override
    public String getContentEncoding() {
        request.startTiming();
        String encoding = delegateConnection.getContentEncoding();
        request.parseUrlResponse(delegateConnection);
        return encoding;
    }

    @Override
    public boolean getInstanceFollowRedirects() {
        return delegateConnection.getInstanceFollowRedirects();
    }

    @Override
    public void setInstanceFollowRedirects(boolean followRedirects) {
        delegateConnection.setInstanceFollowRedirects(followRedirects);
    }

    @Override
    public long getHeaderFieldDate(String field, long defaultValue) {
        return delegateConnection.getHeaderFieldDate(field, defaultValue);
    }

    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
        delegateConnection.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setChunkedStreamingMode(int chunkLength) {
        delegateConnection.setChunkedStreamingMode(chunkLength);
    }

    @Override
    public void connect() throws IOException {
        request.startTiming();
        delegateConnection.connect();
        request.parseUrlResponse(delegateConnection);
    }

    @Override
    public boolean getAllowUserInteraction() {
        return delegateConnection.getAllowUserInteraction();
    }

    @Override
    public void setAllowUserInteraction(boolean newValue) {
        delegateConnection.setAllowUserInteraction(newValue);
    }

    @Override
    public Object getContent() throws IOException {
        Object content = delegateConnection.getContent();
        request.parseUrlResponse(delegateConnection);
        return content;
    }

    @Override
    public Object getContent(Class[] types) throws IOException {
        Object object = delegateConnection.getContent(types);
        request.parseUrlResponse(delegateConnection);
        return object;
    }

    @Override
    public int getContentLength() {
        return delegateConnection.getContentLength();
    }

    @Override
    public String getContentType() {
        String type = delegateConnection.getContentType();
        request.parseUrlResponse(delegateConnection);
        return type;
    }

    @Override
    public long getDate() {
        return delegateConnection.getDate();
    }

    @Override
    public boolean getDefaultUseCaches() {
        return delegateConnection.getDefaultUseCaches();
    }

    @Override
    public void setDefaultUseCaches(boolean newValue) {
        delegateConnection.setDefaultUseCaches(newValue);
    }

    @Override
    public boolean getDoInput() {
        return delegateConnection.getDoInput();
    }

    @Override
    public void setDoInput(boolean newValue) {
        delegateConnection.setDoInput(newValue);
    }

    @Override
    public boolean getDoOutput() {
        return delegateConnection.getDoOutput();
    }

    @Override
    public void setDoOutput(boolean newValue) {
        delegateConnection.setDoOutput(newValue);
    }

    @Override
    public long getExpiration() {
        return delegateConnection.getExpiration();
    }

    @Override
    public String getHeaderField(int pos) {
        String headerField = delegateConnection.getHeaderField(pos);
        request.parseUrlResponse(delegateConnection);
        return headerField;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        Map<String, List<String>> headerFields = delegateConnection.getHeaderFields();
        request.parseUrlResponse(delegateConnection);
        return headerFields;
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return delegateConnection.getRequestProperties();
    }

    @Override
    public void addRequestProperty(String field, String newValue) {
        delegateConnection.addRequestProperty(field, newValue);
    }

    @Override
    public String getHeaderField(String key) {
        String headerField = delegateConnection.getHeaderField(key);
        request.parseUrlResponse(delegateConnection);
        return headerField;
    }

    @Override
    public int getHeaderFieldInt(String field, int defaultValue) {
        int headerFieldInt = delegateConnection.getHeaderFieldInt(field, defaultValue);
        request.parseUrlResponse(delegateConnection);
        return headerFieldInt;
    }

    @Override
    public String getHeaderFieldKey(int posn) {
        String headerFieldKey = delegateConnection.getHeaderFieldKey(posn);
        request.parseUrlResponse(delegateConnection);
        return headerFieldKey;
    }

    @Override
    public long getIfModifiedSince() {
        return delegateConnection.getIfModifiedSince();
    }

    @Override
    public void setIfModifiedSince(long newValue) {
        delegateConnection.setIfModifiedSince(newValue);
    }

    @Override
    public String getRequestProperty(String field) {
        return delegateConnection.getRequestProperty(field);
    }

    @Override
    public URL getURL() {
        return delegateConnection.getURL();
    }

    @Override
    public boolean getUseCaches() {
        return delegateConnection.getUseCaches();
    }

    @Override
    public void setUseCaches(boolean newValue) {
        delegateConnection.setUseCaches(newValue);
    }

    @Override
    public void setRequestProperty(String field, String newValue) {
        delegateConnection.setRequestProperty(field, newValue);
    }

    @Override
    public int getConnectTimeout() {
        return delegateConnection.getConnectTimeout();
    }

    @Override
    public void setConnectTimeout(int timeoutMillis) {
        delegateConnection.setConnectTimeout(timeoutMillis);
    }

    @Override
    public int getReadTimeout() {
        return delegateConnection.getReadTimeout();
    }

    @Override
    public void setReadTimeout(int timeoutMillis) {
        delegateConnection.setReadTimeout(timeoutMillis);
    }

    @Override
    public String toString() {
        return delegateConnection.toString();
    }

    @Override
    public boolean equals(Object o) {
        return delegateConnection.equals(o);
    }

    @Override
    public int hashCode() {
        return delegateConnection.hashCode();
    }

    @Override
    public final long getLastModified() {
        return delegateConnection.getLastModified();
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        OutputStream stream = delegateConnection.getOutputStream();

        if (stream != null) {
            if (this.outputStream != null) {
                return this.outputStream;
            } else {
                this.outputStream = new MPOutputStream(stream, request);
            }
        }
        return this.outputStream;
    }

    @Override
    public final InputStream getInputStream() throws IOException {
        try {
            InputStream inputStreams = this.delegateConnection.getInputStream();
            request.parseUrlResponse(delegateConnection);
            if (inputStreams != null) {
                if (this.inputStream != null && inputStream.isSameStream(inputStreams)) {
                    return this.inputStream;
                } else {
                    this.inputStream = new MPInputStream(inputStreams, request);
                }
            }
            return this.inputStream;
        } catch (IOException e) {
            request.parseUrlResponse(delegateConnection);
            throw e;
        }
    }
}