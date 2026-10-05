package com.mparticle.integrationtests;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Strict structural diff: every missing, extra or changed value is reported with its path. */
public final class BaselineDiff {
    private BaselineDiff() {
    }

    public static List<String> diff(Object expected, Object actual) throws JSONException {
        List<String> out = new ArrayList<>();
        diff("$", expected, actual, out);
        return out;
    }

    private static void diff(String path, Object expected, Object actual, List<String> out) throws JSONException {
        if (expected instanceof JSONObject && actual instanceof JSONObject) {
            JSONObject e = (JSONObject) expected;
            JSONObject a = (JSONObject) actual;
            Set<String> keys = new TreeSet<>();
            for (java.util.Iterator<String> it = e.keys(); it.hasNext(); ) keys.add(it.next());
            for (java.util.Iterator<String> it = a.keys(); it.hasNext(); ) keys.add(it.next());
            for (String key : keys) {
                String child = path + "." + key;
                if (!a.has(key)) {
                    out.add(child + ": missing (expected " + e.get(key) + ")");
                } else if (!e.has(key)) {
                    out.add(child + ": unexpected (was " + a.get(key) + ")");
                } else {
                    diff(child, e.get(key), a.get(key), out);
                }
            }
        } else if (expected instanceof JSONArray && actual instanceof JSONArray) {
            JSONArray e = (JSONArray) expected;
            JSONArray a = (JSONArray) actual;
            if (e.length() != a.length()) {
                out.add(path + ": expected " + e.length() + " elements but was " + a.length());
            }
            for (int i = 0; i < Math.min(e.length(), a.length()); i++) {
                diff(path + "[" + i + "]", e.get(i), a.get(i), out);
            }
        } else if (!sameScalar(expected, actual)) {
            out.add(path + ": expected " + describe(expected) + " but was " + describe(actual));
        }
    }

    // org.json parses 1 as Integer and 1.0 as Double; a migration that changes 1 to 1.0 is a
    // real payload change, so compare the type as well as the value.
    private static boolean sameScalar(Object expected, Object actual) {
        if (expected == null || actual == null) return expected == actual;
        if (JSONObject.NULL.equals(expected) || JSONObject.NULL.equals(actual)) {
            return JSONObject.NULL.equals(expected) && JSONObject.NULL.equals(actual);
        }
        if (expected instanceof Number && actual instanceof Number) {
            boolean expectedIntegral = isIntegral(expected);
            boolean actualIntegral = isIntegral(actual);
            if (expectedIntegral != actualIntegral) return false;
            return expectedIntegral
                    ? ((Number) expected).longValue() == ((Number) actual).longValue()
                    : ((Number) expected).doubleValue() == ((Number) actual).doubleValue();
        }
        return expected.getClass() == actual.getClass() && expected.equals(actual);
    }

    private static boolean isIntegral(Object number) {
        return number instanceof Integer || number instanceof Long || number instanceof java.math.BigInteger;
    }

    private static String describe(Object value) {
        if (value instanceof String) return "\"" + value + "\"";
        return value + (value == null || JSONObject.NULL.equals(value) ? "" : " (" + value.getClass().getSimpleName() + ")");
    }
}
