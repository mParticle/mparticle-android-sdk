package com.mparticle.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.Map;

public final class MPHttpUrlConnection extends HttpURLConnection {
    private HttpURLConnection b;
    private MeasuredRequest request;

    private MPInputStream inputStream;
    private MPOutputStream outputStream;

    public MPHttpUrlConnection(HttpURLConnection paramHttpURLConnection) {
        super(paramHttpURLConnection.getURL());
        this.b = paramHttpURLConnection;
        request = new MeasuredRequest(paramHttpURLConnection.getURL().toString());
    }

    private void b() {

        /*  long l1 = localp.a("X-Android-Sent-Millis");
          long l2 = localp.a("X-Android-Received-Millis");
        */
    }

    public final void connect() throws IOException {
        this.b.connect();
    }

    public final boolean getAllowUserInteraction() {
        return this.b.getAllowUserInteraction();
    }

    public final void setAllowUserInteraction(boolean newValue) {
        this.b.setAllowUserInteraction(newValue);
    }

    public final Object getContent() throws IOException {
        return b.getContent();
    }

    public final Object getContent(Class[] types) throws IOException {
        return b.getContent(types);

    }

    public final String getContentEncoding() {
        return b.getContentEncoding();
    }

    public final int getContentLength() {
        return this.b.getContentLength();
    }

    public final String getContentType() {
        return this.b.getContentType();
    }

    public final long getDate() {
        return this.b.getDate();
    }

    public final boolean getDefaultUseCaches() {
        return this.b.getDefaultUseCaches();
    }

    public final void setDefaultUseCaches(boolean newValue) {
        this.b.setDefaultUseCaches(newValue);
    }

    public final boolean getDoInput() {
        return this.b.getDoInput();
    }

    public final void setDoInput(boolean newValue) {
        this.b.setDoInput(newValue);
    }

    public final boolean getDoOutput() {
        return this.b.getDoOutput();
    }

    public final void setDoOutput(boolean newValue) {
        this.b.setDoOutput(newValue);
    }

    public final long getExpiration() {
        return this.b.getExpiration();
    }

    public final String getHeaderField(int pos) {
        return this.b.getHeaderField(pos);
    }

    public final Map getHeaderFields() {
        return b.getHeaderFields();
    }

    public final Map getRequestProperties() {
        return this.b.getRequestProperties();
    }

    public final void addRequestProperty(String field, String newValue) {
        this.b.addRequestProperty(field, newValue);
    }

    public final String getHeaderField(String key) {
        return b.getHeaderField(key);
    }

    public final long getHeaderFieldDate(String field, long defaultValue) {
        return this.b.getHeaderFieldDate(field, defaultValue);
    }

    public final int getHeaderFieldInt(String field, int defaultValue) {
        return this.b.getHeaderFieldInt(field, defaultValue);
    }

    public final String getHeaderFieldKey(int posn) {
        return this.b.getHeaderFieldKey(posn);
    }

    public final long getIfModifiedSince() {
        return this.b.getIfModifiedSince();
    }

    public final void setIfModifiedSince(long newValue) {
        this.b.setIfModifiedSince(newValue);
    }



    public final long getLastModified() {
        return this.b.getLastModified();
    }

    public final OutputStream getOutputStream() throws IOException {
        this.request.startTiming();
        OutputStream outputStream = this.b.getOutputStream();
        if (outputStream != null){
            if (this.outputStream != null){
                return this.outputStream;
            }else{
                this.outputStream = new MPOutputStream(outputStream, request);
            }
        }
        return this.outputStream;
    }
    public final InputStream getInputStream() throws IOException {
        InputStream inputStreams = this.b.getInputStream();
        if (inputStreams != null){
            if (this.inputStream != null && inputStream.isSameStream(inputStreams)){
                return this.inputStream;
            }else{
                this.inputStream = new MPInputStream(inputStreams, request);
            }
        }
        return this.inputStream;

    }

    public final Permission getPermission() throws IOException {
        return this.b.getPermission();
    }

    public final String getRequestProperty(String field) {
        return this.b.getRequestProperty(field);
    }

    public final URL getURL() {
        return this.b.getURL();
    }

    public final boolean getUseCaches() {
        return this.b.getUseCaches();
    }

    public final void setUseCaches(boolean newValue) {
        this.b.setUseCaches(newValue);
    }

    public final void setRequestProperty(String field, String newValue) {
        this.b.setRequestProperty(field, newValue);
    }

    public final int getConnectTimeout() {
        return this.b.getConnectTimeout();
    }

    public final void setConnectTimeout(int timeoutMillis) {
        this.b.setConnectTimeout(timeoutMillis);
    }

    public final int getReadTimeout() {
        return this.b.getReadTimeout();
    }

    public final void setReadTimeout(int timeoutMillis) {
        this.b.setReadTimeout(timeoutMillis);
    }

    public final String toString() {
        return this.b.toString();
    }

    public final void disconnect() {
        this.b.disconnect();
    }

    public final boolean usingProxy() {
        return this.b.usingProxy();
    }

    public final InputStream getErrorStream() {
        return this.b.getErrorStream();
    }

    public final String getRequestMethod() {
        return this.b.getRequestMethod();
    }

    public final void setRequestMethod(String method) throws ProtocolException {
        this.b.setRequestMethod(method);
    }

    public final int getResponseCode() throws IOException {
        return b.getResponseCode();
    }

    public final String getResponseMessage() throws IOException {
        return b.getResponseMessage();
    }

    public final boolean getInstanceFollowRedirects() {
        return this.b.getInstanceFollowRedirects();
    }

    public final void setInstanceFollowRedirects(boolean followRedirects) {
        this.b.setInstanceFollowRedirects(followRedirects);
    }

    public final void setFixedLengthStreamingMode(int contentLength) {
        this.b.setFixedLengthStreamingMode(contentLength);
    }

    public final void setChunkedStreamingMode(int chunkLength) {
        this.b.setChunkedStreamingMode(chunkLength);
    }

    public final boolean equals(Object o) {
        return this.b.equals(o);
    }

    public final int hashCode() {
        return this.b.hashCode();
    }
}