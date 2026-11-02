package com.mparticle.internal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito

class SegmentDatabaseTest {
    private val database: SQLiteDatabase = Mockito.mock(SQLiteDatabase::class.java)
    private val segmentDatabase = SegmentDatabase(Mockito.mock(Context::class.java))

    @Test
    fun testOnCreateCreatesTheSegmentTableThenTheMembershipTable() {
        segmentDatabase.onCreate(database)

        assertCreateStatementsInOrder()
    }

    @Test
    fun testOnUpgradeRunsTheSameCreateStatements() {
        segmentDatabase.onUpgrade(database, 1, 2)

        assertCreateStatementsInOrder()
    }

    @Test
    fun testDatabaseFileNameAndVersion() {
        // The file name and version decide which database file an upgraded app opens on the device.
        val name = SegmentDatabase::class.java.getDeclaredField("DB_NAME")
        name.isAccessible = true
        val version = SegmentDatabase::class.java.getDeclaredField("DB_VERSION")
        version.isAccessible = true

        Assert.assertEquals("mparticle_segment.db", name.get(null))
        Assert.assertEquals(1, version.getInt(null))
    }

    private fun assertCreateStatementsInOrder() {
        val statements = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(database, Mockito.times(2)).execSQL(statements.capture())
        Mockito.verifyNoMoreInteractions(database)
        Assert.assertEquals(listOf(CREATE_SEGMENT_DDL, CREATE_SEGMENT_MEMBERSHIP_DDL), statements.allValues.map(::normalized))
    }

    // The Kotlin statements are raw strings laid out over several lines. With whitespace collapsed,
    // each must equal the Java's text, so SQLite creates the same tables.
    private fun normalized(sql: String): String = sql.replace(Regex("\\s+"), " ").replace("( ", "(").replace(" )", ")").trim()

    companion object {
        // The Java's statements, in the normalized form: one line each, a single space between words.
        private const val CREATE_SEGMENT_DDL =
            "CREATE TABLE IF NOT EXISTS segments (_id INTEGER PRIMARY KEY, name TEXT NOT NULL, endpoint_ids TEXT);"
        private val CREATE_SEGMENT_MEMBERSHIP_DDL =
            """
            CREATE TABLE IF NOT EXISTS segment_memberships (_id INTEGER PRIMARY KEY AUTOINCREMENT,
            segment_id INTEGER NOT NULL, timestamp REAL NOT NULL, action INTEGER NOT NULL,
            FOREIGN KEY (segment_id) REFERENCES segments (_id));
            """.trimIndent().replace("\n", " ")
    }
}
