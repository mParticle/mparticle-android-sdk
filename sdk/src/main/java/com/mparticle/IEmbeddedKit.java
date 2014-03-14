package com.mparticle;

import android.app.Activity;

/**
 * Created by sdozor on 3/14/14.
 */
public interface IEmbeddedKit {
    void logEvent() throws Exception;
    void onCreate(Activity activity) throws Exception;
    void onResume(Activity activity) throws Exception;
    void logTransaction(MPTransaction transaction) throws Exception;
}
