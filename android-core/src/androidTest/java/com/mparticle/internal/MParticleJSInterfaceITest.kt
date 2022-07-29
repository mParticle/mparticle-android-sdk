package com.mparticle.internal

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.MutableBoolean
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.BaseEvent
import com.mparticle.BuildConfig
import com.mparticle.MParticle
import com.mparticle.api.identity.toIdentityType
import com.mparticle.commerce.Product
import com.mparticle.identity.AccessUtils
import com.mparticle.test.R
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.Mutable
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.Utils.assertJsonEqual
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch

class MParticleJSInterfaceITest : BaseStartedTest() {
    @Rule
    @JvmField
    var rule: ActivityScenarioRule<WebViewActivity> = ActivityScenarioRule<WebViewActivity>(
        WebViewActivity::class.java
    )

    @Before
    fun before() {
        Assume.assumeTrue(sdkFetchedSuccessfully)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Handler(Looper.getMainLooper()).post(
                Runnable {
                    WebView.setWebContentsDebuggingEnabled(
                        true
                    )
                }
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserAttribute() {
        val key: String = RandomUtils.getAlphaNumericString(25)
        val value: String = RandomUtils.getAlphaNumericString(25)
        val testJavascript = String.format(
            "mParticle.Identity.getCurrentUser().setUserAttribute(\"%s\", \"%s\");\n",
            key,
            value
        )
        val called = MutableBoolean(false)
        val latch: CountDownLatch = FailureLatch()
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                @JavascriptInterface
                override fun setUserAttribute(json: String) {
                    super.setUserAttribute(json)
                    try {
                        val jsonObject = JSONObject(json)
                        Assert.assertEquals(key, jsonObject.getString("key"))
                        Assert.assertEquals(value, jsonObject.getString("value"))
                        called.value = true
                        latch.countDown()
                    } catch (jse: JSONException) {
                        jse.printStackTrace()
                    }
                }
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveUserAttribute() {
        val key: String = RandomUtils.getAlphaNumericString(20)
        val testJavascript =
            String.format("mParticle.Identity.getCurrentUser().removeUserAttribute(\"%s\");\n", key)
        val called = MutableBoolean(false)
        val latch: CountDownLatch = FailureLatch()
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                @JavascriptInterface
                override fun removeUserAttribute(json: String) {
                    super.removeUserAttribute(json)
                    try {
                        val jsonObject = JSONObject(json)
                        Assert.assertEquals(key, jsonObject.getString("key"))
                        called.value = true
                        latch.countDown()
                    } catch (jse: JSONException) {
                        jse.printStackTrace()
                    }
                }
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserTag() {
        val tag: String = RandomUtils.getAlphaNumericString(25)
        val testJavascript =
            String.format("mParticle.Identity.getCurrentUser().setUserTag(\"%s\");\n", tag)
        val called = MutableBoolean(false)
        val latch: CountDownLatch = FailureLatch()
        // This is acceptable if the JS SDK calls either setUserTag, or setUserAttribute with a null value
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                @JavascriptInterface
                override fun setUserTag(json: String) {
                    super.setUserTag(json)
                    try {
                        val jsonObject = JSONObject(json)
                        Assert.assertEquals(tag, jsonObject.getString("key"))
                        called.value = true
                        latch.countDown()
                    } catch (jse: JSONException) {
                        jse.printStackTrace()
                    }
                }

                @JavascriptInterface
                override fun setUserAttribute(json: String) {
                    super.setUserAttribute(json)
                    try {
                        val jsonObject = JSONObject(json)
                        Assert.assertEquals(tag, jsonObject.getString("key"))
                        Assert.assertEquals(jsonObject.optString("value", "null"), "null")
                        called.value = true
                        latch.countDown()
                    } catch (jse: JSONException) {
                        jse.printStackTrace()
                    }
                }
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testLogEvent() {
        val customAttributes = MPUtility.mapToJson(RandomUtils.getRandomAttributes(10))
        val customFlagsJSON = MPUtility.mapToJson(customFlags)
        val testJavascript = String.format(
            """mParticle.logEvent('Play Movie Tapped',
                         mParticle.EventType.Navigation,
                         %s,
                         %s);""",
            customAttributes.toString(4), customFlagsJSON.toString(4)
        )
        var count = 0
        val latch: CountDownLatch = FailureLatch()
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                override fun logEvent(event: BaseEvent) {
                    val customFlags = event.customFlags
                    Assert.assertEquals(3, customFlags!!.size.toLong())
                    Assert.assertTrue(customFlags.containsKey("foo"))
                    Assert.assertTrue(customFlags.containsKey("bar"))
                    Assert.assertTrue(customFlags.containsKey("baz"))
                    val fooFlags = customFlags["foo"]!!
                    val barFlags = customFlags["bar"]!!
                    val bazFlags = customFlags["baz"]!!
                    Assert.assertEquals(3, fooFlags.size.toLong())
                    Assert.assertTrue(fooFlags.contains("50"))
                    Assert.assertTrue(fooFlags.contains("true"))
                    Assert.assertTrue(fooFlags.contains("-27"))
                    Assert.assertEquals(2, barFlags.size.toLong())
                    Assert.assertEquals(1, bazFlags.size.toLong())
                    count++
                    if (count == 2) {
                        latch.countDown()
                    }
                }

                @JavascriptInterface
                override fun logEvent(json: String) {
                    super.logEvent(json)
                    try {
                        val jsonObject = JSONObject(json)
                        // make sure we are receiving the expected event from JS world
                        if (jsonObject.getInt(JS_KEY_EVENT_DATATYPE) == JS_MSG_TYPE_PE) {
                            Assert.assertEquals(
                                jsonObject.getInt(JS_KEY_EVENT_CATEGORY).toLong(),
                                MParticle.EventType.Navigation.ordinal.toLong()
                            )
                            val receivedCustomAttributes = jsonObject.getJSONObject(
                                JS_KEY_EVENT_ATTRIBUTES
                            )
                            val receivedCustomFlags = jsonObject.getJSONObject(JS_KEY_EVENT_FLAGS)
                            assertJsonEqual(customAttributes, receivedCustomAttributes)
                            assertJsonEqual(customFlagsJSON, receivedCustomFlags)
                            count++
                            if (count == 2) {
                                latch.countDown()
                            }
                        }
                        Logger.error(JSONObject(json).toString(4))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        )
        latch.await()
    }

    @Test
    @Throws(Exception::class)
    fun testLogCommerceEvent() {
        val customAttributes = MPUtility.mapToJson(RandomUtils.getRandomAttributes(10))
        val customFlags = MPUtility.mapToJson(customFlags)
        val testJavascript = String.format(
            """// 1. Create the product
var product = mParticle.eCommerce.createProduct(
    'Double Room - Econ Rate', //
    'econ-1', 
    100.00, 
    4
);

// 2. Summarize the transaction
var transactionAttributes = {
    Id: 'foo-transaction-id',
    Revenue: 430.00,
    Tax: 30
};

// 3. Log the purchase event
mParticle.eCommerce.logPurchase(transactionAttributes, product, true, %s, %s);""",
            customAttributes.toString(4),
            customFlags
        )
        val called = MutableBoolean(false)
        val error = Mutable<Any?>(null)
        val latch: CountDownLatch = FailureLatch(count = 2)
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                override fun logEvent(event: BaseEvent) {
                    val customFlags = event.customFlags
                    Assert.assertEquals(3, customFlags!!.size.toLong())
                    Assert.assertTrue(customFlags.containsKey("foo"))
                    Assert.assertTrue(customFlags.containsKey("bar"))
                    Assert.assertTrue(customFlags.containsKey("baz"))
                    val fooFlags = customFlags["foo"]!!
                    val barFlags = customFlags["bar"]!!
                    val bazFlags = customFlags["baz"]!!
                    Assert.assertEquals(3, fooFlags.size.toLong())
                    Assert.assertTrue(fooFlags.contains("50"))
                    Assert.assertTrue(fooFlags.contains("true"))
                    Assert.assertTrue(fooFlags.contains("-27"))
                    Assert.assertEquals(2, barFlags.size.toLong())
                    Assert.assertEquals(1, bazFlags.size.toLong())
                    latch.countDown()
                }

                @JavascriptInterface
                override fun logEvent(json: String) {
                    super.logEvent(json)
                    try {
                        val commerceEvent = toCommerceEvent(JSONObject(json))
                        Assert.assertEquals(1, commerceEvent.products!!.size.toLong())
                        Assert.assertEquals(Product.PURCHASE, commerceEvent.productAction)
                        Assert.assertNull(commerceEvent.currency)
                        val product = commerceEvent.products!![0]
                        Assert.assertEquals("Double Room - Econ Rate", product.name)
                        Assert.assertEquals("econ-1", product.sku)
                        Assert.assertEquals(100.0, product.unitPrice, .1)
                        Assert.assertEquals(4.0, product.quantity, .1)
                        val transactionAttributes = commerceEvent.transactionAttributes
                        Assert.assertEquals("foo-transaction-id", transactionAttributes!!.id)
                        Assert.assertEquals(430.0, transactionAttributes.revenue!!, .1)
                        Assert.assertEquals(30.0, transactionAttributes.tax!!, .1)
                        Assert.assertNull(transactionAttributes.shipping)
                        Assert.assertNull(transactionAttributes.affiliation)
                        Assert.assertNull(transactionAttributes.couponCode)
                        assertJsonEqual(
                            customAttributes,
                            MPUtility.mapToJson(commerceEvent.customAttributes)
                        )
                        called.value = true
                    } catch (e: Exception) {
                        error.value = e
                    } catch (e: AssertionError) {
                        error.value = e
                    }
                    latch.countDown()
                }
            }
        )
        Assert.assertNull(error.value)
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testLogout() {
        val userIdentityMap: Map<MParticle.IdentityType, String> =
            RandomUtils.getRandomUserIdentities().entries.associate { it.key.toIdentityType() to it.value }
        val jsonObject = userIdentityMapToJson(userIdentityMap)
        val testJavascript =
            String.format("mParticle.Identity.logout(%s , null);", jsonObject.toString(4))
        val called = MutableBoolean(false)
        val latch: CountDownLatch = FailureLatch()
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                @JavascriptInterface
                override fun logout(json: String) {
                    super.logout(json)
                    try {
                        assertJsonEqual(
                            JSONObject(json),
                            userIdentityMapToJsonJsSdkStyle(userIdentityMap)
                        )
                        called.value = true
                        latch.countDown()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testLogoutEmpty() {
        val testJavascript = "mParticle.Identity.logout();"
        val called = MutableBoolean(false)
        val latch: CountDownLatch = FailureLatch()
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                @JavascriptInterface
                override fun logout(json: String) {
                    if (json == null || json == "undefined") {
                        logout()
                    }
                }

                @JavascriptInterface
                override fun logout() {
                    called.value = true
                    latch.countDown()
                }
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testLogin() {
        val userIdentityMap: Map<MParticle.IdentityType, String> =
            RandomUtils.getRandomUserIdentities().entries.associate { it.key.toIdentityType() to it.value }
        val jsonObject = userIdentityMapToJson(userIdentityMap)
        val testJavascript =
            String.format("mParticle.Identity.login(%s , null);", jsonObject.toString(4))
        val called = MutableBoolean(false)
        val latch: CountDownLatch = FailureLatch()
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                @JavascriptInterface
                override fun login(json: String) {
                    super.login(json)
                    try {
                        assertJsonEqual(
                            JSONObject(json),
                            userIdentityMapToJsonJsSdkStyle(userIdentityMap)
                        )
                        called.value = true
                        latch.countDown()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testLoginEmpty() {
        val testJavascript = "mParticle.Identity.login();"
        val called = MutableBoolean(false)
        val latch: CountDownLatch = FailureLatch()
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                @JavascriptInterface
                override fun login(json: String) {
                    if (json == null || json == "undefined") {
                        login()
                    }
                }

                @JavascriptInterface
                override fun login() {
                    called.value = true
                    latch.countDown()
                }
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testModify() {
        val userIdentities: Map<MParticle.IdentityType, String> =
            RandomUtils.getRandomUserIdentities().entries.associate { it.key.toIdentityType() to it.value }
        val jsonObject = userIdentityMapToJson(userIdentities)
        val testJavascript =
            String.format("mParticle.Identity.modify(%s , null);", jsonObject.toString(4))
        val called = MutableBoolean(false)
        val latch: CountDownLatch = FailureLatch()
        runJavascriptTest(
            testJavascript,
            object : MParticleJSInterface() {
                @JavascriptInterface
                override fun modify(json: String) {
                    super.modify(json)
                    try {
                        assertJsonEqual(
                            JSONObject(json),
                            userIdentityMapToJsonJsSdkStyle(userIdentities)
                        )
                        called.value = true
                        latch.countDown()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    private fun getJavascriptWrappedinHtml(testJavascript: String): String {
        val javascriptBuilder = StringBuilder()
            .append(jsSetMpidFunction)
            .append("\n")
            .append(setMpidJavascript(mStartingMpid))
            .append("\n")
            .append(jsStartupMParticle)
            .append("\n")
            .append(jsSdk)
            .append("\n")
            .append(String.format(jsTestWrapper, testJavascript))
        return String.format(htmlWrapper, javascriptBuilder.toString())
    }

    private fun runJavascriptTest(testJavascript: String, jsInterface: MParticleJSInterface) {
        Handler(Looper.getMainLooper()).post(
            Runnable {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                rule.scenario.onActivity {
                    it.findViewById<WebView>(R.id.web_view).let { wv ->
                        wv.setWebViewClient(object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                // Overriding the method and allowing Options response essentially
                                // disables CORS, which will allow us to point network requests at our
                                // local server
                                return if (request.getMethod()
                                    .equals("OPTIONS", ignoreCase = true)
                                ) {
                                    OptionsAllowResponse.build()
                                } else null
                            }
                        })
                        cookieManager.setAcceptThirdPartyCookies(wv, true)
                        wv.getSettings().setDomStorageEnabled(true)
                        wv.getSettings().setJavaScriptEnabled(true)
                        val bridgeName = MParticleJSInterface.getBridgeName(bridgeToken)
                        wv.removeJavascriptInterface(bridgeName)
                        wv.addJavascriptInterface(jsInterface, bridgeName)
                        wv.setWebChromeClient(object : WebChromeClient() {
                            override fun onConsoleMessage(
                                message: String,
                                lineNumber: Int,
                                sourceID: String
                            ) {
                                Logger.warning(
                                    "MParticle JS sdk",
                                    message + " -- From line " +
                                        lineNumber
                                )
                            }
                        })
                        val jsString = getJavascriptWrappedinHtml(testJavascript)
                        Logger.error(jsString)
                        wv.loadDataWithBaseURL(
                            "http://localhost/",
                            jsString,
                            "text/html", "utf-8",
                            null
                        )
                    }
                }
            }
        )
    }

    private fun setMpidJavascript(mpid: Long): String {
        val cookies = String.format(
            "{'gs':{'ie':1|'dt':'test_key'|'cgid':'886e874b-862b-4822-a24a-1146cd057101'|'das':'62c91b8d-fef6-44ea-b2cc-b55714b0d827'|'csm':'WyJ0ZXN0TVBJRCJd'|'sid':'2535f9ed-ab19-4a7c-9eeb-ce4e41e0cb06'|'les':1518536950918|'ssd':1518536950916}|'%s':{'ui':'eyIxIjoiY3VzdG9tZXJpZDEifQ=='}|'cu':'%s'}",
            mpid.toString(),
            mpid.toString()
        )
        return String.format("setCookie('mprtcl-v4', \"%s\");", cookies, cookies)
    }

    @Throws(JSONException::class)
    private fun userIdentityMapToJson(userIdentities: Map<MParticle.IdentityType, String>): JSONObject {
        val userIdentityJson = JSONObject()
        for ((key, value) in userIdentities) {
            userIdentityJson.put(AccessUtils.getIdentityTypeString(key), value)
        }
        return JSONObject()
            .put("userIdentities", userIdentityJson)
    }

    @Throws(JSONException::class)
    private fun userIdentityMapToJsonJsSdkStyle(userIdentities: Map<MParticle.IdentityType, String>): JSONObject {
        val userIdentityJson = JSONArray()
        for ((key, value) in userIdentities) {
            userIdentityJson.put(
                JSONObject()
                    .put("Type", key.value)
                    .put("Identity", value)
            )
        }
        return JSONObject()
            .put("UserIdentities", userIdentityJson)
    }

    private val customFlags: Map<String, Any?>
        private get() {
            val fooFlags = mutableListOf(50, true, -27)
            val barFlags = mutableListOf("this other", "that other")
            return mutableMapOf(
                "foo" to fooFlags,
                "bar" to barFlags,
                "baz" to "foobar",
                "nullval" to null
            )
        }

    internal object OptionsAllowResponse {
        val formatter = SimpleDateFormat("E, dd MMM yyyy kk:mm:ss", Locale.US)
        @TargetApi(21)
        fun build(): WebResourceResponse {
            val date = Date()
            val dateString = formatter.format(date)
            val headers: Map<String, String> = mutableMapOf(
                "Connection" to "close",
                "Content-Type" to "text/plain",
                "Date" to "$dateString GMT",
                "Access-Control-Allow-Origin" to "*",
                "Access-Control-Allow-Methods" to "GET, POST, DELETE, PUT, OPTIONS",
                "Access-Control-Max-Age" to "600",
                "Access-Control-Allow-Credentials" to "true",
                "Access-Control-Allow-Headers" to "accept, authorization, Content-Type, x-mp-key",
                "Via" to "1.1 vegur"
            )

            return WebResourceResponse("text/plain", "UTF-8", 200, "OK", headers, null)
        }
    }

    companion object {
        private var jsSdk: String? = null
        private var sdkFetchedSuccessfully = false
        private val bridgeToken: String = RandomUtils.getAlphaString(5)
        private const val bridgeVersion = "2"
        private val jsStartupMParticle = """window.mParticle = {
            config: {
                isDevelopmentMode: true,
                useCookieStorage: true,
                identifyRequest: {
                   userIdentities: { email: 'email@example.com', customerid: '123456' }
                   },
                 requiredWebviewBridgeName: "$bridgeToken",
                minWebviewBridgeVersion:"$bridgeVersion"
            }
        };    window.mParticle = window.mParticle || {};
    window.mParticle.config = window.mParticle.config || {};
    window.mParticle.config.rq = [];
    window.mParticle.ready = function (f) {
        window.mParticle.config.rq.push(f);
        console.log("pushed f");
    };
"""
        private const val jsTestWrapper = "   mParticle.init();\n" +
            "   mParticle.isDebug = true;\n" +
            " console.log(\"testing started\")\n " +
            "       window.mParticle.ready(function () {\n" +
            "       console.log(\"mparticle started in JS land\");\n" +
            "%s\n" +
            "      })\n"
        private const val jsSetMpidFunction = "function getCookieDomain() {\n" +
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
            "}"
        private const val htmlWrapper = "\n" +
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
            "</html>"

        @BeforeClass
        fun beforeClassJS() {
            try {
                if (BuildConfig.JS_TEST_SDK) {
                    val inputStream =
                        InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(
                            R.raw.mparticle_js_sdk
                        )
                    // add in all the basic configuration stuff the server would send with a production sdk fetch from the url
                    jsSdk = StringBuilder()
                        .append(
                            """
    window.mParticle = window.mParticle || {};;
    window.mParticle.config = window.mParticle.config || {};;
    window.mParticle.config.serviceUrl = 'jssdk.mparticle.com/v2/JS/';;
    window.mParticle.config.secureServiceUrl = 'jssdks.mparticle.com/v2/JS/';;
    window.mParticle.config.minWebviewBridgeVersion = 1;
    window.mParticle.config.aliasMaxWindow = 90;
    window.mParticle.config.kitConfigs = window.mParticle.config.kitConfigs || [];;
    window.mParticle.config.pixelConfigs = window.mParticle.config.pixelConfigs || [];;
                            """.trimIndent()
                        )
                        .append(toString(inputStream))
                        .append(
                            """
    window.mParticle.config.requestConfig = false;;
    mParticle.init(null, window.mParticle.config);;
                            """.trimIndent()
                        )
                        .toString()
                } else {
                    val connection =
                        URL("https://jssdkcdns.mparticle.com/js/v2/mparticle.js").openConnection()
                    jsSdk = toString(connection.getInputStream())
                }
                sdkFetchedSuccessfully = true
            } catch (ex: Exception) {
                sdkFetchedSuccessfully = false
            }
            if (sdkFetchedSuccessfully) {
                jsSdk = jsSdk!!.replace(
                    "jssdk.mparticle.com/v2/JS/",
                    "http://localhost:8080/v2"
                ) // ; console.log(\"replaced url V2 single\");")
                    .replace(
                        "jssdks.mparticle.com/v2/JS/",
                        "http://localhost:8080/v2"
                    ) // ; console.log(\"replaced url V2 plural\");")
                    .replace(
                        "jssdks.mparticle.com/v1/JS/",
                        "http://localhost:8080/v1"
                    ) // ; console.log(\"replaced url V1 plural\");")
                    .replace(
                        "jssdk.mparticle.com/v1/JS/",
                        "http://localhost:8080/v1"
                    ) // ; console.log(\"replaced url V1 single\");")
                    .replace(
                        "https://identity.mparticle.com/v1/",
                        "http://localhost:8080/v1/"
                    ) // ; console.log(\"replaced url Identity\");")
                    .replace(
                        "//  jQuery v1.10.2 | (c) 2005, 2013 jQuery Foundation, Inc. | jquery.org/license",
                        "console.log(\"starting sdk\")"
                    )
                    .replace(
                        "window.mParticle.config.minWebviewBridgeVersion = 1",
                        "window.mParticle.config.minWebviewBridgeVersion = " + bridgeVersion
                    )
            }
        }

        @Throws(IOException::class)
        private fun toString(inputStream: InputStream): String {
            val `in` = BufferedReader(InputStreamReader(inputStream))
            val document = StringBuilder()
            var line: String
            while (`in`.readLine().also { line = it } != null) {
                document.append(
                    """
    $line
    
                    """.trimIndent()
                )
            }
            `in`.close()
            return document.toString()
        }
    }

    class WebViewActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(com.mparticle.test.R.layout.web_view_activity)
        }
    }
}
