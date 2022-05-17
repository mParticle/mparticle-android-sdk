package com.mparticle.internal;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import android.util.MutableBoolean;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mparticle.BaseEvent;
import com.mparticle.MParticle;
import com.mparticle.WebViewActivity;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.identity.AccessUtils;
import com.mparticle.test.R;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.BuildConfig;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.RandomUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.mparticle.testutils.TestingUtils.assertJsonEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MParticleJSInterfaceITest extends BaseCleanStartedEachTest {

    @Rule
    public ActivityTestRule<WebViewActivity> rule = new ActivityTestRule<WebViewActivity>(WebViewActivity.class);

    private static String jsSdk;
    private static boolean sdkFetchedSuccessfully = false;
    private static String bridgeToken = new RandomUtils().getAlphaString(5);
    private static String bridgeVersion = "2";

    private static final String jsStartupMParticle = "window.mParticle = {\n" +
            "            config: {\n" +
            "                isDevelopmentMode: true,\n" +
            "                useCookieStorage: true,\n" +
            "                identifyRequest: {\n" +
            "                   userIdentities: { email: 'email@example.com', customerid: '123456' }\n" +
            "                   },\n " +
            "                requiredWebviewBridgeName: \"" + bridgeToken + "\",\n" +
            "                minWebviewBridgeVersion:\"" + bridgeVersion + "\"\n" +
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
            "    window.document.cookie = cookie;\n" +
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

    @BeforeClass
    public static void beforeClass() {
        try {
            if (BuildConfig.JS_TEST_SDK) {
                InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().openRawResource(R.raw.mparticle_js_sdk);
                //add in all the basic configuration stuff the server would send with a production sdk fetch from the url
                jsSdk = new StringBuilder()
                        .append("window.mParticle = window.mParticle || {};;\n" +
                                "window.mParticle.config = window.mParticle.config || {};;\n" +
                                "window.mParticle.config.serviceUrl = 'jssdk.mparticle.com/v2/JS/';;\n" +
                                "window.mParticle.config.secureServiceUrl = 'jssdks.mparticle.com/v2/JS/';;\n" +
                                "window.mParticle.config.minWebviewBridgeVersion = 1;\n" +
                                "window.mParticle.config.aliasMaxWindow = 90;\n" +
                                "window.mParticle.config.kitConfigs = window.mParticle.config.kitConfigs || [];;\n" +
                                "window.mParticle.config.pixelConfigs = window.mParticle.config.pixelConfigs || [];;")
                        .append(toString(inputStream))
                        .append("window.mParticle.config.requestConfig = false;;\n" +
                                "mParticle.init(null, window.mParticle.config);;")
                        .toString();
            } else {
                URLConnection connection = new URL("https://jssdkcdns.mparticle.com/js/v2/mparticle.js").openConnection();
                jsSdk = toString(connection.getInputStream());
            }
            sdkFetchedSuccessfully = true;
        } catch (Exception ex) {
            sdkFetchedSuccessfully = false;
        }
        if (sdkFetchedSuccessfully) {
            jsSdk = jsSdk.replace("jssdk.mparticle.com/v2/JS/", "http://localhost:8080/v2")//; console.log(\"replaced url V2 single\");")
                    .replace("jssdks.mparticle.com/v2/JS/", "http://localhost:8080/v2")//; console.log(\"replaced url V2 plural\");")
                    .replace("jssdks.mparticle.com/v1/JS/", "http://localhost:8080/v1")//; console.log(\"replaced url V1 plural\");")
                    .replace("jssdk.mparticle.com/v1/JS/", "http://localhost:8080/v1")//; console.log(\"replaced url V1 single\");")
                    .replace("https://identity.mparticle.com/v1/", "http://localhost:8080/v1/")//; console.log(\"replaced url Identity\");")
                    .replace("//  jQuery v1.10.2 | (c) 2005, 2013 jQuery Foundation, Inc. | jquery.org/license", "console.log(\"starting sdk\")")
                    .replace("window.mParticle.config.minWebviewBridgeVersion = 1", "window.mParticle.config.minWebviewBridgeVersion = " + bridgeVersion);
        }
    }

    @Before
    public void before() {
        Assume.assumeTrue(sdkFetchedSuccessfully);
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
        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(1);
        runJavascriptTest(testJavascript, new MParticleJSInterface(){
            @Override
            @JavascriptInterface
            public void setUserAttribute(String json) {
                super.setUserAttribute(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    assertEquals(key, jsonObject.getString("key"));
                    assertEquals(value, jsonObject.getString("value"));
                    called.value = true;
                    latch.countDown();
                }
                catch (JSONException jse) {
                    jse.printStackTrace();
                }

            }
        });
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testRemoveUserAttribute() throws Exception {
        final String key = mRandomUtils.getAlphaNumericString(20);
        String testJavascript = String.format("mParticle.Identity.getCurrentUser().removeUserAttribute(\"%s\");\n", key);
        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(1);
        runJavascriptTest(testJavascript, new MParticleJSInterface(){
            @Override
            @JavascriptInterface
            public void removeUserAttribute(String json) {
                super.removeUserAttribute(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    assertEquals(key, jsonObject.getString("key"));
                    called.value = true;
                    latch.countDown();
                }
                catch (JSONException jse) {
                    jse.printStackTrace();
                }
            }
        });
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testSetUserTag() throws Exception {
        final String tag = mRandomUtils.getAlphaNumericString(25);
        String testJavascript = String.format("mParticle.Identity.getCurrentUser().setUserTag(\"%s\");\n", tag);
        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(1);
        //This is acceptable if the JS SDK calls either setUserTag, or setUserAttribute with a null value
        runJavascriptTest(testJavascript, new MParticleJSInterface(){
            @Override
            @JavascriptInterface
            public void setUserTag(String json) {
                super.setUserTag(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    assertEquals(tag, jsonObject.getString("key"));
                    called.value = true;
                    latch.countDown();
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
                    called.value = true;
                    latch.countDown();
                }
                catch (JSONException jse) {
                    jse.printStackTrace();
                }
            }
        });
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testLogEvent() throws Exception {
        final JSONObject customAttributes = MPUtility.mapToJson( mRandomUtils.getRandomAttributes(10));
        final JSONObject customFlagsJSON = MPUtility.mapToJson(getCustomFlags());
        String testJavascript = String.format("mParticle.logEvent('Play Movie Tapped',\n" +
                "                         mParticle.EventType.Navigation,\n" +
                "                         %s,\n" +
                "                         %s);", customAttributes.toString(4), customFlagsJSON.toString(4));
        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(2);
        runJavascriptTest(testJavascript, new MParticleJSInterface() {

            @Override
            protected void logEvent(BaseEvent event) {
                Map<String, List<String>> customFlags = event.getCustomFlags();
                assertEquals(3, customFlags.size());
                assertTrue(customFlags.containsKey("foo"));
                assertTrue(customFlags.containsKey("bar"));
                assertTrue(customFlags.containsKey("baz"));
                List<String> fooFlags = customFlags.get("foo");
                List<String> barFlags = customFlags.get("bar");
                List<String> bazFlags = customFlags.get("baz");
                assertEquals(3, fooFlags.size());
                assertTrue(fooFlags.contains("50"));
                assertTrue(fooFlags.contains("true"));
                assertTrue(fooFlags.contains("-27"));
                assertEquals(2, barFlags.size());
                assertEquals(1, bazFlags.size());
                latch.countDown();
            }

            @Override
            @JavascriptInterface
            public void logEvent(String json) {
                super.logEvent(json);
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    //make sure we are receiving the expected event from JS world
                    if (jsonObject.getInt(JS_KEY_EVENT_DATATYPE) == JS_MSG_TYPE_PE) {
                        assertEquals(jsonObject.getInt(JS_KEY_EVENT_CATEGORY), MParticle.EventType.Navigation.ordinal());
                        JSONObject receivedCustomAttributes = jsonObject.getJSONObject(JS_KEY_EVENT_ATTRIBUTES);
                        JSONObject receivedCustomFlags = jsonObject.getJSONObject(JS_KEY_EVENT_FLAGS);
                        assertJsonEqual(customAttributes, receivedCustomAttributes);
                        assertJsonEqual(customFlagsJSON, receivedCustomFlags);
                        called.value = true;
                        latch.countDown();
                    }
                    Logger.error(new JSONObject(json).toString(4));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testLogCommerceEvent() throws Exception {
        final JSONObject customAttributes = MPUtility.mapToJson( mRandomUtils.getRandomAttributes(10));
        final JSONObject customFlags = MPUtility.mapToJson(getCustomFlags());
        String testJavascript = String.format("// 1. Create the product\n" +
                "var product = mParticle.eCommerce.createProduct(\n" +
                "    'Double Room - Econ Rate', //\n" +
                "    'econ-1', \n" +
                "    100.00, \n" +
                "    4\n" +
                ");\n" +
                "\n" +
                "// 2. Summarize the transaction\n" +
                "var transactionAttributes = {\n" +
                "    Id: 'foo-transaction-id',\n" +
                "    Revenue: 430.00,\n" +
                "    Tax: 30\n" +
                "};\n" +
                "\n" +
                "// 3. Log the purchase event\n" +
                "mParticle.eCommerce.logPurchase(transactionAttributes, product, true, %s, %s);", customAttributes.toString(4), customFlags);

        final MutableBoolean called = new MutableBoolean(false);
        final AndroidUtils.Mutable<Object> error = new AndroidUtils.Mutable<Object>(null);
        final CountDownLatch latch = new MPLatch(2);
        runJavascriptTest(testJavascript, new MParticleJSInterface() {

            @Override
            protected void logEvent(BaseEvent event) {
                Map<String, List<String>> customFlags = event.getCustomFlags();
                assertEquals(3, customFlags.size());
                assertTrue(customFlags.containsKey("foo"));
                assertTrue(customFlags.containsKey("bar"));
                assertTrue(customFlags.containsKey("baz"));
                List<String> fooFlags = customFlags.get("foo");
                List<String> barFlags = customFlags.get("bar");
                List<String> bazFlags = customFlags.get("baz");
                assertEquals(3, fooFlags.size());
                assertTrue(fooFlags.contains("50"));
                assertTrue(fooFlags.contains("true"));
                assertTrue(fooFlags.contains("-27"));
                assertEquals(2, barFlags.size());
                assertEquals(1, bazFlags.size());
                latch.countDown();
            }

            @Override
            @JavascriptInterface
            public void logEvent(String json) {
                super.logEvent(json);
                try {
                    CommerceEvent commerceEvent = toCommerceEvent(new JSONObject(json));
                    assertEquals(1, commerceEvent.getProducts().size());
                    assertEquals(Product.PURCHASE, commerceEvent.getProductAction());
                    assertNull(commerceEvent.getCurrency());
                    Product product = commerceEvent.getProducts().get(0);
                    assertEquals("Double Room - Econ Rate", product.getName());
                    assertEquals("econ-1", product.getSku());
                    assertEquals(100.0, product.getUnitPrice(), .1);
                    assertEquals(4.0, product.getQuantity(), .1);
                    TransactionAttributes transactionAttributes = commerceEvent.getTransactionAttributes();
                    assertEquals("foo-transaction-id", transactionAttributes.getId());
                    assertEquals(430.0, transactionAttributes.getRevenue(), .1);
                    assertEquals(30.0, transactionAttributes.getTax(), .1);
                    assertNull(transactionAttributes.getShipping());
                    assertNull(transactionAttributes.getAffiliation());
                    assertNull(transactionAttributes.getCouponCode());
                    assertJsonEqual(customAttributes, MPUtility.mapToJson(commerceEvent.getCustomAttributeStrings()));
                    called.value = true;
                } catch (Exception e) {
                    error.value = e;
                } catch (AssertionError e) {
                    error.value = e;
                }
                latch.countDown();
            }
        });
        assertNull(error.value);
        latch.await();
        assertTrue(called.value);
    }


    @Test
    public void testLogout() throws Exception {
        final Map<MParticle.IdentityType, String> userIdentityMap =  mRandomUtils.getRandomUserIdentities();
        JSONObject jsonObject = userIdentityMapToJson(userIdentityMap);
        String testJavascript = String.format("mParticle.Identity.logout(%s , null);", jsonObject.toString(4));

        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(1);
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void logout(String json) {
                super.logout(json);
                try {
                    assertJsonEqual(new JSONObject(json), userIdentityMapToJsonJsSdkStyle(userIdentityMap));
                    called.value = true;
                    latch.countDown();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testLogoutEmpty() throws Exception {
        String testJavascript = "mParticle.Identity.logout();";

        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(1);
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
                called.value = true;
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testLogin() throws Exception {
        final Map<MParticle.IdentityType, String> userIdentityMap =  mRandomUtils.getRandomUserIdentities();
        JSONObject jsonObject = userIdentityMapToJson(userIdentityMap);
        String testJavascript = String.format("mParticle.Identity.login(%s , null);", jsonObject.toString(4));

        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(1);
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void login(String json) {
                super.login(json);
                try {
                    assertJsonEqual(new JSONObject(json), userIdentityMapToJsonJsSdkStyle(userIdentityMap));
                    called.value = true;
                    latch.countDown();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testLoginEmpty() throws Exception {
        String testJavascript = "mParticle.Identity.login();";

        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(1);
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
                called.value = true;
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testModify() throws Exception {
        final Map<MParticle.IdentityType, String> userIdentities =  mRandomUtils.getRandomUserIdentities();
        JSONObject jsonObject = userIdentityMapToJson(userIdentities);
        String testJavascript = String.format("mParticle.Identity.modify(%s , null);", jsonObject.toString(4));

        final MutableBoolean called = new MutableBoolean(false);
        final CountDownLatch latch = new MPLatch(1);
        runJavascriptTest(testJavascript, new MParticleJSInterface() {
            @Override
            @JavascriptInterface
            public void modify(String json) {
                super.modify(json);
                try {
                    assertJsonEqual(new JSONObject(json), userIdentityMapToJsonJsSdkStyle(userIdentities));
                    called.value = true;
                    latch.countDown();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await();
        assertTrue(called.value);
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
                        String bridgeName = MParticleJSInterface.getBridgeName(bridgeToken);
                        wv.removeJavascriptInterface(bridgeName);
                        wv.addJavascriptInterface(jsInterface, bridgeName);
                        wv.setWebChromeClient(new WebChromeClient() {
                            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                                Logger.warning("MParticle JS sdk", message + " -- From line "
                                        + lineNumber);
                            }
                        });

                        String jsString = getJavascriptWrappedinHtml(testJavascript);
                        Logger.error(jsString);
                        wv.loadDataWithBaseURL("http://localhost/",
                                jsString,
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

    private Map<String, Object> getCustomFlags() {
        final HashMap<String, Object> customFlags = new HashMap<String, Object>();
        List<Object> fooFlags = new ArrayList<Object>();
        fooFlags.add(50);
        fooFlags.add(true);
        fooFlags.add(-27);
        List<String> barFlags = new ArrayList<String>();
        barFlags.add("this other");
        barFlags.add("that other");
        customFlags.put("foo", fooFlags);
        customFlags.put("bar", barFlags);
        customFlags.put("baz", "foobar");
        customFlags.put("nullval", null);
        return customFlags;
    }

    private static String toString(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder document = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            document.append(line + '\n');
        }
        in.close();
        return document.toString();
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


