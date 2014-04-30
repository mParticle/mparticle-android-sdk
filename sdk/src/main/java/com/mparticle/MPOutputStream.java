package com.mparticle;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by sdozor on 3/4/14.
 *
 * This is a decorator around the given OutputStream (or OutputStream subclass).
 */
final class MPOutputStream extends OutputStream {

    private final MPInputStream inputStream;
    private OutputStream localOutputStream;
    public MeasuredRequest measuredRequest;
    private boolean writing = false;

    public MPOutputStream(OutputStream outputStream, MPInputStream inputStream) {
        localOutputStream = outputStream;
        this.inputStream  = inputStream;
    }

    @Override
    public final void flush() throws IOException {
        localOutputStream.flush();
    }

    @Override
    public final void close() throws IOException {
        localOutputStream.close();
    }

    @Override
    public final void write(int oneByte) throws IOException {
        if (measuredRequest == null || !writing || inputStream == null || inputStream.getRead()) {
            measuredRequest = new MeasuredRequest();
        }
        if (inputStream != null){
            inputStream.resetRead();
        }
        writing = true;

        localOutputStream.write(oneByte);
    }

    @Override
    public final void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    @Override
    public final void write(byte[] buffer, int offset, int byteCount) throws IOException {
        if (measuredRequest == null || !writing || inputStream == null || inputStream.getRead()) {
            measuredRequest = new MeasuredRequest();
        }
        if (inputStream != null){
            inputStream.resetRead();
            inputStream.setMeasuredRequest(measuredRequest);
        }
        writing = true;
        localOutputStream.write(buffer, offset, byteCount);
        try{
            measuredRequest.parseOutputStreamBytes(buffer, offset, byteCount);
        }catch (Exception e){
            if (MParticle.getInstance().getDebugMode()){
                Log.w(Constants.LOG_TAG, "Exception thrown while parsing networking OutputStream: " + e.getMessage());
            }
        }
    }

    public final boolean isSameStream(OutputStream paramOutputStream) {
        return this.localOutputStream == paramOutputStream;
    }

}
