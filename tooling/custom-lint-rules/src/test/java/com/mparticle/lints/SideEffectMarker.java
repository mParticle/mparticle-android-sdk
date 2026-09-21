package com.mparticle.lints;

import java.io.File;
import java.io.IOException;

/**
 * Test fixture only, deliberately never referenced from normal Kotlin/Java code in this module -
 * only ever named by a string inside a piece of analyzed lint source. Its static initializer
 * touches a file named by a system property, so a test can tell whether this class was ever
 * loaded (i.e. whether Class.forName ran on attacker-controlled input) without itself triggering
 * the load just by checking - reading a static field would load the class and defeat the test.
 */
public class SideEffectMarker {
    public static final String MARKER_PATH_PROPERTY = "mparticle.lint.test.sideEffectMarkerPath";

    static {
        String path = System.getProperty(MARKER_PATH_PROPERTY);
        if (path != null) {
            try {
                new File(path).createNewFile();
            } catch (IOException ignored) {
            }
        }
    }
}
