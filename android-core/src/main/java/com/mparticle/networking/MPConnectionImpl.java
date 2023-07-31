package com.mparticle.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

class MPConnectionImpl implements MPConnection {
    private HttpURLConnection httpURLConnection;
    private MPUrl url;
    private Integer responseCode = null;

    public MPConnectionImpl(HttpURLConnection mpUrl, MPUrl url) {
        this.httpURLConnection = mpUrl;
        this.url = url;
    }


    @Override
    public boolean isHttps() {
        return httpURLConnection instanceof HttpsURLConnection;
    }

    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        httpURLConnection.setRequestMethod(method);
    }

    @Override
    public void setDoOutput(Boolean doOutput) {
        httpURLConnection.setDoOutput(doOutput);
    }

    @Override
    public void setConnectTimeout(Integer timeout) {
        httpURLConnection.setConnectTimeout(timeout);
    }

    @Override
    public void setReadTimeout(Integer readTimeout) {
        httpURLConnection.setReadTimeout(readTimeout);
    }

    @Override
    public void setRequestProperty(String key, String value) {
        httpURLConnection.setRequestProperty(key, value);
    }

    @Override
    public MPUrl getURL() {
        return url;
    }

    @Override
    public String getRequestMethod() {
        return httpURLConnection.getRequestMethod();
    }

    @Override
    public String getHeaderField(String key) {
        return httpURLConnection.getHeaderField(key);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return httpURLConnection.getHeaderFields();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return httpURLConnection.getOutputStream();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return httpURLConnection.getInputStream();
    }

    @Override
    public InputStream getErrorStream() {
        return httpURLConnection.getErrorStream();
    }

    @Override
    public int getResponseCode() throws IOException {
        if (responseCode == null) {
            return httpURLConnection.getResponseCode();
        } else {
            return responseCode;
        }
    }

    @Override
    public String getResponseMessage() throws IOException {
        return httpURLConnection.getResponseMessage();
    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory factory) {
        ((HttpsURLConnection) httpURLConnection).setSSLSocketFactory(factory);
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return ((HttpsURLConnection) httpURLConnection).getSSLSocketFactory();
    }
}
