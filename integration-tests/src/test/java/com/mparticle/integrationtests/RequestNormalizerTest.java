package com.mparticle.integrationtests;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RequestNormalizerTest {

    private static JSONObject request(String url, String body) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return new JSONObject()
                .put("method", "POST")
                .put("url", url)
                .put("headers", new JSONObject()
                        .put("Content-Type", "application/json")
                        .put("Date", "Mon, 05 Oct 2026 10:00:00 GMT")
                        .put("Host", "10.0.2.2:18443"))
                .put("bodyAsBase64", Base64.getEncoder().encodeToString(bytes.toByteArray()));
    }

    private static JSONObject normalizeOne(JSONObject request) throws Exception {
        return RequestNormalizer.normalize(new JSONArray().put(request)).getJSONObject(0);
    }

    @Test
    public void gunzipsAndIgnoresValuesByPathButKeepsKeys() throws Exception {
        JSONObject body = normalizeOne(request("/v2/k/events",
                "{\"ct\":123,\"msgs\":[{\"ct\":456,\"n\":\"Event\",\"cs\":{\"bl\":0.5}}]}"))
                .getJSONObject("body");
        assertEquals(RequestNormalizer.IGNORED, body.get("ct"));
        JSONObject msg = body.getJSONArray("msgs").getJSONObject(0);
        assertEquals(RequestNormalizer.IGNORED, msg.get("ct"));
        assertEquals("Event", msg.get("n"));
        assertEquals(RequestNormalizer.IGNORED, msg.getJSONObject("cs").get("bl"));
    }

    @Test
    public void ignoreRulesAreScopedByPathNotKeyName() throws Exception {
        JSONObject body = normalizeOne(request("/v2/k/events",
                "{\"msgs\":[{\"attrs\":{\"ct\":\"kept\"}}]}")).getJSONObject("body");
        assertEquals("kept", body.getJSONArray("msgs").getJSONObject(0).getJSONObject("attrs").get("ct"));
    }

    @Test
    public void generatedIdsBecomeTokensThatPreserveEquality() throws Exception {
        JSONObject body = normalizeOne(request("/v2/k/events",
                "{\"id\":\"batch\",\"msgs\":[{\"dt\":\"ss\",\"id\":\"session\"},"
                        + "{\"dt\":\"e\",\"id\":\"event\",\"sid\":\"session\"}]}"))
                .getJSONObject("body");
        JSONArray msgs = body.getJSONArray("msgs");
        assertEquals("<id:1>", body.get("id"));
        assertEquals(msgs.getJSONObject(0).get("id"), msgs.getJSONObject(1).get("sid"));
        assertTrue(!msgs.getJSONObject(1).get("id").equals(msgs.getJSONObject(1).get("sid")));
    }

    @Test
    public void headersAreFilteredAndVolatileOnesMasked() throws Exception {
        JSONObject headers = normalizeOne(request("/v1/identify", "{}")).getJSONObject("headers");
        assertEquals("application/json", headers.get("content-type"));
        assertEquals(RequestNormalizer.IGNORED, headers.get("date"));
        assertTrue(!headers.has("host"));
    }

    @Test
    public void sdkVersionQueryParameterIsMaskedAndSorted() {
        assertEquals("/v4/k/config?av=1.0&sv=" + RequestNormalizer.IGNORED,
                RequestNormalizer.normalizeUrl("/v4/k/config?sv=6.1.5&av=1.0"));
    }

    @Test
    public void diffReportsMissingExtraChangedAndRetypedValues() throws Exception {
        JSONObject expected = new JSONObject("{\"a\":1,\"b\":\"x\",\"c\":[1,2],\"d\":1}");
        JSONObject actual = new JSONObject("{\"a\":1,\"b\":\"y\",\"c\":[1],\"d\":1.0,\"e\":true}");
        List<String> diff = BaselineDiff.diff(expected, actual);
        assertTrue(diff.toString(), diff.contains("$.b: expected \"x\" but was \"y\""));
        assertTrue(diff.toString(), diff.contains("$.c: expected 2 elements but was 1"));
        assertTrue(diff.toString(), diff.stream().anyMatch(line -> line.startsWith("$.d: expected 1")));
        assertTrue(diff.toString(), diff.contains("$.e: unexpected (was true)"));
        assertTrue(BaselineDiff.diff(expected, new JSONObject(expected.toString())).isEmpty());
    }

    @Test
    public void eventBatchOrderDoesNotMatterButCallOrderDoes() throws Exception {
        JSONObject identify = request("/v1/identify", "{\"request_id\":\"r\"}");
        JSONObject uic = request("/v2/k/events", "{\"id\":\"b2\",\"msgs\":[{\"dt\":\"uic\"}]}");
        JSONObject event = request("/v2/k/events", "{\"id\":\"b1\",\"msgs\":[{\"dt\":\"e\"}]}");
        JSONArray a = RequestNormalizer.normalize(new JSONArray().put(identify).put(uic).put(event));
        JSONArray b = RequestNormalizer.normalize(new JSONArray().put(event).put(identify).put(uic));
        assertTrue(BaselineDiff.diff(a, b).toString(), BaselineDiff.diff(a, b).isEmpty());
        assertEquals("/v1/identify", a.getJSONObject(0).get("url"));

        JSONObject logout = request("/v1/logout", "{}");
        JSONArray loginLogout = RequestNormalizer.normalize(new JSONArray().put(identify).put(logout));
        JSONArray logoutLogin = RequestNormalizer.normalize(new JSONArray().put(logout).put(identify));
        assertTrue(!BaselineDiff.diff(loginLogout, logoutLogin).isEmpty());
    }

    @Test
    public void callsOnDifferentEndpointsAreGroupedSoThreadRacesDoNotMatter() throws Exception {
        JSONObject config = request("/v4/k/config?av=1.0", "");
        JSONObject modify = request("/v1/1/modify", "{\"identity_changes\":[]}");
        JSONArray a = RequestNormalizer.normalize(new JSONArray().put(modify).put(config));
        JSONArray b = RequestNormalizer.normalize(new JSONArray().put(config).put(modify));
        assertTrue(BaselineDiff.diff(a, b).toString(), BaselineDiff.diff(a, b).isEmpty());
    }

    @Test
    public void unexpectedHeadersAreComparedNotDropped() throws Exception {
        JSONObject withExtra = request("/v1/identify", "{}");
        withExtra.getJSONObject("headers").put("X-Mp-New", "1");
        assertEquals("1", normalizeOne(withExtra).getJSONObject("headers").get("x-mp-new"));
    }

    @Test
    public void stackTracesCompareOnlyTheExceptionLine() throws Exception {
        String api28 = "{\"msgs\":[{\"st\":\"java.lang.IllegalStateException: boom\\n\\tat android.app.Instrumentation.run(Instrumentation.java:2597)\"}]}";
        String api36 = "{\"msgs\":[{\"st\":\"java.lang.IllegalStateException: boom\\n\\tat android.app.Instrumentation.run(Instrumentation.java:2453)\"}]}";
        JSONObject a = normalizeOne(request("/v2/k/events", api28));
        assertTrue(BaselineDiff.diff(a, normalizeOne(request("/v2/k/events", api36))).isEmpty());
        assertEquals("java.lang.IllegalStateException: boom\n" + RequestNormalizer.IGNORED,
                a.getJSONObject("body").getJSONArray("msgs").getJSONObject(0).get("st"));
    }

    @Test
    public void unorderedArraysCompareByMembership() throws Exception {
        String one = "{\"identity_changes\":[{\"identity_type\":\"email\"},{\"identity_type\":\"other\"}]}";
        String two = "{\"identity_changes\":[{\"identity_type\":\"other\"},{\"identity_type\":\"email\"}]}";
        assertTrue(BaselineDiff.diff(normalizeOne(request("/v1/1/modify", one)),
                normalizeOne(request("/v1/1/modify", two))).isEmpty());
    }
}
