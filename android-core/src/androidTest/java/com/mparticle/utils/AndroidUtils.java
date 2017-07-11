package com.mparticle.utils;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.ApplicationContextWrapper;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;

public class AndroidUtils {

    private static AndroidUtils sInstance;

    public static AndroidUtils getInstance() {
        if (sInstance == null) {
            sInstance = new AndroidUtils();
        }
        return sInstance;
    }

    private AndroidUtils() {}

    public void deleteDatabase() {
        InstrumentationRegistry.getTargetContext().deleteDatabase(MParticleDatabaseHelper.DB_NAME);
    }

    public Context getProductionContext(final Context context) {
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
}
