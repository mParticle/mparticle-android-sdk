package com.mparticle;


import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by sdozor on 3/4/14.
 *
 * This is a decorator around the given InputStream (or InputStream subclass).
 */
final class MPInputStream extends InputStream {
    private MeasuredRequest measuredRequest;
    private InputStream localInputStream;

    public MPInputStream(InputStream paramInputStream, MeasuredRequest request) {
        if (paramInputStream == null)
            throw new NullPointerException("InputStream was null");

        localInputStream = paramInputStream;
        measuredRequest = request;
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
        length =  localInputStream.read(buffer, offset, length);
        try{
            measuredRequest.parseInputStreamBytes(buffer, offset, length);
        }catch (Exception e){
            if (MParticle.getInstance().getDebugMode()){
                Log.w(Constants.LOG_TAG, "Exception thrown while parsing networking InputStream: " + e.getMessage());
            }
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
}