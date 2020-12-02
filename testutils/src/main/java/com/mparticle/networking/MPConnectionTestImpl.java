package com.mparticle.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLSocketFactory;

public class MPConnectionTestImpl implements MPConnection {
    MPUrl url;
    String requestMethod;
    Boolean doOutput = null;
    Integer connectTimeout = null;
    Integer readTimeout = null;
    Map<String, List<String>> requestProperties = new HashMap<>();
    Integer responseCode = null;

    ByteArrayOutputStream outputStream;
    ByteArrayInputStream inputStream;

    SSLSocketFactory sslSocketFactory;

    String response = "";

    MPConnectionTestImpl(MPUrl url) {
        this.url = url;
    }

    @Override
    public boolean isHttps() {
        return url.getFile().startsWith("https");
    }

    @Override
    public void setRequestMethod(String method) {
        this.requestMethod = method;
    }

    @Override
    public void setDoOutput(Boolean doOutput) {
        this.doOutput = doOutput;
    }

    @Override
    public void setConnectTimeout(Integer timeout) {
        this.connectTimeout = timeout;
    }

    @Override
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public void setRequestProperty(String key, String value) {
        List<String> values = requestProperties.get(key);
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(value);
        requestProperties.put(key, values);
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    @Override
    public MPUrl getURL() {
        return url;
    }

    @Override
    public String getRequestMethod() {
        return requestMethod;
    }

    @Override
    public String getHeaderField(String key) {
        if (requestProperties.containsKey(key)) {
            List<String> strings = requestProperties.get(key);
            if (strings.size() > 1) {
                throw new RuntimeException("Multiple header fields, not implemented.");
            }
            if (strings.size() == 1) {
                return strings.get(0);
            }
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return requestProperties;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new ByteArrayOutputStream();
        }
        return outputStream;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            if (responseCode == null) {
                MockServer.getInstance().onRequestMade(this);
            }
            inputStream = new ByteArrayInputStream(response.getBytes());
        }
        return inputStream;
    }

    @Override
    public InputStream getErrorStream() {
        return null;
    }

    @Override
    public int getResponseCode() throws IOException {
        if (responseCode == null) {
            MockServer.getInstance().onRequestMade(this);
        }
        return responseCode;
    }

    @Override
    public String getResponseMessage() throws IOException {
        return response;
    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory factory) {
        sslSocketFactory = factory;
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    public String getBody() {
        if (outputStream == null) {
            return "{}";
        } else {
            if (getHeaderField("Content-Encoding").equals("gzip")) {
                try {
                    byte[] bytes = outputStream.toByteArray();
                    InputStream inputStream = new ByteArrayInputStream(bytes);
                    GZIPInputStream stream = new GZIPInputStream(inputStream);
                    byte[] data = new byte[32];
                    int bytesRead;
                    StringBuilder builder = new StringBuilder();
                    while ((bytesRead = stream.read(data)) != -1) {
                        builder.append(new String(data, 0, bytesRead));
                    }
                    stream.close();
                    inputStream.close();
                    return builder.toString();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return new String(outputStream.toByteArray());
        }
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Boolean isDoOutput() {
        if (doOutput == null) {
            return false;
        } else {
            return doOutput;
        }
    }
}
