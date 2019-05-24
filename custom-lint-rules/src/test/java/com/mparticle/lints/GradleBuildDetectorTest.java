package com.mparticle.lints;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.mparticle.lints.detectors.GradleBuildDetector;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class GradleBuildDetectorTest extends LintDetectorTest {
    @Override
    protected Detector getDetector() {
        return new GradleBuildDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(GradleBuildDetector.ISSUE);
    }

    // This one inherently can't happen, since the lint.jar file is in the mParticle dependency, but
    // we should test it anyway, in case there might be a weird case where the lint.jar get's cached,
    // and we should make sure we aren't breaking any project's lints.
    @Test
    public void testNoDependency() throws Exception {
        String source =
                "apply plugin: 'com.android.application'\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion \"25.0.2\"\n" +
                        "    defaultConfig {\n" +
                        "        applicationId \"com.mparticle.sample\"\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "        versionCode 1\n" +
                        "        versionName \"1.0\"\n" +
                        "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\" \n" +
                        "    }\n" +
                        "    buildTypes {\n" +
                        "        release {\n" +
                        "            minifyEnabled false\n" +
                        "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    compile 'com.android.support:appcompat-v7:25.1.1'\n" +
                        "    compile 'com.android.support:design:25.1.1'\n}";
        assertThat(lintProject(gradle(source)))
                .isEqualTo(Constants.NO_WARNINGS);
    }

    @Test
    public void testNoSingleDependencies() throws Exception {
        String source =
                "apply plugin: 'com.android.application'\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion \"25.0.2\"\n" +
                        "    defaultConfig {\n" +
                        "        applicationId \"com.mparticle.sample\"\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "        versionCode 1\n" +
                        "        versionName \"1.0\"\n" +
                        "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\" \n" +
                        "    }\n" +
                        "    buildTypes {\n" +
                        "        release {\n" +
                        "            minifyEnabled false\n" +
                        "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    compile 'com.android.support:appcompat-v7:25.1.1'\n" +
                        "    compile 'com.android.support:design:25.1.1'\n" +
                        "    compile 'com.mparticle:android-core:5.0'\n}";
        assertThat(lintProject(gradle(source)))
                .isEqualTo(Constants.NO_WARNINGS);
    }

    @Test
    public void testNoSingleDependencyWithPlus() throws Exception {
        String source =
                "apply plugin: 'com.android.application'\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion \"25.0.2\"\n" +
                        "    defaultConfig {\n" +
                        "        applicationId \"com.mparticle.sample\"\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "        versionCode 1\n" +
                        "        versionName \"1.0\"\n" +
                        "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\" \n" +
                        "    }\n" +
                        "    buildTypes {\n" +
                        "        release {\n" +
                        "            minifyEnabled false\n" +
                        "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    compile 'com.android.support:appcompat-v7:25.1.1'\n" +
                        "    compile 'com.android.support:design:25.1.1'\n" +
                        "    compile 'com.mparticle:android-core:5+'\n}";
        assertThat(lintProject(gradle(source)))
                .isEqualTo(Constants.NO_WARNINGS);
    }

    @Test
    public void testNoErrorMultipleDependencies() throws Exception {
        String source =
                "apply plugin: 'com.android.application'\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion \"25.0.2\"\n" +
                        "    defaultConfig {\n" +
                        "        applicationId \"com.mparticle.sample\"\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "        versionCode 1\n" +
                        "        versionName \"1.0\"\n" +
                        "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\" \n" +
                        "    }\n" +
                        "    buildTypes {\n" +
                        "        release {\n" +
                        "            minifyEnabled false\n" +
                        "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    compile 'com.android.support:appcompat-v7:25.1.1'\n" +
                        "    compile 'com.android.support:design:25.1.1'\n" +
                        "    compile 'com.mparticle:android-core:5.0'\n" +
                        "    compile 'com.mparticle:android-branch-kit:5.0'\n}";
        assertThat(lintProject(gradle(source)))
                .isEqualTo(Constants.NO_WARNINGS);
    }

    @Test
    public void testNoErrorMultipleDependenciesWithPluses() throws Exception {
        String source =
                "apply plugin: 'com.android.application'\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion \"25.0.2\"\n" +
                        "    defaultConfig {\n" +
                        "        applicationId \"com.mparticle.sample\"\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "        versionCode 1\n" +
                        "        versionName \"1.0\"\n" +
                        "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\" \n" +
                        "    }\n" +
                        "    buildTypes {\n" +
                        "        release {\n" +
                        "            minifyEnabled false\n" +
                        "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    compile 'com.android.support:appcompat-v7:25.1.1'\n" +
                        "    compile 'com.android.support:design:25.1.1'\n" +
                        "    compile 'com.mparticle:android-core:5+'\n" +
                        "    compile 'com.mparticle:android-branch-kit:5+'\n}";
        assertThat(lintProject(gradle(source)))
                .isEqualTo(Constants.NO_WARNINGS);
    }


    @Test
    public void testInconsistentVersions() throws Exception {
        String source =
                "apply plugin: 'com.android.application'\n" +
                "\n" +
                "android {\n" +
                "    compileSdkVersion 25\n" +
                "    buildToolsVersion \"25.0.2\"\n" +
                "    defaultConfig {\n" +
                "        applicationId \"com.mparticle.sample\"\n" +
                "        minSdkVersion 15\n" +
                "        targetSdkVersion 25\n" +
                "        versionCode 1\n" +
                "        versionName \"1.0\"\n" +
                "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\" \n" +
                "    }\n" +
                "    buildTypes {\n" +
                "        release {\n" +
                "            minifyEnabled false\n" +
                "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n" +
                "        }\n" +
                "    }\n" +
                "}\n"+
                "\n"+
                "dependencies {\n" +
                "    compile 'com.android.support:appcompat-v7:25.1.1'\n" +
                "    compile 'com.android.support:design:25.1.1'\n" +
                "    compile 'com.mparticle:android-core:5.0'\n" +
                "    compile 'com.mparticle:android-branch-kit:4.10.+'\n}";
        assertThat(lintProject(gradle(source)))
                .contains(GradleBuildDetector.MESSAGE_INCONSISTENCY_IN_VERSIONS_DETECTED)
                .contains(Constants.getErrorWarningMessageString(1,0));
    }

    @Test
    public void testInconsistentPluses() throws Exception {
        String source =
                "apply plugin: 'com.android.application'\n" +
                        "\n" +
                        "android {\n" +
                        "    compileSdkVersion 25\n" +
                        "    buildToolsVersion \"25.0.2\"\n" +
                        "    defaultConfig {\n" +
                        "        applicationId \"com.mparticle.sample\"\n" +
                        "        minSdkVersion 15\n" +
                        "        targetSdkVersion 25\n" +
                        "        versionCode 1\n" +
                        "        versionName \"1.0\"\n" +
                        "        testInstrumentationRunner \"android.support.test.runner.AndroidJUnitRunner\" \n" +
                        "    }\n" +
                        "    buildTypes {\n" +
                        "        release {\n" +
                        "            minifyEnabled false\n" +
                        "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    compile 'com.android.support:appcompat-v7:25.1.1'\n" +
                        "    compile 'com.android.support:design:25.1.1'\n" +
                        "    compile 'com.mparticle:android-core:5.0'\n" +
                        "    compile 'com.mparticle:android-android-adjust-kit:5+'\n}";
        assertThat(lintProject(gradle(source)))
                .contains(GradleBuildDetector.MESSAGE_INCONSISTENCY_IN_VERSIONS_DETECTED)
                .contains(GradleBuildDetector.MESSAGE_DONT_MIX_PLUSES)
                .contains(Constants.getErrorWarningMessageString(1, 0));
    }
}
