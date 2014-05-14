package com.mparticle;

/**
 * Created by sdozor on 5/13/14.
 */
public class MPUnityException extends Exception {
    String stackTrace = null;

    private MPUnityException() {
        super();
    }

    private MPUnityException(String detailMessage) {
        super(detailMessage);
    }

    private MPUnityException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    private MPUnityException(Throwable throwable) {
        super(throwable);
    }

    public MPUnityException(String detailMessage, String stackTrace) {
        super(detailMessage);
        this.stackTrace = stackTrace;
    }

    public String getManualStackTrace(){
        return stackTrace;
    }
}
