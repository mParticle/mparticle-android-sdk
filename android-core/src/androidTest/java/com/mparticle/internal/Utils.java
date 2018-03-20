package com.mparticle.internal;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;

import com.mparticle.AccessUtils;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class Utils {
    private static final String sAlpha = "abcdefghijklmnopqrstuvwxyzABC ,.";
    private static final String sNumbers = "0123456789";

    private static Utils instance;

    public static Utils getInstance() {
        if  (instance == null) {
            instance = new Utils();
        }
        return instance;
    }

    public Map<MParticle.IdentityType, String> getRandomUserIdentities() {
        Map<MParticle.IdentityType, String> randomIdentities = new HashMap<MParticle.IdentityType, String>();

        int identityTypeLength = MParticle.IdentityType.values().length;
        int numIdentities = randomInt(1, identityTypeLength);
        Set<Integer> identityIndices = randomIntSet(0, identityTypeLength, numIdentities);
        for (Integer identityIndex: identityIndices) {
            randomIdentities.put(MParticle.IdentityType.values()[identityIndex], getAlphaNumericString(randomInt(1, 55)));
        }
        randomIdentities.remove(MParticle.IdentityType.Alias);
        return randomIdentities;
    }

    public Map<String, String> getRandomAttributes(int count) {
        Map<String, String> attributes = new HashMap<String, String>();
        for (int i = 0; i < count; i++) {
            String key = "";
            String value = "";
            if (randomInt(0, 5) == 0) {
                key = null;
            } else {
                key = getAlphaNumericString(20);
            }
            if (randomInt(0, 5) == 0) {
                value = null;
            } else {
                value = getAlphaNumericString(20);
            }
            attributes.put(key, value);
        }
        return attributes;
    }

    private MPEvent.Builder getRandomMPEventBuilder() {
        return new MPEvent.Builder(randomInt(0,10) % 9 == 0 ? null : getAlphaNumericString(randomInt(1,55)), MParticle.EventType.values()[randomInt(0, MParticle.EventType.values().length - 1)]);
    }

    public MPEvent getRandomMPEventRich() {
        MPEvent.Builder builder = getRandomMPEventBuilder();
        Random random = new Random();
        if (random.nextBoolean()) {
            builder.category(getAlphaNumericString(randomInt(5, 55)));
        }
        if (random.nextBoolean()) {
            builder.duration(randomLong(1000, 1000 * 100));
        }
        if (random.nextBoolean()) {
            builder.startTime();
        }
        if (random.nextBoolean()) {
            builder.endTime();
        }
        Map<String, String> infoMap = new HashMap<String, String>();
        for (int i = 0; i < randomInt(-2, 20); i++) {
            String key = getAlphaNumericString(randomInt(0, 55));
            String value = getAlphaNumericString(randomInt(0, 55));
            // 50/50 chance, that if the string is shorter than 5 characters, we will replace it with "null"
            infoMap.put(key.length() <= 5 && random.nextBoolean() ? null : key, value.length() < 5 && random.nextBoolean() ? null : value);
        }
        //put in a null map sometimes
        if (infoMap.isEmpty() && random.nextBoolean()) {
            infoMap = null;
        }
        builder.info(infoMap);
        for (int i = 0; i < randomInt(-2, 10); i++) {
            String key = getAlphaNumericString(randomInt(0, 55));
            String value = getAlphaNumericString(randomInt(0, 55));
            // 50/50 chance, that if the string is shorter than 5 characters, we will replace it with "null"
            builder.addCustomFlag(key.length() <= 5 && random.nextBoolean() ? null : key, value.length() < 5 && random.nextBoolean() ? null : value);
        }
        if (random.nextBoolean()) {
            builder.internalNavigationDirection(random.nextBoolean());
        }
        return builder.build();
    }

    public String getAlphaNumericString(int length) {
        String characters = getAlphNumeric();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++){
            builder.append(characters.charAt(randomInt(0, characters.length() - 1)));
        }
        return builder.toString();
    }

    //inclusive of low range, less than high range
    public int randomInt(int from, int to) {
        int random = Math.abs(new Random().nextInt());
        int range = random % (to - from);
        return (range) + from;
    }

    public Set<Integer> randomIntSet(int fromRange, int toRange, int num) {
        if (toRange < fromRange) {
            throw new IllegalArgumentException("toRange must be greater than fromRange");
        }
        if (toRange - fromRange < num) {
            throw new IllegalArgumentException("range must be grater than num, since a Set may only contain one instance of an Entry, you will be unable to fill the Set with these parameters");
        }
        Set<Integer> set = new TreeSet<Integer>();
        for (int i = 0; i < num; i++) {
            int randomInt = randomInt(fromRange, toRange);
            if (set.contains(randomInt)) {
                i--;
            } else {
                set.add(randomInt);
            }
        }
        return set;
    }

    private String getAlphNumeric() {
        return sAlpha + sNumbers;
    }

    public long randomLong(long from, long to) {
        long random = Math.abs(new Random().nextLong());
        long range = random % (to - from);
        return range + from;
    }


    public static void awaitStoreMessage() throws InterruptedException {
        Handler messageHandler = AccessUtils.getMessageManager().mMessageHandler;
        while(messageHandler.hasMessages(MessageHandler.STORE_MESSAGE)) {
            Thread.sleep(500);
            //do nothing, just block
        }
        return;
    }

    public static void clear() {
        Context context = InstrumentationRegistry.getContext();
        MParticle.setInstance(null);
        context.deleteDatabase(MParticleDatabase.DB_NAME);
        com.mparticle.internal.AccessUtils.deleteConfigManager(context);
        context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE).edit().clear().commit();
    }

    static List<JSONObject> getTableEntries(String tableName) throws JSONException {
        List<JSONObject> entries = new ArrayList<JSONObject>();
        Cursor cursor = null;
        try {
            cursor = AccessUtils.getMessageManager().mUploadHandler.mDbHelper.getReadableDatabase()
                    .query(tableName, null, null, null, null, null, null);
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                JSONObject jsonObject = new JSONObject();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    jsonObject.put(cursor.getColumnName(i), cursor.getString(i));
                }
                entries.add(jsonObject);
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return entries;
    }

    public static void checkAllBool(boolean[] array, int everyNDeciseconds, int forDeciseconds) {
        for (int i = 0; i < forDeciseconds + 2; i++) {
            if (i % everyNDeciseconds == 0) {
                boolean allTrue = true;
                for (Boolean bool : array) {
                    if (!bool) {
                        allTrue = false;
                    }
                }
                if (allTrue) {
                    return;
                }
            }
            pause(everyNDeciseconds);
        }
        for (int i = 0; i < array.length; i++) {
            if (!array[i]) {
                fail("failed to satify condition index: " + i);
            }
        }
    }

    private static void pause(int seconds) {
        try {
            Thread.sleep(seconds * 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
