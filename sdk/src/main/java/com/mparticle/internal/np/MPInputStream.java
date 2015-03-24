package com.mparticle.internal.np;


import com.mparticle.MParticle;
import com.mparticle.ConfigManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is a decorator around the given InputStream (or InputStream subclass).
 */
final class MPInputStream extends InputStream {
    private MeasuredRequest measuredRequest;
    private InputStream localInputStream;
    private boolean read;
    private boolean secure;

    public MPInputStream(InputStream paramInputStream) {
        if (paramInputStream == null)
            throw new NullPointerException("InputStream was null");

        localInputStream = paramInputStream;
    }

    public final int available() throws IOException {
        return localInputStream.available();
    }

    public final void close() throws IOException {
        localInputStream.close();
    }

    public final void mark(int readlimit) {
        localInputStream.mark(readlimit);
    }

    public final boolean markSupported() {
        return localInputStream.markSupported();
    }

    public final int read() throws IOException {
        return localInputStream.read();
    }

    public final int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    public final int read(byte[] buffer, int offset, int length) throws IOException {
        read = true;
        length =  localInputStream.read(buffer, offset, length);
        try{
            if (measuredRequest != null) {
                measuredRequest.parseInputStreamBytes(buffer, offset, length);
            }
        }catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Exception thrown while parsing networking InputStream: ", e.getMessage());
        }
        return length;
    }

    public final synchronized void reset() throws IOException {
        localInputStream.reset();
    }

    public final long skip(long byteCount) throws IOException {
        return localInputStream.skip(byteCount);
    }

    public final boolean isSameStream(InputStream inputStream) {
        return localInputStream == inputStream;
    }

    public boolean getRead(){
        return read;
    }

    public void resetRead(){
        read = false;
    }

    public void setMeasuredRequest(MeasuredRequest request){
        measuredRequest = request;
        if (measuredRequest != null && secure){
            measuredRequest.setSecure(secure);
        }
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }
}