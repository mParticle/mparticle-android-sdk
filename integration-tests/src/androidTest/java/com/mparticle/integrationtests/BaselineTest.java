package com.mparticle.integrationtests;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.services.storage.TestStorage;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.networking.DomainMapping;
import com.mparticle.networking.NetworkOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Runs one scenario against WireMock and compares every request the SDK sent with
 * {@code assets/baselines/<Class>.<method>.json}. Run with {@code -Pintegration.tests.record=true}
 * to rewrite the baselines instead.
 */
public abstract class BaselineTest {
    static final String API_KEY = "integration-tests-api-key";
    static final String API_SECRET = "integration-tests-secret";
    static final String EMAIL = "integration@example.com";
    static final String CUSTOMER_ID = "integration-customer";

    private static final long SETTLE_MS = 2000;
    private static final long TIMEOUT_MS = 30000;
    private static final int MAX_UPLOAD_ROUNDS = 5;
    private static final boolean RECORD =
            Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("record"));

    @Rule
    public final TestName testName = new TestName();

    protected Context context;

    @Before
    public final void resetServer() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        WireMockAdmin.resetMappings();
        WireMockAdmin.resetRequests();
    }

    /** Options every scenario shares: WireMock for every endpoint, long upload interval, fixed identity. */
    protected MParticleOptions.Builder options() {
        String host = WireMockAdmin.HOST;
        NetworkOptions networkOptions = NetworkOptions.builder()
                .addDomainMapping(DomainMapping.configMapping(host).build())
                .addDomainMapping(DomainMapping.eventsMapping(host).build())
                .addDomainMapping(DomainMapping.identityMapping(host).build())
                .addDomainMapping(DomainMapping.aliasMapping(host).build())
                .addDomainMapping(DomainMapping.audienceMapping(host).build())
                .setPinningDisabled(true)
                .build();
        return MParticleOptions.builder(context)
                .credentials(API_KEY, API_SECRET)
                .environment(MParticle.Environment.Production)
                .logLevel(MParticle.LogLevel.VERBOSE)
                // Uploads happen only when a scenario calls upload(), so batching is deterministic.
                .uploadInterval(600)
                .networkOptions(networkOptions)
                .identify(IdentityApiRequest.withEmptyUser()
                        .email(EMAIL)
                        .customerId(CUSTOMER_ID)
                        .build());
    }

    protected MParticle start() throws InterruptedException {
        return start(options());
    }

    /** Starts the SDK and blocks until the initial identify call has completed. */
    protected MParticle start(@NonNull MParticleOptions.Builder builder) throws InterruptedException {
        CountDownLatch identified = new CountDownLatch(1);
        AtomicReference<String> failure = new AtomicReference<>();
        BaseIdentityTask task = new BaseIdentityTask()
                .addSuccessListener(result -> identified.countDown())
                .addFailureListener(response -> {
                    failure.set(String.valueOf(response));
                    identified.countDown();
                });
        MParticle.start(builder.identifyTask(task).build());
        assertTrue("initial identify did not complete", identified.await(30, TimeUnit.SECONDS));
        assertNull("initial identify failed", failure.get());
        return MParticle.getInstance();
    }

    /** Flushes the SDK until nothing new is sent, then checks or records the baseline. */
    protected void uploadAndVerify() throws Exception {
        String name = getClass().getSimpleName() + "." + testName.getMethodName();
        String expected = RECORD ? null : readBaseline(name);
        if (!RECORD && expected == null) {
            fail("No baseline for " + name + ". Record one with -Pintegration.tests.record=true");
        }
        JSONArray actual = RequestNormalizer.normalize(uploadUntilQuiet());
        if (RECORD) {
            writeBaseline(name, actual.toString(2) + "\n");
            return;
        }
        List<String> differences = BaselineDiff.diff(new JSONArray(expected), actual);
        if (!differences.isEmpty()) {
            fail("Requests differ from baseline " + name + ":\n  " + String.join("\n  ", differences)
                    + "\n\nActual (normalized):\n" + actual.toString(2));
        }
    }

    /**
     * One upload() only sends what is ready at that moment; messages the SDK writes
     * asynchronously (for example the identity-change messages from the initial identify) can
     * miss it. Keep uploading until a round sends nothing new.
     */
    private static JSONArray uploadUntilQuiet() throws Exception {
        int count = -1;
        for (int round = 0; round < MAX_UPLOAD_ROUNDS; round++) {
            MParticle.getInstance().upload();
            JSONArray requests = awaitQuiet();
            if (requests.length() == count) {
                return requests;
            }
            count = requests.length();
        }
        fail("SDK was still sending requests after " + MAX_UPLOAD_ROUNDS + " upload rounds:\n"
                + summarize(WireMockAdmin.requests()));
        return null;
    }

    private static String summarize(JSONArray requests) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < requests.length(); i++) {
            JSONObject request = requests.getJSONObject(i);
            sb.append("  ").append(request.getString("method")).append(' ')
                    .append(request.getString("url")).append('\n');
        }
        return sb.toString();
    }

    /** Polls the journal until it has been unchanged for {@link #SETTLE_MS}. */
    private static JSONArray awaitQuiet() throws Exception {
        long deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS;
        int lastCount = -1;
        long stableSince = SystemClock.elapsedRealtime();
        while (SystemClock.elapsedRealtime() < deadline) {
            JSONArray requests = WireMockAdmin.requests();
            if (requests.length() != lastCount) {
                lastCount = requests.length();
                stableSince = SystemClock.elapsedRealtime();
            } else if (SystemClock.elapsedRealtime() - stableSince >= SETTLE_MS) {
                return requests;
            }
            SystemClock.sleep(250);
        }
        fail("SDK did not stop sending requests within " + TIMEOUT_MS + "ms");
        return null;
    }

    private String readBaseline(String name) throws IOException {
        try (InputStream in = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open("baselines/" + name + ".json")) {
            return new String(RequestNormalizer.readAll(in), StandardCharsets.UTF_8);
        } catch (java.io.FileNotFoundException missing) {
            return null;
        }
    }

    private static void writeBaseline(String name, String json) throws IOException {
        try (OutputStream out = new TestStorage().openOutputFile("baselines/" + name + ".json")) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }
}
