package com.mparticle.networking;

import java.io.IOException;
import java.io.OutputStream;

public final class MPOutputStream extends OutputStream {

    private OutputStream localOutputStream;
    private MeasuredRequest measuredRequest;


    public MPOutputStream(OutputStream outputStream, MeasuredRequest request) {
        localOutputStream = outputStream;
        measuredRequest = request;
    }

    public final void flush() throws IOException {
        localOutputStream.flush();
    }

    public final void close() throws IOException {
        localOutputStream.close();
    }

    public final void write(int oneByte) throws IOException {
        localOutputStream.write(oneByte);
    }

    public final void write(byte[] buffer) throws IOException {
        localOutputStream.write(buffer);
    }

    public final void write(byte[] buffer, int offset, int byteCount) throws IOException {
        localOutputStream.write(buffer, offset, byteCount);
        measuredRequest.parseRequest(buffer, offset, byteCount);
    }

    public final boolean isSameStream(OutputStream paramOutputStream) {
        return this.localOutputStream == paramOutputStream;
    }
}