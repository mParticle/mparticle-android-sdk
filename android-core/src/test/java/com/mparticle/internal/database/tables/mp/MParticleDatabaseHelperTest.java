package com.mparticle.internal.database.tables.mp;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

public class MParticleDatabaseHelperTest {

    @Test
    public void addColumnStringTest() throws Exception {
        String example = "ALTER TABLE table_name ADD COLUMN column_name INTEGER DEFAULT 123";
        assertTrue(example.equals(MParticleDatabaseHelper.addIntegerColumnString("table_name", "column_name", "123")));
        assertTrue(example.equals(MParticleDatabaseHelper.addColumnString("table_name", "column_name", "INTEGER")));
        example = example + " DEFAULT abcd";
        assertTrue(example.equals(MParticleDatabaseHelper.addColumnString("table_name", "column_name", "STRING", "abcd")));
    }
}
