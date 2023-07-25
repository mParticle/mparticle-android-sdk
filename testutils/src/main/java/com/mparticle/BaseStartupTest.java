package com.mparticle;

import static org.junit.Assert.fail;

import android.Manifest;
import android.content.Context;
import android.os.Environment;
import android.os.Looper;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@OrchestratorOnly
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseStartupTest {
    public static String LEGACY_FILE_NAME = "legacyStartupTimes.txt";
    public static String CURRENT_FILE_NAME = "startupTimes.txt";

    @Rule
    public GrantPermissionRule readExternaStoragePermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule writeExternaStoragePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);


    protected Context mContext;

    /**
     * To pull the startup file from the file dir, use:
     *
     * adb pull /storage/emulated/0/Android/data/com.mparticle.testutils.test/files/startupTimes.txt
     * adb pull /storage/emulated/0/Android/data/com.mparticle.testutils.test/files/legacyStartupTimes.txt
     */

    @BeforeClass
    public static void beforeBaseClass() {
        Looper.prepare();
    }

    @Before
    public void beforeBase() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void _deleteFile() {
        getFile().delete();
    }

    @Test
    public void testStartupTime0() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime1() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime2() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime3() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime4() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime5() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime6() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime7() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime8() throws Exception {
        startupAndRecord();
    }

    @Test
    public void testStartupTime9() throws Exception {
        startupAndRecord();
    }

    @AfterClass
    public static void testStartupTimeAcceptable() throws Exception {
        String legacyFile = readFile(LEGACY_FILE_NAME);
        String currentFile = readFile(CURRENT_FILE_NAME);
        if (MPUtility.isEmpty(legacyFile) || MPUtility.isEmpty(currentFile)) {
            return;
        }
        legacyFile = legacyFile.replace(" ", "");
        currentFile = currentFile.replace(" ", "");

        List<Long> legacyResults = new ArrayList<>();
        List<Long> currentResults = new ArrayList<>();

        for (String resultString : legacyFile.split(":")) {
            try {
                legacyResults.add(Long.parseLong(resultString));
            } catch (NumberFormatException ex) {
            }
        }

        for (String resultString : currentFile.split(":")) {
            try {
                currentResults.add(Long.parseLong(resultString));
            } catch (NumberFormatException ex) {
            }
        }
        Assume.assumeTrue(legacyResults.size() == currentResults.size());
        Assume.assumeTrue(legacyResults.size() == 10);

        Long legacySum = 0L;
        for (Long result : currentResults) {
            legacySum += result;
        }

        Long currentSum = 0L;
        for (Long result : currentResults) {
            currentSum += result;
        }

        Double legacyAverage = legacySum / 10.0;
        Double currentAverage = currentSum / 10.0;

        if (currentAverage > (legacyAverage * 2)) {
            fail(String.format("Startup time is unacceptably slow, Current avg time is %s, which is more that twice as long as Legacy avg time (%s)", currentAverage, legacyAverage));
        }
    }

    private void startupAndRecord() throws Exception {
        Long startTime = System.currentTimeMillis();
        startup();
        Long runTime = System.currentTimeMillis() - startTime;
        Logger.debug("Startup Result = " + runTime);
        FileOutputStream stream = new FileOutputStream(getFile(fileName()), true);
        stream.write((runTime + ":").getBytes());
        Logger.error(readFile());
    }

    protected abstract String fileName();

    protected abstract void startup() throws Exception;

    private static String readFile(String fileName) throws IOException {
        try {
            FileInputStream inputStream = new FileInputStream(getFile(fileName));
            byte[] bytes = new byte[inputStream.read()];
            inputStream.read(bytes);
            return new String(bytes);
        } catch (FileNotFoundException ex) {
            return "";
        }
    }

    protected String readFile() throws IOException {
        return readFile(fileName());
    }

    private File getFile() {
        return getFile(fileName());
    }

    private static File getFile(String fileName) {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), fileName);
    }

}
