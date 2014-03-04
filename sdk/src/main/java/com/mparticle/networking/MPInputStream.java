package com.mparticle.networking;


import java.io.IOException;
import java.io.InputStream;

public final class MPInputStream extends InputStream {
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
        return localInputStream.read(buffer);
    }

    public final int read(byte[] buffer, int offset, int length) throws IOException {
        length =  localInputStream.read(buffer, offset, length);
     //   measuredRequest.parseResponse(buffer, offset, length);
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