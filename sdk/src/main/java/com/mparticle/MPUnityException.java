package com.mparticle;

/**
 * @hide
 *
 * Class specifically for exceptions that occur within the Unity-framework, which
 * the mParticle Unity c# SDK will catch and log.
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
