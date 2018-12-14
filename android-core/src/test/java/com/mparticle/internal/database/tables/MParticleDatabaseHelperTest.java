package com.mparticle.internal.database.tables;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class MParticleDatabaseHelperTest {

    @Test
    public void addColumnStringTest() throws Exception {
        String example = "ALTER TABLE table_name ADD COLUMN column_name INTEGER";
        assertEquals(example + " DEFAULT 123", MParticleDatabaseHelper.addIntegerColumnString("table_name", "column_name", "123"));
        assertEquals(example, MParticleDatabaseHelper.addColumnString("table_name", "column_name", "INTEGER"));
        example = example + " DEFAULT abcd";
        assertEquals(example, MParticleDatabaseHelper.addColumnString("table_name", "column_name", "INTEGER", "abcd"));
    }
}
