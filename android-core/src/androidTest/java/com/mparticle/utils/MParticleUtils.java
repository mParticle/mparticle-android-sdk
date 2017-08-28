package com.mparticle.utils;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MParticleApiClientImpl;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MParticleUtils {
    private RandomUtils mRandom = RandomUtils.getInstance();
    private static MParticleUtils instance;

    public static MParticleUtils getInstance() {
        if (instance == null) {
            instance = new MParticleUtils();
        }
        return instance;
    }

    public static void awaitStoreMessage() throws InterruptedException {
        Handler messageHandler = AccessUtils.getMessageHandler();
        while(messageHandler.hasMessages(AccessUtils.STORE_MESSAGE)) {
            Thread.sleep(500);
            //do nothing, just block
        }
        return;
    }

    public static void awaitSetUserAttribute() throws InterruptedException {
        Handler messageHandler = AccessUtils.getMessageHandler();
        while (messageHandler.hasMessages(AccessUtils.SET_USER_ATTRIBUTE)) {
            Thread.sleep(500);
            //do nothing, just block
        }
        return;
    }

    public static void awaitRemoveUserAttribute() throws InterruptedException {
        Handler messageHandler = AccessUtils.getMessageHandler();
        while (messageHandler.hasMessages(AccessUtils.REMOVE_USER_ATTRIBUTE)) {
            Thread.sleep(500);
            //do nothing, just block
        }
        return;
    }

    public static void awaitUploadRunnables() throws InterruptedException {
        Handler uploadHandler = AccessUtils.getUploadHandler();
        do {
            Thread.sleep(1000);
        }
        while (uploadHandler.hasMessages(0));
        return;
    }

    public static void clear() {
        Context context = InstrumentationRegistry.getContext();
        MParticle.setInstance(null);
        AccessUtils.deleteDatabase();
        AccessUtils.deleteConfigManager(InstrumentationRegistry.getContext());
        context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE).edit().clear().commit();
    }

    /**
     * This method will block the current thread until Upload messages, which are tied to the mpid parameter,
     * or are UploadTriggerMessages, are cleared from the Handler's queue
     *
     * Upload essages which are tied to an MPID, are ones originating from MParticle.getInstance().upload() calls,
     * and initial upload messages
     *
     * the fact that these messages are tied into an MPID is an artifact from a defunct implementation
     * of the UploadHandler, but it is really useful for this use case,
     * @param mpid
     * @throws InterruptedException
     */
    public static void awaitUploadMessages(long mpid) throws InterruptedException {
        Handler uploadHandler = AccessUtils.getUploadHandler();
        do {
            Thread.sleep(200);
            //do nothing, just block
        }
        while (uploadHandler.hasMessages(AccessUtils.UPLOAD_MESSAGES, mpid) || uploadHandler.hasMessages(AccessUtils.UPLOAD_TRIGGER_MESSAGES));
        Thread.sleep(100);
        return;
    }


    private MPEvent.Builder getRandomMPEventBuilder() {
        return new MPEvent.Builder(mRandom.getAlphaNumericString(mRandom.randomInt(1,55)), MParticle.EventType.values()[mRandom.randomInt(0, MParticle.EventType.values().length - 1)]);
    }

    public MPEvent getRandomMPEventSimple() {
        return getRandomMPEventBuilder().build();
    }

    public MPEvent getRandomMPEventRich() {
        MPEvent.Builder builder = getRandomMPEventBuilder();
        Random random = new Random();
        if (random.nextBoolean()) {
            builder.category(mRandom.getAlphaNumericString(mRandom.randomInt(5, 55)));
        }
        if (random.nextBoolean()) {
            builder.duration(mRandom.randomLong(1000, 1000 * 100));
        }
        if (random.nextBoolean()) {
            builder.startTime();
        }
        if (random.nextBoolean()) {
            builder.endTime();
        }
        if (random.nextBoolean()) {
            Map<String, String> infoMap = new HashMap<String, String>();
            for (int i = 0; i < mRandom.randomInt(-5, 20); i++) {
                String key = mRandom.getAlphaNumericString(mRandom.randomInt(0, 55));
                String value = mRandom.getAlphaNumericString(mRandom.randomInt(0, 55));
                // 50/50 chance, that if the string is shorter than 5 characters, we will replace it with "null"
                infoMap.put(key.length() <= 5 && random.nextBoolean() ? null : key, value.length() < 5 && random.nextBoolean() ? null : value);
            }
            //put in a null map sometimes
            if (infoMap.isEmpty() && random.nextBoolean()) {
                infoMap = null;
            }
            builder.info(infoMap);
        }
        if (random.nextBoolean()) {
            for (int i = 0; i < mRandom.randomInt(0, 10); i++) {
                String key = mRandom.getAlphaNumericString(mRandom.randomInt(0, 55));
                String value = mRandom.getAlphaNumericString(mRandom.randomInt(0, 55));
                // 50/50 chance, that if the string is shorter than 5 characters, we will replace it with "null"
                builder.addCustomFlag(key.length() <= 5 && random.nextBoolean() ? null : key, value.length() < 5 && random.nextBoolean() ? null : value);
            }
        }
        if (random.nextBoolean()) {
            builder.internalNavigationDirection(random.nextBoolean());
        }
        return builder.build();
    }

    public void setDefaultClient(Context context) {
        try {
            AccessUtils.setMParticleApiClient(new MParticleApiClientImpl(MParticle.getInstance().getConfigManager(), context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE), context));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (MParticleApiClientImpl.MPNoConfigException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultIdentityClient(Context context) {
        com.mparticle.identity.AccessUtils.setDefaultIdentityApiClient(context);
    }

}
