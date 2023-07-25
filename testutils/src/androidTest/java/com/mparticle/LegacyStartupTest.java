package com.mparticle;

import android.Manifest;
import android.content.Context;

import androidx.test.rule.GrantPermissionRule;

import com.mparticle.internal.Logger;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;

import java.lang.reflect.Method;

@LegacyOnly
public class LegacyStartupTest extends BaseStartupTest {

    Method mParticleStart = null;

    @Rule
    public GrantPermissionRule mWritePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule mReadPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);


    @Before
    public void before() throws InterruptedException {
        try {
            mParticleStart = MParticle.class.getMethod("start", Context.class, String.class, String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Assume.assumeNotNull(mParticleStart);
    }

    @After
    public void after() {
        try {
            Logger.debug("Startup times = " + readFile());
        } catch (Exception ex) {
        }
    }

    @Override
    protected String fileName() {
        return LEGACY_FILE_NAME;
    }

    @Override
    protected void startup() throws Exception {
        mParticleStart.invoke(null, mContext, "key", "secret");
    }
}
