package com.mparticle.testutils;

import com.mparticle.internal.Logger;
import com.mparticle.networking.MPConnectionTestImpl;
import com.mparticle.networking.MockServer;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CaptureLogcatOnFailingTest implements TestRule {
    private static final String LOGCAT_HEADER = "\n============== Logcat Output =============\n";
    private static final String STACKTRACE_HEADER = "\n============== Stacktrace ===============";
    private static final String MOCKSERVER_HEADER = "\n ============== Mock Server Requests ==========\n";
    private static final String ORIGINAL_CLASS_HEADER = "\nOriginal class: ";

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } catch (Throwable throwable) {
                    if (throwable instanceof AssumptionViolatedException) {
                        throw throwable;
                    }
                    String message = getReleventLogsAfterTestStart(description.getMethodName());
                    StringBuilder requestReceivedBuilder = new StringBuilder();
                    MockServer mockServer = MockServer.getInstance();
                    if (mockServer != null) {
                        for (MPConnectionTestImpl connection : mockServer.Requests().requests) {
                            requestReceivedBuilder.append(connection.getURL().toString())
                                    .append("\n")
                                    .append(connection.getBody())
                                    .append("\n")
                                    .append(connection.getResponseCode())
                                    .append(connection.getResponseMessage())
                                    .append("\n");
                        }
                    } else {
                        requestReceivedBuilder.append("Mock Server not started");
                    }
                    message = throwable.getMessage() + ORIGINAL_CLASS_HEADER
                            + throwable.getClass().getName() + LOGCAT_HEADER
                            + message + MOCKSERVER_HEADER +
                            requestReceivedBuilder.toString() + STACKTRACE_HEADER;
                    Throwable modifiedThrowable = new Throwable(message);
                    modifiedThrowable.setStackTrace(throwable.getStackTrace());
                    throw modifiedThrowable;
                }
            }
        };
    }

    private String getReleventLogsAfterTestStart(String testName) {
        String testStartMessage = "TestRunner: started: " + testName;
        String currentProcessId = Integer.toString(android.os.Process.myPid());
        Boolean isRecording = false;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "threadtime"});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(currentProcessId)) {
                    if (line.contains(testStartMessage)) {
                        isRecording = true;
                    }
                }
                if (isRecording) {
                    builder.append(line);
                    builder.append("\n");
                }
            }
        } catch (IOException e) {
            Logger.error(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Logger.error(e);
                }
            }
        }
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-b", "all", "-c"});
        } catch (IOException e) {
            Logger.error(e);
        }
        return builder.toString();
    }
}