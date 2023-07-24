package com.mparticle.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

public interface MPConnection {
    boolean isHttps();


    void setRequestMethod(String method) throws ProtocolException;

    void setDoOutput(Boolean doOutput);

    void setConnectTimeout(Integer timeout);

    void setReadTimeout(Integer readTimeout);

    void setRequestProperty(String key, String value);

    MPUrl getURL();

    String getRequestMethod();

    String getHeaderField(String key);

    Map<String, List<String>> getHeaderFields();

    OutputStream getOutputStream() throws IOException;

    InputStream getInputStream() throws IOException;

    InputStream getErrorStream();

    int getResponseCode() throws IOException;

    String getResponseMessage() throws IOException;

    void setSSLSocketFactory(SSLSocketFactory factory);

    SSLSocketFactory getSSLSocketFactory();
}
