package com.mparticle.internal;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mparticle.MParticle;
import com.mparticle.WebViewActivity;
import com.mparticle.identity.AccessUtils;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleJSInterface;
import com.mparticle.test.R;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.TestingUtils;

import org.eclipse.jetty.server.HttpConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class MParticleJSInterfaceTest extends BaseCleanStartedEachTest {

    @Rule
    public ActivityTestRule<WebViewActivity> rule = new ActivityTestRule<WebViewActivity>(WebViewActivity.class);

    private static String jsSdk;
    private static boolean sdkFetchedSuccessfully = false;

    private static final String jsStartupMParticle = "window.mParticle = {\n" +
            "            config: {\n" +
            "                isDevelopmentMode: true,\n" +
            "                useCookieStorage: true,\n" +
            "                identifyRequest: {\n" +
            "                userIdentities: { email: 'email@example.com', customerid: '123456' }\n" +
            "               } " +
            "            }\n" +
            "        };" +
            "    window.mParticle = window.mParticle || {};\n" +
            "    window.mParticle.config = window.mParticle.config || {};\n" +
            "    window.mParticle.config.rq = [];\n" +
            "    window.mParticle.ready = function (f) {\n" +
            "        window.mParticle.config.rq.push(f);\n" +
            "        console.log(\"pushed f\");\n" +
            "    };\n";

    private static final String jsTestWrapper =
            "   mParticle.init();\n" +
                    "   mParticle.useNativeSdk = true;\n" +
                    "   mParticle.isDebug = true;\n" +
                    " console.log(\"testing started\")\n " +
                    "       window.mParticle.ready(function () {\n" +
                    "       console.log(\"mparticle started in JS land\");\n" +
                    "%s\n" +
                    "      })\n";

    private static final String jsSetMpidFunction = "function getCookieDomain() {\n" +
            "    var rootDomain = getDomain(document, location.hostname);\n" +
            "    if (rootDomain === '') {\n" +
            "        return '';\n" +
            "    } else {\n" +
            "        return '.' + rootDomain;\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "function getDomain(doc, locationHostname) {\n" +
            "    var i,\n" +
            "        testParts,\n" +
            "        mpTest = 'mptest=cookie',\n" +
            "        hostname = locationHostname.split('.');\n" +
            "    for (i = hostname.length - 1; i >= 0; i--) {\n" +
            "        testParts = hostname.slice(i).join('.');\n" +
            "        doc.cookie = mpTest + ';domain=.' + testParts + ';';\n" +
            "        if (doc.cookie.indexOf(mpTest) > -1){\n" +
            "            doc.cookie = mpTest.split('=')[0] + '=;domain=.' + testParts + ';expires=Thu, 01 Jan 1970 00:00:01 GMT;';\n" +
            "            return testParts;\n" +
            "        }\n" +
            "    }\n" +
            "    return '';\n" +
            "}\n" +
            "\n" +
            "\n" +
            "function setCookie(cname, data, raw) {\n" +
            "    var date = new Date(),\n" +
            "        expires = new Date(date.getTime() +\n" +
            "        (365 * 24 * 60 * 60 * 1000)).toGMTString(),\n" +
            "        domain, cookieDomain,\n" +
            "        value;\n" +
            "\n" +
            "    value = data;\n" +
            "\n" +
            "    cookieDomain = getCookieDomain();\n" +
            "\n" +
            "    if (cookieDomain === '') {\n" +
            "        domain = '';\n" +
            "    } else {\n" +
            "        domain = ';domain=' + cookieDomain;\n" +
            "    }\n" +
            "\n" +
            "var cookie = encodeURIComponent(cname) + '=' + value +\n" +
            "        ';expires=' + expires;\n" +
            "        //  +\n" +
            "        // ';path=/' + domain;\n" +
            "            console.log(\"SETTNG COOKIE: \" + cookie);\n" +
            "    window.document.cookie = cookie;" +
            "   console.log(\"RETRIEVING cookie: \" + window.document.cookie);\n" +
            "}";

    private static final String htmlWrapper = "\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <title>Mocha Tests</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"mocha\"></div>\n" +
            "<script type=\"text/javascript\">\n" +
            "%s" +
            "\n</script>" +
            "</body>\n" +
            "</html>";
    
    private RandomUtils mRandomUtils;

    @BeforeClass
    public static void beforeClass() throws Exception {
        try {
            URLConnection connection = new URL("https://jssdkcdns.mparticle.com/js/v2/mparticle.js").openConnection();
            StringBuilder document = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                document.append(line + '\n');
            }
            in.close();

            jsSdk = document.toString()
                    .replace("jssdk.mparticle.com/v2/JS/", "http://localhost:8080/v2")//; console.log(\"replaced url V2 single\");")
                    .replace("jssdks.mparticle.com/v2/JS/", "http://localhost:8080/v2")//; console.log(\"replaced url V2 plural\");")
                    .replace("jssdks.mparticle.com/v1/JS/", "http://localhost:8080/v1")//; console.log(\"replaced url V1 plural\");")
                    .replace("jssdk.mparticle.com/v1/JS/", "http://localhost:8080/v1")//; console.log(\"replaced url V1 single\");")
                    .replace("https://identity.mparticle.com/v1/", "http://localhost:8080/v1/")//; console.log(\"replaced url Identity\");")
                    .replace("//  jQuery v1.10.2 | (c) 2005, 2013 jQuery Foundation, Inc. | jquery.org/license", "console.log(\"starting sdk\")");
            sdkFetchedSuccessfully = true;
        } catch (Exception ex) {
            sdkFetchedSuccessfully = false;
        }
    }

    @Before
    public void before() throws Exception {
        Assume.assumeTrue(sdkFetchedSuccessfully);
        mRandomUtils =  RandomUtils.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    WebView.setWebContentsDebuggingEnabled(true);
                }
            });
        }
    }

    @Test
    public void testSetUserAttribute() throws Exception {
        final String key =  mRandomUtils.getAlphaNumericString(25);
        final String value =  mRandomUtils.getAlphaNumericString(25);
        String testJavascript = String.format("mParticle.Identity.getCurrentUser().setUserAttribute(\"%s\", \"%s\");\n", key, value);
        final boolean[] called = new boolean[1];
        runJavascriptTest(testJavascript, new MParticleJSInterface(){
            @Override
            @JavascriptInterface
            public void setUserAttribute(String json) {
                super.setUserAttribute(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    assertEquals(key, jsonObject.getString("key"));
                    assertEquals(value, jsonObject.getString("value"));
                    called[0] = true;
                }
                catch (JSONException jse) {
                    jse.printStackTrace();
                }

            }
        });
        TestingUtils.checkAllBool(called, 1, 20);
    }

    @Test
    public void testRemoveUserAttribute() throws Exception {
        final String key = mRandomUtils.getAlphaNumericString(255);
        String testJavascript = String.format("mParticle.Identity.getCurrentUser().removeUserAttribute(\"%s\");\n", key);
        final boolean[] called = new boolean[1];
        runJavascriptTest(testJavascript, new MParticleJSInterface(){
            @Override
            @JavascriptInterface
            public void removeUserAttribute(String json) {
                super.removeUserAttribute(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    assertEquals(key, jsonObject.getString("key"));
                    called[0] = true;
                }
                catch (JSONException jse) {
                    jse.printStackTrace();
                }
            }
        });
        TestingUtils.checkAllBool(called, 1, 20);
    }

    @Test
    public void testSetUserTag() throws Exception {
        final String tag = mRandomUtils.getAlphaNumericString(25);
        String testJavascript = String.format("mParticle.Identity.getCurrentUser().setUserTag(\"%s\");\n", tag);
        final boolean[] called = new boolean[1];
        //This is acceptable if the JS SDK calls either setUserTag, or setUserAttribute with a null value
        runJavascriptTest(testJavascript, new MParticleJSInterface(){
            @Override
            @JavascriptInterface
            public void setUserTag(String json) {
                super.setUserTag(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    assertEquals(tag, jsonObject.getString("key"));
                    called[0] = true;
                }
                catch (JSONException jse) {
                    jse.printStackTrace();
                }
            }

            @Override
            @JavascriptInterface
            public void setUserAttribute(String json) {
                super.setUserAttribute(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    assertEquals(tag, jsonObject.getString("key"));
                    assertEquals(jsonObject.optString("value", "null"), "null");
                    called[0] = true;
                }
                catch (JSONException jse) {
                    jse.printStackTrace();
                }
            }
        });
        TestingUtils.checkAllBool(called, 1, 2000);
    }

    @Test
    public void testLogEvent() throws Exception {
        final JSONObject customAttributes = MPUtility.mapToJson( mRandomUtils.getRandomAttributes(10));
        String testJavascript = String.format("mParticle.logEvent('Play Movie Tapped',\n" +
                "                         mParticle.EventType.Navigation,\n" +
                "                         %s\n" +
                "                         );", customAttributes.toString(4));
        final boolean[] called = new boolean[1];
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void logEvent(String json) {
                super.logEvent(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    if (jsonObject.getInt(JS_KEY_EVENT_DATATYPE) == JS_MSG_TYPE_PE) {
                        assertEquals(jsonObject.getInt(JS_KEY_EVENT_CATEGORY), MParticle.EventType.Navigation.ordinal());
                        JSONObject receivedCustomAttributes = jsonObject.getJSONObject(JS_KEY_EVENT_ATTRIBUTES);
                        assertUnorderedJsonEqual(customAttributes, receivedCustomAttributes);
                        called[0] = true;
                    }
                    Logger.error(new JSONObject(json).toString(4));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        TestingUtils.checkAllBool(called, 1, 20);
    }

    @Test
    public void testLogout() throws Exception {
        final Map<MParticle.IdentityType, String> userIdentityMap =  mRandomUtils.getRandomUserIdentities();
        JSONObject jsonObject = userIdentityMapToJson(userIdentityMap);
        String testJavascript = String.format("mParticle.Identity.logout(%s , null);", jsonObject.toString(4));

        final boolean[] called = new boolean[1];
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void logout(String json) {
                super.logout(json);
                try {
                    assertUnorderedJsonEqual(new JSONObject(json), userIdentityMapToJsonJsSdkStyle(userIdentityMap));
                    called[0] = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        TestingUtils.checkAllBool(called, 1, 20);
    }

    @Test
    public void testLogoutEmpty() throws Exception {
        String testJavascript = "mParticle.Identity.logout();";

        final boolean[] called = new boolean[1];
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void logout(String json) {
                if (json == null || json.equals("undefined")) {
                    logout();
                }
            }

            @Override
            @JavascriptInterface
            public void logout() {
                called[0] = true;
            }
        });
        TestingUtils.checkAllBool(called, 1, 20);
    }

    @Test
    public void testLogin() throws Exception {
        final Map<MParticle.IdentityType, String> userIdentityMap =  mRandomUtils.getRandomUserIdentities();
        JSONObject jsonObject = userIdentityMapToJson(userIdentityMap);
        String testJavascript = String.format("mParticle.Identity.login(%s , null);", jsonObject.toString(4));

        final boolean[] called = new boolean[1];
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void login(String json) {
                super.login(json);
                try {
                    assertUnorderedJsonEqual(new JSONObject(json), userIdentityMapToJsonJsSdkStyle(userIdentityMap));
                    called[0] = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        TestingUtils.checkAllBool(called, 1, 20);
    }

    @Test
    public void testLoginEmpty() throws Exception {
        String testJavascript = "mParticle.Identity.login();";

        final boolean[] called = new boolean[1];
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void login(String json) {
                if (json == null || json.equals("undefined")) {
                    login();
                }
            }

            @Override
            @JavascriptInterface
            public void login() {
                called[0] = true;
            }
        });
        TestingUtils.checkAllBool(called, 1, 20);
    }

    @Test
    public void testModify() throws Exception {
        final Map<MParticle.IdentityType, String> userIdentities =  mRandomUtils.getRandomUserIdentities();
        JSONObject jsonObject = userIdentityMapToJson(userIdentities);
        String testJavascript = String.format("mParticle.Identity.modify(%s , null);", jsonObject.toString(4));

        final boolean[] called = new boolean[1];
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void modify(String json) {
                super.modify(json);
                try {
                    assertUnorderedJsonEqual(new JSONObject(json), userIdentityMapToJsonJsSdkStyle(userIdentities));
                    called[0] = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        TestingUtils.checkAllBool(called, 1, 20);
    }

    private String getJavascriptWrappedinHtml(String testJavascript) {
        StringBuilder javascriptBuilder = new StringBuilder()
                .append(jsSetMpidFunction)
                .append("\n")
                .append(setMpidJavascript(mStartingMpid))
                .append("\n")
                .append(jsStartupMParticle)
                .append("\n")
                .append(jsSdk)
                .append("\n")
                .append(String.format(jsTestWrapper, testJavascript));
        return String.format(htmlWrapper, javascriptBuilder.toString());
    }

    private void runJavascriptTest(final String testJavascript, final MParticleJSInterface jsInterface) {
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
                        CookieManager cookieManager = CookieManager.getInstance();
                        cookieManager.setAcceptCookie(true);
                        WebView wv = rule.getActivity().findViewById(R.id.web_view);
                        wv.setWebViewClient(new WebViewClient() {
                            @Override
                            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                                //Overriding the method and allowing Options response essentially
                                //disables CORS, which will allow us to point network requests at our
                                //local server
                                if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
                                    return OptionsAllowResponse.build();
                                }

                                return null;
                            }
                        });
                        cookieManager.setAcceptThirdPartyCookies(wv, true);
                        wv.getSettings().setDomStorageEnabled(true);
                        wv.getSettings().setJavaScriptEnabled(true);
                        wv.removeJavascriptInterface(MParticleJSInterface.INTERFACE_NAME);
                        wv.addJavascriptInterface(
                                jsInterface,
                                MParticleJSInterface.INTERFACE_NAME);

                        wv.loadDataWithBaseURL("http://localhost/",
                                getJavascriptWrappedinHtml(testJavascript),
                                "text/html", "utf-8",
                                null);
                    }
                });
    }

    private String setMpidJavascript(long mpid) {
        String cookies = String.format("{'gs':{'ie':1|'dt':'test_key'|'cgid':'886e874b-862b-4822-a24a-1146cd057101'|'das':'62c91b8d-fef6-44ea-b2cc-b55714b0d827'|'csm':'WyJ0ZXN0TVBJRCJd'|'sid':'2535f9ed-ab19-4a7c-9eeb-ce4e41e0cb06'|'les':1518536950918|'ssd':1518536950916}|'%s':{'ui':'eyIxIjoiY3VzdG9tZXJpZDEifQ=='}|'cu':'%s'}"
                , String.valueOf(mpid)
                , String.valueOf(mpid));
        return String.format("setCookie('mprtcl-v4', \"%s\");", cookies, cookies);
    }

    private JSONObject userIdentityMapToJson(Map<MParticle.IdentityType, String> userIdentities) throws JSONException {
        JSONObject userIdentityJson = new JSONObject();
        for (Map.Entry<MParticle.IdentityType, String> entry : userIdentities.entrySet()) {
            userIdentityJson.put(AccessUtils.getIdentityTypeString(entry.getKey()), entry.getValue());
        }
        return new JSONObject()
                .put("userIdentities", userIdentityJson);
    }

    private JSONObject userIdentityMapToJsonJsSdkStyle(Map<MParticle.IdentityType, String> userIdentities) throws JSONException {
        JSONArray userIdentityJson = new JSONArray();
        for (Map.Entry<MParticle.IdentityType, String> entry : userIdentities.entrySet()) {
            userIdentityJson.put(new JSONObject()
                    .put("Type", entry.getKey().getValue())
                    .put("Identity", entry.getValue()));
        }
        return new JSONObject()
                .put("UserIdentities", userIdentityJson);
    }

    private void assertUnorderedJsonEqual(JSONObject object1, JSONObject object2) {
        if (object1 == object2) {
            return;
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
                    assertUnorderedJsonEqual((JSONObject) obj1Val, (JSONObject) obj2Val);
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
    private void assertJsonArrayEqual(JSONArray jsonArray1, JSONArray jsonArray2) {
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
            }
            catch (JSONException jse) {
                jse.printStackTrace();
            }
            try {
                jsonObject2.put(object2 == null ? null : object2.toString(), object2);
            }
            catch (JSONException jse) {
                jse.printStackTrace();
            }
        }
        assertUnorderedJsonEqual(jsonObject1, jsonObject2);
    }

    static class JavascriptBuilder {
        StringBuilder builder = new StringBuilder();


    }

    static class OptionsAllowResponse {
        static final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy kk:mm:ss", Locale.US);

        @TargetApi(21)
        static WebResourceResponse build() {
            Date date = new Date();
            final String dateString = formatter.format(date);

            Map<String, String> headers = new HashMap<String, String>() {{
                put("Connection", "close");
                put("Content-Type", "text/plain");
                put("Date", dateString + " GMT");
                put("Access-Control-Allow-Origin", "*");
                put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
                put("Access-Control-Max-Age", "600");
                put("Access-Control-Allow-Credentials", "true");
                put("Access-Control-Allow-Headers", "accept, authorization, Content-Type, x-mp-key");
                put("Via", "1.1 vegur");
            }};

            return new WebResourceResponse("text/plain", "UTF-8", 200, "OK", headers, null);
        }
    }
}


