package com.mparticle.utils;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.ApplicationContextWrapper;
import com.mparticle.internal.DatabaseTables;
import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;

/**
 * Utility methods for maniulating the Android sdk state for testing purposes
 */
public class AndroidUtils {

    private static AndroidUtils sInstance;

    public static AndroidUtils getInstance() {
        if (sInstance == null) {
            sInstance = new AndroidUtils();
        }
        return sInstance;
    }

    private AndroidUtils() {}

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
