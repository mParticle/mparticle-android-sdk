package com.mparticle.test;

import android.content.Context;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.embedded.EmbeddedKitManager;

/**
 * Created by sdozor on 3/23/15.
 */
public class MockMParticle extends MParticle {

    public MockMParticle(Context context, MessageManager messageManager, ConfigManager configManager, EmbeddedKitManager embeddedKitManager) {
        super(context, messageManager, configManager, embeddedKitManager);
    }

    public MockMParticle() {
        super();
    }

    static MockMParticle create(){
        EmbeddedKitManager ekManager = new EmbeddedKitManager(new MockContext());
        ConfigManager configManager = new ConfigManager(new MockContext(), ekManager, Environment.AutoDetect);
        MessageManager messageManager = new MessageManager();
        return new MockMParticle(new MockContext(), messageManager, configManager, ekManager);
    }


    @Override
    public void logEvent(MPEvent event) {
        return;
    }

    @Override
    public MParticleInternal internal() {
        return new MParticleInternal();
    }
}
