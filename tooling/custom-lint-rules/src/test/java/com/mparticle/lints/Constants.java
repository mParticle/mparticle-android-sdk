package com.mparticle.lints;

public class Constants {
    public static final String NO_WARNINGS = "No warnings.";

    private static final String ERROR_WARNING_FORMAT = "%d errors, %d warnings";

    public static String getErrorWarningMessageString(int errors, int warnings) {
        return String.format(ERROR_WARNING_FORMAT, errors, warnings);
    }
}
