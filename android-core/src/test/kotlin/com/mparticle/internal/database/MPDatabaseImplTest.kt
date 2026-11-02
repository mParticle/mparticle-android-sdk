package com.mparticle.internal.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.mparticle.SdkListener
import com.mparticle.internal.listeners.InternalListenerManager
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class MPDatabaseImplTest {
    private val sqLiteDatabase: SQLiteDatabase = Mockito.mock(SQLiteDatabase::class.java)
    private val cursor: Cursor = Mockito.mock(Cursor::class.java)
    private val contentValues: ContentValues = Mockito.mock(ContentValues::class.java)
    private val database = MPDatabaseImpl(sqLiteDatabase)

    @Test
    fun testInsertReturnsTheRowIdWithoutAListener() {
        withListener(null) {
            Mockito.`when`(sqLiteDatabase.insert("messages", null, contentValues)).thenReturn(5L)

            Assert.assertEquals(5L, database.insert("messages", null, contentValues))
        }
    }

    @Test
    fun testInsertReportsAStoredRowToAnEnabledListener() {
        val listener = enabledListener()
        withListener(listener) {
            Mockito.`when`(sqLiteDatabase.insert("messages", null, contentValues)).thenReturn(5L)

            Assert.assertEquals(5L, database.insert("messages", null, contentValues))
            Mockito.verify(listener).onEntityStored(5L, "messages", contentValues)
        }
    }

    @Test
    fun testInsertReportsNothingWhenTheInsertFails() {
        val listener = enabledListener()
        withListener(listener) {
            Mockito.`when`(sqLiteDatabase.insert("messages", null, contentValues)).thenReturn(-1L)

            Assert.assertEquals(-1L, database.insert("messages", null, contentValues))
            Mockito.verify(listener, Mockito.never()).onEntityStored(Mockito.anyLong(), Mockito.anyString(), Mockito.any())
        }
    }

    @Test
    fun testRawQueryPassesTheSelectionArguments() {
        Mockito.`when`(sqLiteDatabase.rawQuery("SELECT * FROM messages WHERE _id = ?", arrayOf("1"))).thenReturn(cursor)

        Assert.assertSame(cursor, database.rawQuery("SELECT * FROM messages WHERE _id = ?", "1"))
    }

    @Test
    fun testRawQueryWithNoSelectionArgumentsPassesAnEmptyArray() {
        Mockito.`when`(sqLiteDatabase.rawQuery("SELECT * FROM messages", arrayOf<String>())).thenReturn(cursor)

        Assert.assertSame(cursor, database.rawQuery("SELECT * FROM messages"))
    }

    @Test
    fun testQueryPassesItsArgumentsUnchangedWithoutAListener() {
        withListener(null) {
            val columns = arrayOf<String?>("message")
            val selectionArgs = arrayOf<String?>("1")
            Mockito
                .`when`(sqLiteDatabase.query("messages", columns, "_id = ?", selectionArgs, "session_id", "count > 1", "_id", "10"))
                .thenReturn(cursor)
            Mockito
                .`when`(sqLiteDatabase.query("messages", columns, "_id = ?", selectionArgs, "session_id", "count > 1", "_id"))
                .thenReturn(cursor)

            Assert.assertSame(cursor, database.query("messages", columns, "_id = ?", selectionArgs, "session_id", "count > 1", "_id", "10"))
            Assert.assertSame(cursor, database.query("messages", columns, "_id = ?", selectionArgs, "session_id", "count > 1", "_id"))
            Mockito.verifyNoInteractions(cursor)
        }
    }

    @Test
    fun testQueryAddsTheIdColumnAndReportsEveryRowToAnEnabledListener() {
        val listener = enabledListener()
        withListener(listener) {
            Mockito
                .`when`(sqLiteDatabase.query("messages", arrayOf<String?>("message", "_id"), null, null, null, null, null, null))
                .thenReturn(cursor)
            Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenReturn(0)
            Mockito.`when`(cursor.count).thenReturn(2)
            Mockito.`when`(cursor.isAfterLast).thenReturn(false, false, true)
            Mockito.`when`(cursor.getInt(0)).thenReturn(7, 8)

            Assert.assertSame(cursor, database.query("messages", arrayOf<String?>("message"), null, null, null, null, null, null))

            Mockito.verify(listener).onCompositeObjects("messages7", cursor)
            Mockito.verify(listener).onCompositeObjects("messages8", cursor)
            val inOrder = Mockito.inOrder(cursor)
            inOrder.verify(cursor).moveToFirst()
            inOrder.verify(cursor, Mockito.times(2)).moveToNext()
            inOrder.verify(cursor).moveToFirst()
            inOrder.verify(cursor).move(-1)
        }
    }

    @Test
    fun testQueryWithAnEnabledListenerKeepsAnIdColumnAndPassesNullColumns() {
        val listener = enabledListener()
        withListener(listener) {
            val columns = arrayOf<String?>("_id", "message")
            Mockito.`when`(sqLiteDatabase.query("messages", columns, null, null, null, null, null)).thenReturn(cursor)
            Mockito.`when`(sqLiteDatabase.query("breadcrumbs", null, null, null, null, null, null)).thenReturn(cursor)
            Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenReturn(0)

            Assert.assertSame(cursor, database.query("messages", columns, null, null, null, null, null))
            Assert.assertSame(cursor, database.query("breadcrumbs", null, null, null, null, null, null))

            Mockito.verify(listener, Mockito.never()).onCompositeObjects(Mockito.any(), Mockito.any())
            Mockito.verify(cursor, Mockito.times(2)).moveToFirst()
            Mockito.verify(cursor, Mockito.times(2)).move(-1)
        }
    }

    @Test
    fun testDeleteUpdateAndTheTransactionMethodsCallTheDatabase() {
        val whereArgs = arrayOf<String?>("1")
        Mockito.`when`(sqLiteDatabase.delete("messages", "_id = ?", whereArgs)).thenReturn(3)
        Mockito.`when`(sqLiteDatabase.update("messages", contentValues, "_id = ?", whereArgs)).thenReturn(2)

        Assert.assertEquals(3, database.delete("messages", "_id = ?", whereArgs))
        Assert.assertEquals(2, database.update("messages", contentValues, "_id = ?", whereArgs))
        database.beginTransaction()
        database.setTransactionSuccessful()
        database.endTransaction()

        val inOrder = Mockito.inOrder(sqLiteDatabase)
        inOrder.verify(sqLiteDatabase).beginTransaction()
        inOrder.verify(sqLiteDatabase).setTransactionSuccessful()
        inOrder.verify(sqLiteDatabase).endTransaction()
    }

    @Test
    fun testANullDatabaseThrowsAtTheFirstCallNotAtConstruction() {
        val nullDatabase = MPDatabaseImpl(null)

        Assert.assertThrows(NullPointerException::class.java) { nullDatabase.beginTransaction() }
    }

    private fun enabledListener(): InternalListenerManager {
        val constructor = InternalListenerManager::class.java.getDeclaredConstructor(Context::class.java)
        constructor.isAccessible = true
        val listener = Mockito.spy(constructor.newInstance(Mockito.mock(Context::class.java)))
        listener.addListener(SdkListener())
        Mockito.doNothing().`when`(listener).onEntityStored(Mockito.anyLong(), Mockito.anyString(), Mockito.any())
        Mockito.doNothing().`when`(listener).onCompositeObjects(Mockito.any(), Mockito.any())
        return listener
    }

    private fun withListener(listener: InternalListenerManager?, block: () -> Unit) {
        // InternalListenerManager has no test seam, so the test sets its single instance and restores the previous one.
        val instance = InternalListenerManager::class.java.getDeclaredField("instance")
        instance.isAccessible = true
        val previous = instance.get(null)
        instance.set(null, listener)
        try {
            Assert.assertEquals(listener != null, InternalListenerManager.isEnabled)
            block()
        } finally {
            instance.set(null, previous)
        }
    }
}
