package com.mparticle.networking;

import android.util.Log;

import com.mparticle.MParticle;

import org.apache.http.Header;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.CharArrayBuffer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;

public final class MeasuredRequest {
    public long startTime = Long.MAX_VALUE;
    public long bytesIn = 0L;
    public long bytesOut = 0L;
    public int responseCode = 0;
    public String requestMethod = "";
    public String host;
    private long endTime = Long.MAX_VALUE;
    private ByteArrayBuffer outputByteBuffer;
    private ByteArrayBuffer inputByteBuffer;

    public MeasuredRequest() {
    }

    public MeasuredRequest(String paramString) {
        if (paramString != null)
            this.host = paramString;
    }

    public MeasuredRequest(URL paramURL) {
        if (paramURL != null)
            this.host = paramURL.toExternalForm();
    }

    public final String a() {
      /*  Object localObject1;
        if ((localObject1 = this.i) == null)
        {
            Object localObject2 = localObject1 = this.h;
            String str1 = "unknown-host";
            if (((endTime)localObject2).MeasuredRequest != null)
                str1 = ((endTime)localObject2).MeasuredRequest;
            else if (((endTime)localObject2).a != null)
                str1 = ((endTime)localObject2).a.getHostName();
            int i1 = ((endTime)localObject1).responseCode;
            localObject2 = str1;
            if (i1 > 0)
                localObject1 = ":" + i1;
            String str2 = ((endTime)localObject1).c;
            String str3 = "";
            if (((endTime)localObject1).bytesOut != null)
                str3 = str3 + endTime.a.a(((endTime)localObject1).bytesOut) + ":";
            str3 = str3 + "//";
            localObject2 = "";
            if ((((endTime)localObject1).responseCode > 0) && ((((endTime)localObject1).bytesOut == null) || (endTime.a.MeasuredRequest(((endTime)localObject1).bytesOut) != ((endTime)localObject1).responseCode)))
            {
                localObject1 = ":" + ((endTime)localObject1).responseCode;
                if (!i1.endsWith((String)localObject1))
                    localObject2 = localObject1;
            }
            localObject1 = str3 + i1 + (String)localObject2 + str2;
            this.i = ((String)localObject1);
        }
        return localObject1;*/
        return "IMPLEMENT ME";
    }

    private long getTotalTime() {
        if (this.startTime != Long.MAX_VALUE && this.endTime != Long.MAX_VALUE){
            return this.endTime - this.startTime;
        }
        return 0;
    }

    public final void startTiming() {
        if (startTime == Long.MAX_VALUE){
            startTime = System.currentTimeMillis();
        }
    }

    public final void endTiming() {
        endTime = System.currentTimeMillis();
    }

    public final String toString() {
        String str = "";
        str = str + "URI            : " + this.host + "\n";
        str = str + "Response time  : " + getTotalTime() + "\n";
        str = str + "Start time     : " + this.startTime + "\n";
        str = str + "End time       : " + this.endTime + "\n";
        str = str + "Bytes up       : " + this.bytesOut + "\n";
        str = str + "Bytes down     : " + this.bytesIn + "\n";
        str = str + "Response code  : " + this.responseCode + "\n";
        str = str + "Request method : " + this.requestMethod + "\n";
        return str;
    }

    public void parseResponse(byte[] buffer, int offset, int length) {
        Log.d(MParticle.NETWORK_TAG, "PARSING RESPONSE");
        endTiming();
        if (!isResponseParsed()){
            if (outputByteBuffer == null){
                outputByteBuffer = new ByteArrayBuffer(length);
            }
            outputByteBuffer.append(buffer, offset, length);
            try{
                ByteArrayInputStream is = new ByteArrayInputStream(outputByteBuffer.toByteArray());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String responseLine = reader.readLine();
                CharArrayBuffer firstLine = new CharArrayBuffer(responseLine.length());
                firstLine.append(responseLine);
                ParserCursor cursor = new ParserCursor(0, firstLine.length());
                responseCode = BasicLineParser.DEFAULT.parseStatusLine(firstLine, cursor).getStatusCode();

                while (true){
                    String nextLine = reader.readLine();

                    CharArrayBuffer nextBuffer = new CharArrayBuffer(nextLine.length());
                    nextBuffer.append(nextLine);
                    Header header = BasicLineParser.DEFAULT.parseHeader(nextBuffer);
                    if (header.getValue() != null){
                        if (header.getName().equalsIgnoreCase("content-length")){
                            bytesIn = Integer.parseInt(header.getValue());
                            Log.d(MParticle.NETWORK_TAG, toString());
                            break;
                        }
                    }
                }
            }catch (Exception e){
                String test = "";
            }
        }
    }

    private boolean isResponseParsed(){
        return responseCode > 0 && bytesIn > 0;
    }

    private boolean isRequestParsed(){
        return requestMethod != null && bytesOut > 0 && host != null;
    }

    public void parseRequest(byte[] buffer, int offset, int length) {
        startTiming();
        if (!isRequestParsed()){
            if (inputByteBuffer == null){
                inputByteBuffer = new ByteArrayBuffer(length);
            }
            inputByteBuffer.append(buffer, offset, length);
            try{
                ByteArrayInputStream is = new ByteArrayInputStream(inputByteBuffer.toByteArray());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String requestLine = reader.readLine();
                CharArrayBuffer firstLine = new CharArrayBuffer(requestLine.length());
                firstLine.append(requestLine);
                ParserCursor cursor = new ParserCursor(0, requestLine.length());
                RequestLine line = BasicLineParser.DEFAULT.parseRequestLine(firstLine, cursor);
                requestMethod = line.getMethod();

                while (true){
                    String nextLine = reader.readLine();

                    CharArrayBuffer nextBuffer = new CharArrayBuffer(nextLine.length());
                    nextBuffer.append(nextLine);
                    Header header = BasicLineParser.DEFAULT.parseHeader(nextBuffer);
                    if (header.getValue() != null){
                        if (header.getName().equalsIgnoreCase("content-length")){
                            bytesOut = Integer.parseInt(header.getValue());
                        }else if (header.getName().equalsIgnoreCase("host")){
                            host = header.getValue() + line.getUri();
                        }
                    }
                }
            }catch (Exception e){
                String test = "";
            }
        }
    }
}