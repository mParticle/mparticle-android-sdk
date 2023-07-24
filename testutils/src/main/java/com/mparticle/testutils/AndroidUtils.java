package com.mparticle.testutils;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;

import com.mparticle.internal.ApplicationContextWrapper;

/**
 * Utility methods for manipulating the Android sdk state for testing purposes.
 */
public class AndroidUtils {

    public static Context getProductionContext(final Context context) {
        return new ContextWrapper(context) {
            @Override
            public ApplicationInfo getApplicationInfo() {
                ApplicationInfo applicationInfo = new ApplicationInfo();
                applicationInfo.flags = 0;
                return applicationInfo;
            }

            @Override
            public Context getApplicationContext() {
                return new ApplicationContextWrapper((Application) context.getApplicationContext()) {
                    @Override
                    public ApplicationInfo getApplicationInfo() {
                        ApplicationInfo applicationInfo = new ApplicationInfo();
                        applicationInfo.flags = 0;
                        return applicationInfo;
                    }
                };
            }
        };
    }

    public static class Mutable<T> {
        public T value;

        public Mutable(T value) {
            this.value = value;
        }
    }


}
