package com.mparticle.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.firebase.iid.FirebaseInstanceId;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.identity.AliasRequest;
import com.mparticle.internal.JsonReportingMessage;
import com.mparticle.internal.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class TestingUtils {
    private RandomUtils randomUtils = new RandomUtils();
    Random random = new Random();
    private static TestingUtils instance;
    private static boolean firebasePresent = false;

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
                        for (String exclude : exclusions) {
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

    public static void setFirebasePresent(boolean present, String token) {
        firebasePresent = present;
        FirebaseInstanceId.setToken(token);
    }

    public static boolean isFirebasePresent() {
        return firebasePresent;
    }


    private MPEvent.Builder getRandomMPEventBuilder() {
        return new MPEvent.Builder(randomUtils.getAlphaNumericString(randomUtils.randomInt(1, 5)), MParticle.EventType.values()[randomUtils.randomInt(0, MParticle.EventType.values().length - 1)]);
    }

    public MPEvent getRandomMPEventSimple() {
        return getRandomMPEventBuilder().build();
    }

    public MPEvent getRandomMPEventRich() {
        MPEvent.Builder builder = getRandomMPEventBuilder();
        if (random.nextBoolean()) {
            builder.category(randomUtils.getAlphaNumericString(randomUtils.randomInt(5, 55)));
        }
        if (random.nextBoolean()) {
            builder.duration(randomUtils.randomLong(1000l, 1000l * 100));
        }
        if (random.nextBoolean()) {
            builder.startTime();
        }
        if (random.nextBoolean()) {
            builder.endTime();
        }
        if (random.nextBoolean()) {
            Map<String, String> infoMap = new HashMap<String, String>();
            for (int i = 0; i < randomUtils.randomInt(-5, 20); i++) {
                String key = randomUtils.getAlphaNumericString(randomUtils.randomInt(0, 55));
                String value = randomUtils.getAlphaNumericString(randomUtils.randomInt(0, 55));
                // 50/50 chance, that if the string is shorter than 5 characters, we will replace it with "null"
                infoMap.put(key.length() <= 5 && random.nextBoolean() ? null : key, value.length() < 5 && random.nextBoolean() ? null : value);
            }
            //put in a null map sometimes
            if (infoMap.isEmpty() && random.nextBoolean()) {
                infoMap = null;
            }
            builder.customAttributes(infoMap);
        }
        if (random.nextBoolean()) {
            for (int i = 0; i < randomUtils.randomInt(0, 10); i++) {
                String key = randomUtils.getAlphaNumericString(randomUtils.randomInt(0, 55));
                String value = randomUtils.getAlphaNumericString(randomUtils.randomInt(0, 55));
                // 50/50 chance, that if the string is shorter than 5 characters, we will replace it with "null"
                builder.addCustomFlag(key.length() <= 5 && random.nextBoolean() ? null : key, value.length() < 5 && random.nextBoolean() ? null : value);
            }
        }
        if (random.nextBoolean()) {
            builder.internalNavigationDirection(random.nextBoolean());
        }
        return builder.build();
    }

    public AliasRequest getRandomAliasRequest() {
        final Long destinationMpid = random.nextLong();
        final Long sourceMpid = random.nextLong();
        final Long startTime = randomUtils.randomLong(0, System.currentTimeMillis());
        final Long endTime = randomUtils.randomLong(startTime, System.currentTimeMillis());
        return AliasRequest.builder()
                .destinationMpid(destinationMpid)
                .sourceMpid(sourceMpid)
                .sourceMpid(startTime)
                .endTime(endTime)
                .build();
    }

    public JsonReportingMessage getRandomReportingMessage(final String sessionId) {
        final Random ran = new Random();
        return new JsonReportingMessage() {
            int randomNumber;

            @Override
            public void setDevMode(boolean development) {
                //do nothing
            }

            @Override
            public long getTimestamp() {
                return System.currentTimeMillis() - 100;
            }

            @Override
            public int getModuleId() {
                return 1;//MParticle.ServiceProviders.APPBOY;
            }

            @Override
            public JSONObject toJson() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("fieldOne", "a value");
                    jsonObject.put("fieldTwo", "another value");
                    jsonObject.put("a random Number", randomNumber == -1 ? randomNumber = ran.nextInt() : randomNumber);
                } catch (JSONException ignore) {
                }
                return jsonObject;
            }

            @Override
            public String getSessionId() {
                return sessionId;
            }

            @Override
            public void setSessionId(String sessionId) {

            }
        };
    }

    public static void assertJsonEqual(JSONObject object1, JSONObject object2) {
        if (object1 == object2) {
            return;
        }
        try {
            object1 = new JSONObject(object1.toString());
            object2 = new JSONObject(object2.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        assertEquals(object1.length(), object2.length());
        Iterator<String> keys = object1.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object obj1Val = object1.get(key);
                Object obj2Val = object2.get(key);
                //dealing with nested JSONObjects, not going to deal with nested JSONArray's
                if (obj1Val instanceof JSONObject && obj2Val instanceof JSONObject) {
                    assertJsonEqual((JSONObject) obj1Val, (JSONObject) obj2Val);
                } else if (obj1Val instanceof JSONArray && obj2Val instanceof JSONArray) {
                    assertJsonArrayEqual((JSONArray) obj1Val, (JSONArray) obj2Val);
                } else {
                    assertEquals(obj1Val, obj2Val);
                }
            } catch (JSONException jse) {
                fail(jse.getMessage());
            }
        }
    }

    // This method does NOT account for repeated elements in the JSONArray.
    // We don't need to for our current use case, but keep this in mind if the
    // method is going to be ported for a more general use case
    public static void assertJsonArrayEqual(JSONArray jsonArray1, JSONArray jsonArray2) {
        if (jsonArray1 == jsonArray2) {
            return;
        }
        assertEquals(jsonArray1.length(), jsonArray2.length());
        JSONObject jsonObject1 = new JSONObject();
        JSONObject jsonObject2 = new JSONObject();
        for (int i = 0; i < jsonArray1.length(); i++) {
            Object object1 = jsonArray1.opt(i);
            Object object2 = jsonArray2.opt(i);
            try {
                jsonObject1.put(object1 == null ? null : object1.toString(), object1);
            } catch (JSONException jse) {
                fail(jse.getMessage());
            }
            try {
                jsonObject2.put(object2 == null ? null : object2.toString(), object2);
            } catch (JSONException jse) {
                fail(jse.getMessage());
            }
        }
        assertJsonEqual(jsonObject1, jsonObject2);
    }
}
