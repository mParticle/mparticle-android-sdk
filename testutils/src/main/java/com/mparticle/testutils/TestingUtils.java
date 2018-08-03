package com.mparticle.testutils;

import android.content.Context;

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

public class TestingUtils {
    private RandomUtils mRandom = RandomUtils.getInstance();
    private static TestingUtils instance;

    public static TestingUtils getInstance() {
        if (instance == null) {
            instance = new TestingUtils();
        }
        return instance;
    }

    public static void setStrictMode(MParticle.LogLevel logLevel) {
        setStrictMode(logLevel, "");
    }

    public static void setStrictMode(final MParticle.LogLevel logLevelLimit, final String... exclusions) {
        Logger.DefaultLogHandler logListener;
        if (logLevelLimit != null) {
            logListener = new Logger.DefaultLogHandler() {

                @Override
                public void log(MParticle.LogLevel logLevel, Throwable error, String messages) {
                    super.log(logLevel, error, messages);
                    if (logLevel.ordinal() <= logLevelLimit.ordinal()) {
                        for (String exclude: exclusions) {
                            if (messages.equals(exclude)) {
                                return;
                            }
                            throw new RuntimeException(String.format("Unacceptable Log of level \"%s\" : \n\"%s\" ", logLevel.name(), messages));
                        }
                    }
                }
            };
        } else {
            logListener = null;
        }
        Logger.setLogHandler(logListener);
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
