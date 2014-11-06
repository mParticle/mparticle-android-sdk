package com.mparticle;

import org.apache.http.Header;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.CharArrayBuffer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by sdozor on 3/4/14.
 */
final class MeasuredRequest {
    //max time to wait before logging this request
    //TODO: this could be set to the timeout of the given socket/connection
    private static final long TIMEOUT = 120;
    boolean endOfStream = false;
    private long headerStartTime = 0L;
    private long streamSystemNanoEndTime = 0L;
    private long streamSystemNanoStartTime = 0L;
    private long streamSystemMilliStartTime = 0L;
    private long headerEndTime = 0L;
    private long responseContentLength = 0L;
    private long requestContentLength = 0L;
    private long streamBytesWritten = 0L;
    private long streamBytesRead = 0L;
    private int responseCode = 0;
    private String requestMethod;
    private String host;
    private URL url;
    private ByteArrayBuffer outputByteBuffer;
    private ByteArrayBuffer inputByteBuffer;
    private boolean chunked;
    private boolean parseHeaders = true;
    private boolean added;
    private boolean secure;

    public MeasuredRequest() {
        log();
        startTiming();
    }

    public boolean foundHeaderTiming() {
        return headerStartTime > 0 && headerEndTime > 0;
    }

    private final void startTiming() {
        if (streamSystemNanoStartTime == 0L) {
            streamSystemNanoStartTime = MPUtility.millitime();
            streamSystemMilliStartTime = System.currentTimeMillis();
        }
    }

    String getUri() {
        String uri;
        if (url != null) {
            uri = url.getHost().toString() + url.getPath().toString();
        } else {
            uri = host;
        }
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        if (uri != null && !uri.startsWith("http://")) {
            return (secure ? "https://" : "http://") + uri;
        } else {
            return uri;
        }
    }

    long getTotalTime() {
        long headerTime = headerEndTime - headerStartTime;
        if (headerTime > 0) {
            return headerTime;
        }
        return streamSystemNanoEndTime - streamSystemNanoStartTime;
    }

    /*public String getKey() {
        String key = (getStartTime() / 1000) + getUri() + requestMethod;
       // Log.e(Constants.LOG_TAG, "KEY: " + key);
        return key;
    }*/

    public void parseUrlResponse(HttpURLConnection connection) {
        streamSystemNanoEndTime = MPUtility.millitime();
        if (!foundHeaderTiming() && responseContentLength == 0) {
            try {
                requestMethod = connection.getRequestMethod();
                responseCode = connection.getResponseCode();
                url = connection.getURL();
                responseContentLength = computeHeaderSize(connection.getHeaderFields());
                int contentLength = connection.getContentLength();
                if (contentLength > 0) {
                    responseContentLength += contentLength;
                }
                //these are special headers the UrlConnection adds internally
                headerStartTime = Long.parseLong(connection.getHeaderField("X-Android-Sent-Millis"));
                headerEndTime = Long.parseLong(connection.getHeaderField("X-Android-Received-Millis"));
            } catch (Exception e) {

            } finally {
              //  log();
            }
        }

    }

    private long computeHeaderSize(Map<String, List<String>> headerFields) {
        long byteCount = 0;
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if (entry.getKey() != null) {
                byteCount += entry.getKey().getBytes().length;
            }
            if (entry.getValue() != null) {
                for (String str : entry.getValue()) {
                    byteCount += str.getBytes().length;
                }
            }
        }
        return byteCount;
    }

    private void log() {
        if (!"CONNECT".equalsIgnoreCase(requestMethod)) {
            added = true;
            startTiming();
            MParticle.getInstance().measuredRequestManager.addRequest(this);
        }
    }

    //this is an optimization to prevent us from reading the entire request/response into memory
    //TODO: improve to account for when the header boundary is split across buffers.
    private int findEndOfHeaders(byte[] buffer) {
        try {
            for (int i = 0; i < buffer.length - 3; i++) {
                if (buffer[i] == 13 &&
                        buffer[i + 1] == 10 &&
                        buffer[i + 2] == 13 &&
                        buffer[i + 3] == 10) {
                    return (i + 3);
                }
            }
        } catch (Exception e) {

        }
        return -1;
    }

    public void parseInputStreamBytes(byte[] buffer, int offset, int length) throws Exception {
        streamSystemNanoEndTime = MPUtility.millitime();
        //if we're passed a length of -1, then that means there's nothing more to parse.
        if (length == -1) {
            endOfStream = true;
            return;
        }

        int respbodyIndex = findEndOfHeaders(buffer);
        if (respbodyIndex < 0) {
            respbodyIndex = length;
        }

        if (outputByteBuffer == null) {
            outputByteBuffer = new ByteArrayBuffer(respbodyIndex);
        }

        outputByteBuffer.append(buffer, offset, respbodyIndex);
        streamBytesRead += length;

        //if we already know it's chunked there's no point in trying to parse the headers again
        if (!chunked && parseHeaders) {
            try {
                ByteArrayInputStream is = new ByteArrayInputStream(outputByteBuffer.toByteArray());

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String responseLine = reader.readLine();
                CharArrayBuffer firstLine = new CharArrayBuffer(responseLine.length());
                firstLine.append(responseLine);
                ParserCursor cursor = new ParserCursor(0, firstLine.length());
                responseCode = BasicLineParser.DEFAULT.parseStatusLine(firstLine, cursor).getStatusCode();

                String line;
                while ((line = reader.readLine()) != null && responseContentLength == 0 && !chunked) {
                    try {
                        CharArrayBuffer nextBuffer = new CharArrayBuffer(line.length());
                        nextBuffer.append(line);
                        Header header = BasicLineParser.DEFAULT.parseHeader(nextBuffer);
                        if (header.getValue() != null &&
                                header.getName().equalsIgnoreCase("content-length")) {
                            responseContentLength = Long.parseLong(header.getValue());
                            break;
                        } else if (header.getValue() != null &&
                                header.getName().equalsIgnoreCase("transfer-encoding")) {
                            chunked = header.getValue().equalsIgnoreCase("chunked");
                            break;
                        }
                    } catch (org.apache.http.ParseException pse) {
                        //swallow weird headers
                    }

                }
            } catch (org.apache.http.ParseException pse) {
                //swallow weird headers
            }
        }
    }

    public boolean readyForLogging() {
        if ("CONNECT".equalsIgnoreCase(getMethod()) || getTotalTime() < 0 || (MPUtility.millitime() - getStartTime() < 2000) && responseCode > 0){
            return false;
        }
        return (endOfStream ||
                (responseContentLength > 0 && streamBytesRead >= responseContentLength) ||
                (MPUtility.millitime() - getStartTime()) > TIMEOUT);
    }

    //This is needed in the case where a Socket is reused.
    public void reset() {
        streamSystemMilliStartTime = 0L;
        streamSystemNanoEndTime = 0L;
        streamSystemNanoStartTime = 0L;
        headerStartTime = 0L;
        headerEndTime = 0L;
        responseContentLength = 0L;
        requestContentLength = 0L;
        streamBytesWritten = 0L;
        streamBytesRead = 0L;
        responseCode = 0;
        outputByteBuffer = null;
        inputByteBuffer = null;
        endOfStream = false;
        chunked = false;
        added = false;
        parseHeaders = true;
    }

    public void parseOutputStreamBytes(byte[] buffer, int offset, int length) throws Exception {
        startTiming();


        if (inputByteBuffer == null) {
            inputByteBuffer = new ByteArrayBuffer(length);
        }

        inputByteBuffer.append(buffer, offset, length);

        streamBytesWritten += length;

        if (!isRequestParsed()) {
            try {
                ByteArrayInputStream is = new ByteArrayInputStream(inputByteBuffer.toByteArray());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String firstLine = reader.readLine();

                CharArrayBuffer firstLineBuffer = new CharArrayBuffer(firstLine.length());
                firstLineBuffer.append(firstLine);
                ParserCursor cursor = new ParserCursor(0, firstLineBuffer.length());
                RequestLine requestLine = BasicLineParser.DEFAULT.parseRequestLine(firstLineBuffer, cursor);

                requestMethod = requestLine.getMethod();

                String nextLine;
                while ((nextLine = reader.readLine()) != null && !isRequestParsed()) {
                    try {
                        CharArrayBuffer nextBuffer = new CharArrayBuffer(nextLine.length());
                        nextBuffer.append(nextLine);
                        Header header = BasicLineParser.DEFAULT.parseHeader(nextBuffer);
                        if (header.getValue() != null) {
                            if (header.getName().equalsIgnoreCase("content-length")) {
                                requestContentLength = Math.max(Long.parseLong(header.getValue()), requestContentLength);
                            } else if (header.getName().equalsIgnoreCase("host")) {
                                String headerHost = header.getValue();
                                if (requestLine.getUri().contains(headerHost)) {
                                    host = requestLine.getUri();
                                } else {
                                    host = header.getValue() + requestLine.getUri();
                                }

                            }
                        }
                    } catch (org.apache.http.ParseException pse) {
                        //just swallow weird headers
                    }
                }
            } catch (org.apache.http.ParseException pse) {
                //just swallow weird headers
            }
        }
    }

    private boolean isRequestParsed() {
        return requestMethod != null && requestContentLength > 0 && host != null;
    }

    public long getStartTime() {
        return headerStartTime > 0 ? headerStartTime : streamSystemMilliStartTime;
    }

    @Override
    public final String toString() {
        String str = "\n";
        str = str + "URI                      : " + getUri() + "\n";
        str = str + "Request method           : " + requestMethod + "\n";
        str = str + "Response time            : " + getTotalTime() + "\n";
        str = str + "Bytes sent               : " + getBytesSent() + "\n";
        str = str + "Bytes received           : " + getBytesReceived() + "\n";
        str = str + "Response code            : " + responseCode + "\n";
        /*
        str = str + "ID                       : " + id + "\n";
        str = str + "Stream start time        : " + streamStartTime + "\n";
        str = str + "Stream end time          : " + streamEndTime + "\n";
        str = str + "Header start time        : " + headerStartTime + "\n";
        str = str + "Header end time          : " + headerEndTime + "\n";
        str = str + "Req. Content-Length      : " + requestContentLength + "\n";
        str = str + "Resp. Content-Length     : " + responseContentLength + "\n";
        str = str + "Stream bytes written     : " + streamBytesWritten + "\n";
        str = str + "Stream bytes read        : " + streamBytesRead + "\n";
        str = str + "Key                      : " + getKey() + "\n";
        */
        return str;
    }

    public void setParseHeaders(boolean parseHeaders) {
        this.parseHeaders = parseHeaders;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getMethod() {
        return requestMethod;
    }

    public long getBytesSent() {
        return Math.max(getUri().getBytes().length, Math.max(streamBytesWritten, requestContentLength));
    }

    public long getBytesReceived() {
        return Math.max(streamBytesRead, requestContentLength);
    }

    public void setUri(URL uri) {
        this.url = uri;
    }

    public String getRequestString() {
        if ("POST".equalsIgnoreCase(requestMethod) && getUri().contains("google-analytics.com")) {
            try {
                byte[] entireRequest = inputByteBuffer.toByteArray();
                return new String(entireRequest, "UTF-8").split("\r\n\r\n")[1];
            } catch (Exception use) {

            }
        }
        return null;
    }
}