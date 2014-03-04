package com.mparticle.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

public final class MPHttpsUrlConnection extends HttpsURLConnection {
    private final MeasuredRequest request;
    private HttpsURLConnection b = null;

    private MPInputStream inputStream;
    private MPOutputStream outputStream;

    public MPHttpsUrlConnection(HttpsURLConnection paramHttpsURLConnection) {
        super(paramHttpsURLConnection.getURL());
        request = new MeasuredRequest(paramHttpsURLConnection.getURL().toString());
        this.b = paramHttpsURLConnection;
    }


    private void b() {
    /*
          long l1 = localp.a("X-Android-Sent-Millis");
          long l2 = localp.a("X-Android-Received-Millis");
 */
    }

    public final String getCipherSuite() {
        return this.b.getCipherSuite();
    }

    public final HostnameVerifier getHostnameVerifier() {
        return this.b.getHostnameVerifier();
    }

    public final void setHostnameVerifier(HostnameVerifier v) {
        this.b.setHostnameVerifier(v);
    }

    public final Certificate[] getLocalCertificates() {
        return this.b.getLocalCertificates();
    }

    public final Principal getLocalPrincipal() {
        return this.b.getLocalPrincipal();
    }

    public final Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return this.b.getPeerPrincipal();
    }

    public final SSLSocketFactory getSSLSocketFactory() {
        return this.b.getSSLSocketFactory();
    }

    public final void setSSLSocketFactory(SSLSocketFactory sf) {
        this.b.setSSLSocketFactory(sf);
    }

    public final Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
        return this.b.getServerCertificates();
    }

    public final void disconnect() {
        this.b.disconnect();
    }

    public final InputStream getErrorStream() {
        return b.getErrorStream();
    }

    public final long getHeaderFieldDate(String field, long defaultValue) {
        return b.getHeaderFieldDate(field, defaultValue);
    }

    public final boolean getInstanceFollowRedirects() {
        return this.b.getInstanceFollowRedirects();
    }

    public final void setInstanceFollowRedirects(boolean followRedirects) {
        this.b.setInstanceFollowRedirects(followRedirects);
    }

    public final Permission getPermission() throws IOException {
        return this.b.getPermission();
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

    public final void setChunkedStreamingMode(int chunkLength) {
        this.b.setChunkedStreamingMode(chunkLength);
    }

    public final void setFixedLengthStreamingMode(int contentLength) {
        this.b.setFixedLengthStreamingMode(contentLength);
    }

    public final boolean usingProxy() {
        return this.b.usingProxy();
    }

    public final void addRequestProperty(String field, String newValue) {
        this.b.addRequestProperty(field, newValue);
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

    public final int getConnectTimeout() {
        return this.b.getConnectTimeout();
    }

    public final void setConnectTimeout(int timeoutMillis) {
        this.b.setConnectTimeout(timeoutMillis);
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
        return b.getHeaderField(pos);
    }

    public final String getHeaderField(String key) {
        return b.getHeaderField(key);
    }

    public final int getHeaderFieldInt(String field, int defaultValue) {

        return b.getHeaderFieldInt(field, defaultValue);

    }

    public final String getHeaderFieldKey(int posn) {
        return b.getHeaderFieldKey(posn);

    }

    public final Map getHeaderFields() {
        return b.getHeaderFields();
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
        if (outputStream != null) {
            if (this.outputStream != null) {
                return this.outputStream;
            } else {
                this.outputStream = new MPOutputStream(outputStream, request);
            }
        }
        return this.outputStream;
    }

    public final InputStream getInputStream() throws IOException {
        InputStream inputStreams = this.b.getInputStream();
        if (inputStreams != null) {
            if (this.inputStream != null && inputStream.isSameStream(inputStreams)) {
                return this.inputStream;
            } else {
                this.inputStream = new MPInputStream(inputStreams, request);
            }
        }
        return this.inputStream;

    }

    public final int getReadTimeout() {
        return this.b.getReadTimeout();
    }

    public final void setReadTimeout(int timeoutMillis) {
        this.b.setReadTimeout(timeoutMillis);
    }

    public final Map getRequestProperties() {
        return this.b.getRequestProperties();
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

    public final String toString() {
        return this.b.toString();
    }

    public final boolean equals(Object o) {
        return this.b.equals(o);
    }

    public final int hashCode() {
        return this.b.hashCode();
    }
}