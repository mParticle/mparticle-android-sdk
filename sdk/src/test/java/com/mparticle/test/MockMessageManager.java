package com.mparticle.test;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.ConfigManager;
import com.mparticle.internal.MessageManager;

/**
 * Created by sdozor on 3/23/15.
 */
public class MockMessageManager extends MessageManager{
    public MockMessageManager(Context appContext, ConfigManager configManager, MParticle.InstallType installType) {
        super(appContext, configManager, installType);
    }
}
