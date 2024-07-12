package com.mparticle.networking;

import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.ALIAS;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.AUDIENCE;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.CONFIG;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.EVENTS;
import static org.junit.Assert.fail;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.identity.AccessUtils;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;
import com.mparticle.testutils.MPLatch;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class MockServer {

    private static MockServer instance;

    private Context context;
    private List<MPConnectionTestImpl> requests = new LinkedList<>();
    private LinkedHashMap<Matcher, Object> serverLogic = new LinkedHashMap<>();
    private Map<Matcher, CountDownLatch> blockers = new HashMap<>();
    private MParticleBaseClientImpl baseClient;
    private Random ran = new Random();
    private ConfigManager mConfigManager;
    private Endpoints endpoints;

    private String IDENTIFY = AccessUtils.IDENTIFY_PATH;
    private String LOGIN = AccessUtils.LOGIN_PATH;
    private String LOGOUT = AccessUtils.LOGOUT_PATH;
    private String MODIFY = AccessUtils.MODIFY_PATH;

    public static MockServer getNewInstance(Context context) {
        stop();
        if (instance == null) {
            instance = new MockServer(context);
            instance.start();
        }
        return instance;
    }

    public static MockServer getInstance() {
        return instance;
    }

    private MockServer() {
    }

    private MockServer(Context context) {
        this.context = context;
        if (MParticle.getInstance() != null) {
            this.mConfigManager = MParticle.getInstance().Internal().getConfigManager();
        } else {
            this.mConfigManager = new ConfigManager(context);
        }
        MPUrl.setMPUrlFactory(new MPUrl.UrlFactory() {
            @Override
            public MPUrl getInstance(String url) {
                return new MPUrlTestImpl(url);
            }
        });
        this.endpoints = new Endpoints();
    }

    public static void stop() {
        instance = null;
    }

    private void start() {
        setUpHappyConfig();
        setupHappyEvents();
        setupHappyIdentify();
        setupHappyLogin();
        setupHappyLogout();
        setupHappyModify();
        setupHappyAlias();
        setupHappyAudience();
    }

    public Endpoints Endpoints() {
        return endpoints;
    }

    public ReceivedRequests Requests() {
        return new ReceivedRequests(requests);
    }

    void onRequestMade(MPConnectionTestImpl mockConnection) {
        try {
            Thread.sleep(50);
            requests.add(mockConnection);

            List<Map.Entry<Matcher, Object>> logicList = new ArrayList<>(serverLogic.entrySet());
            //try to get a match FILO style
            reverseAndUpdateKey(logicList);
            long delay = 0;
            boolean found = false;
            for (Map.Entry<Matcher, Object> entry : logicList) {
                if (entry.getKey().isMatch(mockConnection)) {
                    if (entry.getValue() instanceof Response) {
                        Response response = (Response) entry.getValue();
                        response.setRequest(mockConnection);
                        mockConnection.response = response.responseBody;
                        mockConnection.responseCode = response.responseCode;
                        if (!entry.getKey().keepAfterMatch) {
                            serverLogic.remove(entry.getKey());
                        }
                        delay = response.delay;
                        found = true;
                        break;
                    }
                    if (entry.getValue() instanceof CallbackResponse) {
                        CallbackResponse callbackResponse = (CallbackResponse) entry.getValue();
                        callbackResponse.invokeCallback(mockConnection);
                        if (!entry.getKey().keepAfterMatch) {
                            serverLogic.remove(entry.getKey());
                        }
                    }
                }
            }
            if (!found) {
                Logger.error("response not found for request: " + mockConnection.url.toString());
            }

            for (Map.Entry<Matcher, CountDownLatch> entry : new HashSet<>(blockers.entrySet())) {
                if (entry.getKey().isMatch(mockConnection)) {
                    blockers.remove(entry.getKey()).countDown();
                }
            }
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    interface OnRequestCallback {
        void onRequest(Response response, MPConnectionTestImpl connection);
    }


    public MockServer setUpHappyConfig() {
        Matcher match = new Matcher().urlMatcher(new UrlMatcher() {
            @Override
            public boolean isMatch(MPUrl url) {
                return url.getFile().equals(getUrl(CONFIG).getFile()) || url.getFile().contains("config2.mparticle.com");
            }
        });
        match.keepAfterMatch = true;
        Response response = new Response();
        response.responseCode = 200;
        response.responseBody = "{}";
        serverLogic.put(match, response);
        return this;
    }


    public MockServer setupConfigResponse(String responseString) {
        return setupConfigResponse(responseString, 0);
    }


    public MockServer setupConfigResponse(String responseString, int delay) {
        Matcher match = new Matcher(getUrl(CONFIG));
        match.keepAfterMatch = true;
        Response response = new Response();
        response.responseCode = 200;
        response.responseBody = responseString;
        response.delay = delay;
        serverLogic.put(match, response);
        return this;
    }

    public MockServer setupConfigDeferred() {
        Matcher match = new Matcher(getUrl(CONFIG));
        match.keepAfterMatch = true;
        Response response = new Response();
        response.responseCode = 304;
        response.responseBody = "";
        serverLogic.put(match, response);
        return this;
    }

    public MockServer setupHappyEvents() {
        Matcher match = new Matcher(getUrl(EVENTS));
        match.keepAfterMatch = true;
        Response response = new Response();
        response.responseCode = 200;
        response.responseBody = "{}";
        serverLogic.put(match, response);
        return this;
    }


    private MockServer setupHappyIdentify() {
        Matcher match = new Matcher(getUrl(IDENTIFY));
        match.keepAfterMatch = true;
        Response response = new Response();
        response.onRequestCallback = getHappyIdentityRequestCallback();
        serverLogic.put(match, response);
        return this;
    }


    public MockServer setupHappyIdentify(long mpid) {
        return setupHappyIdentify(mpid, 0);
    }


    public MockServer setupHappyIdentify(long mpid, int delay) {
        Matcher match = new Matcher(getUrl(IDENTIFY));
        Response response = new Response(getIdentityResponse(mpid, ran.nextBoolean()));
        response.delay = delay;
        serverLogic.put(match, response);
        return this;
    }

    private MockServer setupHappyLogin() {
        Matcher match = new Matcher(getUrl(LOGIN));
        match.keepAfterMatch = true;
        Response response = new Response();
        response.onRequestCallback = getHappyIdentityRequestCallback();
        serverLogic.put(match, response);
        return this;
    }


    public MockServer setupHappyLogin(long mpid) {
        Matcher match = new Matcher(getUrl(LOGIN));
        Response response = new Response(getIdentityResponse(mpid, ran.nextBoolean()));
        serverLogic.put(match, response);
        return this;
    }

    private MockServer setupHappyLogout() {
        Matcher match = new Matcher(getUrl(LOGOUT));
        match.keepAfterMatch = true;
        Response response = new Response();
        response.onRequestCallback = getHappyIdentityRequestCallback();
        serverLogic.put(match, response);
        return this;
    }


    public MockServer setupHappyLogout(long mpid) {
        Matcher match = new Matcher(getUrl(LOGOUT));
        Response response = new Response(getIdentityResponse(mpid, ran.nextBoolean()));
        serverLogic.put(match, response);
        return this;
    }


    public MockServer setupHappyModify() {
        return setupHappyModify(0);
    }

    public MockServer setupHappyModify(long delay) {
        Matcher match = new Matcher();
        match.urlMatcher = new UrlMatcher() {
            @Override
            public boolean isMatch(MPUrl url) {
                if (!url.getFile().contains(MODIFY)) {
                    return false;
                }
                return true;
            }
        };
        match.keepAfterMatch = true;
        Response response = new Response(new JSONObject().toString());
        if (delay > 0) {
            response.delay = delay;
        }
        serverLogic.put(match, response);
        return this;
    }

    public MockServer setupHappyAlias() {
        Matcher matcher = new Matcher(getUrl(ALIAS));
        matcher.keepAfterMatch = true;
        Response response = new Response(200, new JSONObject().toString());
        serverLogic.put(matcher, response);
        return this;
    }

    public MockServer setupHappyAudience() {
        Matcher matcher = new Matcher(getUrl(AUDIENCE));
        matcher.keepAfterMatch = true;
        Response response = new Response(200, new JSONObject().toString());
        serverLogic.put(matcher, response);
        return this;
    }


    public MockServer setEventsResponseLogic(int responseCode) {
        Matcher matcher = new Matcher(getUrl(EVENTS));
        matcher.keepAfterMatch = true;
        Response response = new Response(responseCode, new JSONObject().toString());
        serverLogic.put(matcher, response);
        return this;
    }

    public MockServer addConditionalIdentityResponse(long ifMpid, long thenMpid) {
        return addConditionalIdentityResponse(ifMpid, thenMpid, ran.nextBoolean(), 0);
    }

    public MockServer addConditionalIdentityResponse(long ifMpid, long thenMpid, boolean isLoggedIn) {
        return addConditionalIdentityResponse(ifMpid, thenMpid, isLoggedIn, 0);
    }

    public MockServer addConditionalIdentityResponse(long ifMpid, long thenMpid, boolean isLoggedIn, int delay) {
        Matcher match = new Matcher(getUrl(IDENTIFY));
        match.bodyMatch = getIdentityMpidMatch(ifMpid);
        Response response = new Response(200, getIdentityResponse(thenMpid, isLoggedIn));
        response.delay = delay;
        serverLogic.put(match, response);
        return this;
    }

    public MockServer addConditionalLoginResponse(long ifMpid, long thenMpid) {
        return addConditionalLoginResponse(ifMpid, thenMpid, ran.nextBoolean(), 0);
    }

    public MockServer addConditionalLoginResponse(long ifMpid, long thenMpid, boolean isLoggedIn) {
        return addConditionalLoginResponse(ifMpid, thenMpid, isLoggedIn, 0);
    }


    public MockServer addConditionalLoginResponse(long ifMpid, long thenMpid, boolean isLoggedIn, int delay) {
        Matcher match = new Matcher(getUrl(LOGIN));
        match.bodyMatch = getIdentityMpidMatch(ifMpid);
        Response response = new Response(200, getIdentityResponse(thenMpid, isLoggedIn));
        response.delay = delay;
        serverLogic.put(match, response);
        return this;
    }


    public MockServer addConditionalLogoutResponse(long ifMpid, long thenMpid) {
        return addConditionalLogoutResponse(ifMpid, thenMpid, ran.nextBoolean(), 0);
    }

    public MockServer addConditionalLogoutResponse(long ifMpid, long thenMpid, boolean isLoggedIn) {
        return addConditionalLogoutResponse(ifMpid, thenMpid, isLoggedIn, 0);
    }

    public MockServer addConditionalLogoutResponse(long ifMpid, long thenMpid, boolean isLoggedIn, int delay) {
        Matcher match = new Matcher(getUrl(LOGOUT));
        match.bodyMatch = getIdentityMpidMatch(ifMpid);
        Response response = new Response(200, getIdentityResponse(thenMpid, isLoggedIn));
        response.delay = delay;
        serverLogic.put(match, response);
        return this;
    }

    public void setupAliasResponse(int responseCode) {
        Matcher match = new Matcher(getUrl(ALIAS));
        match.keepAfterMatch = true;
        Response response = new Response(responseCode, new JSONObject().toString());
        serverLogic.put(match, response);
    }


    /**
     * This WILL block.
     */
    public void waitForVerify(Matcher matcher) throws InterruptedException {
        CountDownLatch latch = new MPLatch(1);
        waitForVerify(matcher, latch);
        latch.await();
    }

    /**
     * These WILL NOT block.
     */
    public void waitForVerify(Matcher matcher, final CountDownLatch latch) {
        waitForVerify(matcher, new RequestReceivedCallback() {
            @Override
            public void onRequestReceived(Request request) {
                latch.countDown();
            }
        });
    }

    public void waitForVerify(Matcher matcher, final RequestReceivedCallback callback) {
        CallbackResponse response = new CallbackResponse(callback);
        serverLogic.put(matcher, response);
    }

    private MPUrl getUrl(MParticleBaseClientImpl.Endpoint endpoint) {
        try {
            return getClient().getUrl(endpoint);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private MPUrl getUrl(String identityPath) {
        return AccessUtils.getUrl(identityPath);
    }

    private MPUrl getUrl(String identityPath, Long mpid) {
        return AccessUtils.getUrl(identityPath, mpid);
    }

    private MParticleBaseClientImpl getClient() {
        if (MParticle.getInstance() != null) {
            mConfigManager = MParticle.getInstance().Internal().getConfigManager();
        }
        if (mConfigManager != null && baseClient != null && mConfigManager.getApiKey() != null && !mConfigManager.getApiKey().equals(baseClient.mApiKey)) {
            baseClient = new MParticleBaseClientImpl(context, mConfigManager);
        }
        if (baseClient != null) {
            return baseClient;
        } else {
            return new MParticleBaseClientImpl(context, mConfigManager);
        }
    }

    private OnRequestCallback getHappyIdentityRequestCallback() {
        return new OnRequestCallback() {

            public void onRequest(Response response, MPConnectionTestImpl connection) {
                try {
                    IdentityRequest.IdentityRequestBody request = new IdentityRequest(connection).getBody();
                    response.responseCode = 200;
                    response.responseBody = getIdentityResponse(request.previousMpid != null && request.previousMpid != 0 ? request.previousMpid : ran.nextLong(), ran.nextBoolean());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    private String getIdentityResponse(long mpid, boolean isLoggedIn) {
        try {
            return new JSONObject().put("mpid", String.valueOf(mpid))
                    .put("context", "randomContext")
                    .put("is_logged_in", isLoggedIn)
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private JSONMatch getIdentityMpidMatch(final long mpid) {
        return new JSONMatch() {
            public boolean isMatch(JSONObject jsonObject) {
                return IdentityRequest.IdentityRequestBody.from(jsonObject).previousMpid == mpid;
            }
        };
    }

    //This does 2 things to clean up the logic before we can test it against the request
    //1) it reverses it, so we prioritize which "logic" gets the match by which one was set most
    //recently.
    //2) we update the request which have the SDK "key" in their URL to match the current Key. Usually
    //the server is started before MParticle.start() is called, which means the urls in the "logic"
    //might not contain the same SDK key that was set later in MParticle.start()
    private void reverseAndUpdateKey(List<Map.Entry<Matcher, Object>> logic) {
        Collections.sort(logic, new Comparator<Map.Entry<Matcher, Object>>() {
            @Override
            public int compare(Map.Entry<Matcher, Object> o1, Map.Entry<Matcher, Object> o2) {
                return o1.getKey().timestamp == o2.getKey().timestamp ? 0 : o1.getKey().timestamp < o2.getKey().timestamp ? 1 : -1;
            }
        });
        String apiKey = ConfigManager.getInstance(context).getApiKey();
        if (apiKey == null) {
            apiKey = "null";
        }
        try {
            for (Map.Entry<Matcher, Object> entry : logic) {
                String url = entry.getKey().url;
                if (url != null) {
                    if (url.contains(NetworkOptionsManager.MP_CONFIG_URL) && !url.contains(apiKey)) {
                        entry.getKey().url = getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).getFile();
                        Logger.error("New Url: " + url + " -> " + entry.getKey().url);
                    }
                    if (url.contains(NetworkOptionsManager.MP_URL_PREFIX) && !url.contains(apiKey)) {
                        entry.getKey().url = getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).getFile();
                    }
                    if (url.contains("/alias") && !url.contains(apiKey)) {
                        entry.getKey().url = getUrl(MParticleBaseClientImpl.Endpoint.ALIAS).getFile();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public interface RequestReceivedCallback {
        void onRequestReceived(Request request);
    }

    public interface UrlMatcher {
        boolean isMatch(MPUrl url);
    }

    abstract public static class IdentityMatcher implements JSONMatch {

        @Override
        public boolean isMatch(JSONObject jsonObject) {
            return isIdentityMatch(IdentityRequest.IdentityRequestBody.from(jsonObject));
        }

        abstract protected boolean isIdentityMatch(IdentityRequest.IdentityRequestBody identityRequest);
    }


    public class ReceivedRequests {
        public List<MPConnectionTestImpl> requests = new ArrayList<>();


        ReceivedRequests(List<MPConnectionTestImpl> connections) {
            if (connections != null) {
                requests = new ArrayList<>(connections);
            }
        }

        public List<Request> getIdentify() {
            return get(IDENTIFY);
        }

        public List<Request> getLogin() {
            return get(LOGIN);
        }

        public List<Request> getLogout() {
            return get(LOGOUT);
        }

        public List<Request> getModify() {
            List<Request> matchingRequests = new ArrayList<>();
            for (MPConnectionTestImpl request : requests) {
                String url = request.getURL().getFile();
                if (url.endsWith(MODIFY)) {
                    matchingRequests.add(new IdentityRequest(request));
                }
            }
            return matchingRequests;
        }

        public List<Request> getEvents() {
            return get(MParticleBaseClientImpl.Endpoint.EVENTS);
        }

        public List<Request> getConfig() {
            return get(MParticleBaseClientImpl.Endpoint.CONFIG);
        }

        public List<Request> getAudience() {
            return get(MParticleBaseClientImpl.Endpoint.AUDIENCE);
        }

        private List<Request> get(String identityEndpoint) {
            return get(MParticleBaseClientImpl.Endpoint.IDENTITY, getUrl(identityEndpoint));
        }

        private List<Request> get(MParticleBaseClientImpl.Endpoint endpoint) {
            return get(endpoint, getUrl(endpoint));
        }

        private List<Request> get(MParticleBaseClientImpl.Endpoint endpoint, MPUrl url) {
            List<Request> matchingRequests = new ArrayList<>();
            for (MPConnectionTestImpl request : requests) {
                if (request.getURL().getFile().equals(url.getFile())) {
                    switch (endpoint) {
                        case IDENTITY:
                            matchingRequests.add(new IdentityRequest(request));
                            break;
                        case CONFIG:
                        case EVENTS:
                        case AUDIENCE:
                            matchingRequests.add(new Request(request));
                            break;
                    }
                }
            }
            return matchingRequests;
        }
    }

    public interface JSONMatch {
        boolean isMatch(JSONObject jsonObject);
    }

    public class Endpoints {

        private Endpoints() {
        }

        public MPUrl getIdentifyUrl() {
            return getUrl(IDENTIFY);
        }

        public MPUrl getLoginUrl() {
            return getUrl(LOGIN);
        }

        public MPUrl getLogoutUrl() {
            return getUrl(LOGOUT);
        }

        public MPUrl getModifyUrl(Long mpid) {
            return getUrl(MODIFY, mpid);
        }

        public MPUrl getEventsUrl() {
            return getUrl(MParticleBaseClientImpl.Endpoint.EVENTS);
        }

        public MPUrl getConfigUrl() {
            return getUrl(MParticleBaseClientImpl.Endpoint.CONFIG);
        }

        public MPUrl getAliasUrl() {
            return getUrl(MParticleBaseClientImpl.Endpoint.ALIAS);
        }

    }
}
