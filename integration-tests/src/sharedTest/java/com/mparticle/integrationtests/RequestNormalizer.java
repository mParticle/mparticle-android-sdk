package com.mparticle.integrationtests;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 * Turns WireMock's request journal into a stable, diffable form. Values that legitimately change
 * between runs are replaced by {@link #IGNORED}, but their keys are kept, so a field that appears,
 * disappears or changes type is still a diff.
 */
public final class RequestNormalizer {
    public static final String IGNORED = "<ignored>";

    /** Headers that depend on the transport, not the SDK: the WireMock port, the gzipped length. */
    private static final Set<String> DROPPED_HEADERS = new HashSet<>(Arrays.asList(
            "host", "content-length"));

    /** Headers that must be present but whose value varies per run or per device. */
    private static final Set<String> MASKED_HEADERS = new HashSet<>(Arrays.asList(
            "x-mp-signature", "date", "user-agent", "if-modified-since"));

    /** Query parameters whose value varies (the SDK version changes on every release). */
    private static final Set<String> IGNORED_QUERY = new HashSet<>(Arrays.asList("sv"));

    /**
     * Body paths whose value varies per run or per device. {@code *} matches any one key or array
     * index. Scoped by path, not by key name, so an {@code id} that is stable somewhere else is
     * still compared there.
     */
    static final List<String[]> IGNORED_PATHS = paths(
            // identity requests
            "request_timestamp_ms", "client_sdk.sdk_version",
            // batch envelope
            "ct", "sdk", "ai.ict", "ai.ud", "di.*", "ui.*.dfs",
            // messages
            "msgs.*.ct", "msgs.*.sct", "msgs.*.est", "msgs.*.dct", "msgs.*.cs.*",
            "msgs.*.ni.dfs", "msgs.*.oi.dfs", "msgs.*.pd.pl.*.act", "msgs.*.pi.*.pl.*.act",
            // session length in seconds, rounds to 0 or 1 depending on timing
            "msgs.*.sl", "msgs.*.slx",
            // breadcrumbs are whole messages embedded in an error message
            "msgs.*.bc.*.ct", "msgs.*.bc.*.sct", "msgs.*.bc.*.est", "msgs.*.bc.*.cs.*"
    );

    /**
     * Body paths holding generated identifiers. Each distinct value becomes {@code <id:N>} in
     * order of first appearance, so the baseline still asserts which values are equal: every
     * message's {@code sid} must be the session-start {@code id}, {@code das} must match the
     * identity request's device_application_stamp, and so on.
     */
    static final List<String[]> ID_PATHS = paths(
            "request_id", "known_identities.device_application_stamp", "data.device_application_stamp",
            "id", "das", "msgs.*.id", "msgs.*.sid", "msgs.*.bc.*.id", "msgs.*.bc.*.sid"
    );

    private RequestNormalizer() {
    }

    private static List<String[]> paths(String... patterns) {
        List<String[]> out = new ArrayList<>();
        for (String pattern : patterns) out.add(pattern.split("\\."));
        return out;
    }

    /**
     * Stack traces: only the first line (exception class and message) is compared. The frames
     * below it run through the test runner and the OS, whose line numbers differ by API level.
     */
    static final List<String[]> STACK_TRACE_PATHS = paths("msgs.*.st");

    /**
     * Arrays the SDK builds from unordered collections. Their elements are sorted, so only
     * membership is compared.
     */
    static final List<String[]> UNORDERED_PATHS = paths("identity_changes");

    private static final String MASKED_ID = "<id>";

    /**
     * Normalizes the journal (oldest first). Requests are grouped by endpoint (config, identity,
     * alias, audience, event batches) because calls on different endpoints run on different
     * threads and reach the server in no fixed order. Identity calls keep the order they were
     * made in. Event batches are put in a canonical order: the SDK splits messages into one batch
     * per session and MPID, and the order those batches are sent in is not a contract.
     */
    public static JSONArray normalize(JSONArray journal) throws JSONException, IOException {
        List<List<JSONObject>> groups = new ArrayList<>();
        for (int i = 0; i <= EVENTS; i++) groups.add(new ArrayList<>());
        for (int i = 0; i < journal.length(); i++) {
            JSONObject request = journal.getJSONObject(i);
            groups.get(group(request)).add(request);
        }
        List<Object[]> keyed = new ArrayList<>();
        for (JSONObject batch : groups.get(EVENTS)) {
            keyed.add(new Object[]{canonical(normalizeRequest(batch, null)), batch});
        }
        keyed.sort((a, b) -> ((String) a[0]).compareTo((String) b[0]));
        groups.get(EVENTS).clear();
        for (Object[] entry : keyed) groups.get(EVENTS).add((JSONObject) entry[1]);

        Map<String, String> ids = new java.util.HashMap<>();
        JSONArray out = new JSONArray();
        for (List<JSONObject> group : groups) {
            for (JSONObject request : group) out.put(normalizeRequest(request, ids));
        }
        return out;
    }

    private static final int CONFIG = 0, IDENTITY = 1, ALIAS = 2, AUDIENCE = 3, OTHER = 4, EVENTS = 5;

    private static int group(JSONObject request) throws JSONException {
        String path = request.getString("url").split("\\?")[0];
        if (path.matches("/v4/[^/]+/config")) return CONFIG;
        if (path.matches("/v1/(identify|login|logout|[0-9]+/modify)")) return IDENTITY;
        if (path.matches("/v1/identity/[^/]+/alias")) return ALIAS;
        if (path.matches("/v1/[^/]+/audience")) return AUDIENCE;
        if (path.matches("/v2/[^/]+/events")) return EVENTS;
        return OTHER;
    }

    /** With {@code ids == null}, generated IDs are masked rather than numbered (for sorting). */
    private static JSONObject normalizeRequest(JSONObject request, Map<String, String> ids)
            throws JSONException, IOException {
        JSONObject out = new JSONObject();
        out.put("method", request.getString("method"));
        out.put("url", normalizeUrl(request.getString("url")));
        out.put("headers", normalizeHeaders(request.optJSONObject("headers")));
        String body = decodeBody(request);
        if (!body.isEmpty()) {
            Object parsed = parse(body);
            out.put("body", parsed instanceof String ? parsed : normalizeValue(parsed, new ArrayList<>(), ids));
        }
        return out;
    }

    static String normalizeUrl(String url) {
        int q = url.indexOf('?');
        if (q < 0) return url;
        Map<String, String> params = new TreeMap<>();
        for (String pair : url.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            params.put(key, IGNORED_QUERY.contains(key) ? IGNORED : value);
        }
        StringBuilder sb = new StringBuilder(url.substring(0, q)).append('?');
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static JSONObject normalizeHeaders(JSONObject headers) throws JSONException {
        Map<String, Object> sorted = new TreeMap<>();
        if (headers != null) {
            for (Iterator<String> it = headers.keys(); it.hasNext(); ) {
                String name = it.next();
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (MASKED_HEADERS.contains(lower)) {
                    sorted.put(lower, IGNORED);
                } else if (!DROPPED_HEADERS.contains(lower)) {
                    sorted.put(lower, headers.get(name));
                }
            }
        }
        return new JSONObject(sorted);
    }

    /** WireMock keeps the raw bytes in bodyAsBase64; the SDK gzips event batches. */
    private static String decodeBody(JSONObject request) throws IOException {
        String base64 = request.optString("bodyAsBase64", "");
        if (base64.isEmpty()) return request.optString("body", "");
        byte[] bytes = Base64.getDecoder().decode(base64);
        if (bytes.length > 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b) {
            bytes = readAll(new GZIPInputStream(new ByteArrayInputStream(bytes)));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static Object parse(String body) {
        try {
            return new JSONTokener(body).nextValue();
        } catch (JSONException notJson) {
            return body;
        }
    }

    private static Object normalizeValue(Object value, List<String> path, Map<String, String> ids)
            throws JSONException {
        if (matches(IGNORED_PATHS, path)) return IGNORED;
        if (matches(STACK_TRACE_PATHS, path) && value instanceof String) {
            String trace = (String) value;
            int newline = trace.indexOf('\n');
            return newline < 0 ? trace : trace.substring(0, newline) + "\n" + IGNORED;
        }
        if (matches(ID_PATHS, path) && value instanceof String) {
            if (ids == null) return MASKED_ID;
            String token = ids.get(value);
            if (token == null) {
                token = "<id:" + (ids.size() + 1) + ">";
                ids.put((String) value, token);
            }
            return token;
        }
        if (value instanceof JSONObject) {
            JSONObject in = (JSONObject) value;
            Set<String> keys = new java.util.TreeSet<>();
            for (Iterator<String> it = in.keys(); it.hasNext(); ) keys.add(it.next());
            // Sorted traversal keeps <id:N> numbering independent of the parser's key order.
            Map<String, Object> sorted = new TreeMap<>();
            for (String key : keys) {
                path.add(key);
                sorted.put(key, normalizeValue(in.get(key), path, ids));
                path.remove(path.size() - 1);
            }
            return new JSONObject(sorted);
        }
        if (value instanceof JSONArray) {
            JSONArray in = (JSONArray) value;
            List<Object> elements = new ArrayList<>();
            for (int i = 0; i < in.length(); i++) elements.add(in.get(i));
            if (matches(UNORDERED_PATHS, path)) {
                elements.sort((a, b) -> canonical(a).compareTo(canonical(b)));
            }
            JSONArray out = new JSONArray();
            for (int i = 0; i < elements.size(); i++) {
                path.add(Integer.toString(i));
                out.put(normalizeValue(elements.get(i), path, ids));
                path.remove(path.size() - 1);
            }
            return out;
        }
        return value;
    }

    private static boolean matches(List<String[]> patterns, List<String> path) {
        for (String[] pattern : patterns) {
            if (pattern.length != path.size()) continue;
            boolean match = true;
            for (int i = 0; i < pattern.length && match; i++) {
                match = pattern[i].equals("*") || pattern[i].equals(path.get(i));
            }
            if (match) return true;
        }
        return false;
    }

    /** JSON text with sorted keys; org.json's own toString order differs between Android and the JVM. */
    static String canonical(Object value) {
        if (value instanceof JSONObject) {
            JSONObject in = (JSONObject) value;
            Set<String> keys = new java.util.TreeSet<>();
            for (Iterator<String> it = in.keys(); it.hasNext(); ) keys.add(it.next());
            StringBuilder sb = new StringBuilder("{");
            for (String key : keys) {
                if (sb.length() > 1) sb.append(',');
                sb.append(JSONObject.quote(key)).append(':').append(canonical(in.opt(key)));
            }
            return sb.append('}').toString();
        }
        if (value instanceof JSONArray) {
            JSONArray in = (JSONArray) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < in.length(); i++) {
                if (i > 0) sb.append(',');
                sb.append(canonical(in.opt(i)));
            }
            return sb.append(']').toString();
        }
        return value instanceof String ? JSONObject.quote((String) value) : String.valueOf(value);
    }

    static byte[] readAll(InputStream in) throws IOException {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toByteArray();
        }
    }
}
