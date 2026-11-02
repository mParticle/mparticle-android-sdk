package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import com.mparticle.internal.Logger
import com.mparticle.internal.database.MPDatabase
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class UserAttributesServiceTest {
    private val database: MPDatabase = Mockito.mock(MPDatabase::class.java)
    private val cursor: Cursor = Mockito.mock(Cursor::class.java)

    @Test
    fun testInsertAttributeStoresEveryColumn() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            UserAttributesService.insertAttribute(database, "color", "blue", 100L, true, 7L)

            Assert.assertEquals(1, contentValues.constructed().size)
            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("mp_id", 7L)
            Mockito.verify(values).put("attribute_key", "color")
            Mockito.verify(values).put("attribute_value", "blue")
            Mockito.verify(values).put("is_list", true)
            Mockito.verify(values).put("created_time", 100L)
            Mockito.verify(database).insert("attributes", null, values)
        }
    }

    @Test
    fun testDeleteAttributesDeletesByKeyAndMpid() {
        Mockito.`when`(database.delete("attributes", "attribute_key = ? and mp_id = ?", arrayOf<String?>("color", "7"))).thenReturn(2)

        Assert.assertEquals(2, UserAttributesService.deleteAttributes(database, "color", 7L))
    }

    @Test
    fun testGetUserAttributesSinglesReadsTheSingleValuesIntoACaseInsensitiveMap() {
        Mockito
            .`when`(database.query("attributes", null, "is_list != ? and mp_id = ?", arrayOf<String?>("1", "7"), null, null, "attribute_key, created_time desc"))
            .thenReturn(cursor)
        stubRows(arrayOf("Color", "color", "size"), arrayOf("blue", "red", null))

        val attributes = UserAttributesService.getUserAttributesSingles(database, 7L)

        Assert.assertSame(String.CASE_INSENSITIVE_ORDER, attributes.comparator())
        Assert.assertEquals(mapOf("Color" to "red", "size" to null), attributes)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetUserAttributesSinglesLogsAFailedQueryAndReturnsAnEmptyMap() {
        val failure = IllegalStateException("query failed")
        Mockito
            .`when`(database.query("attributes", null, "is_list != ? and mp_id = ?", arrayOf<String?>("1", "7"), null, null, "attribute_key, created_time desc"))
            .thenThrow(failure)
        Mockito.mockStatic(Logger::class.java).use { logger ->
            Assert.assertTrue(UserAttributesService.getUserAttributesSingles(database, 7L).isEmpty())

            logger.verify { Logger.error(failure, "Error while querying user attributes: ", failure.toString()) }
        }
    }

    @Test
    fun testGetUserAttributesListsGroupsAdjacentRowsAndStartsANewListWhenTheKeyChangesCase() {
        Mockito
            .`when`(database.query("attributes", null, "is_list = ? and mp_id = ?", arrayOf<String?>("1", "7"), null, null, "attribute_key, created_time desc"))
            .thenReturn(cursor)
        stubRows(arrayOf("color", "color", "Color", "size"), arrayOf("blue", "red", "green", "10"))

        val attributes = UserAttributesService.getUserAttributesLists(database, 7L)

        Assert.assertSame(String.CASE_INSENSITIVE_ORDER, attributes.comparator())
        Assert.assertEquals(mapOf("color" to listOf("green"), "size" to listOf("10")), attributes)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetUserAttributesListsLogsAFailedQueryAndReturnsAnEmptyMap() {
        val failure = IllegalStateException("query failed")
        Mockito
            .`when`(database.query("attributes", null, "is_list = ? and mp_id = ?", arrayOf<String?>("1", "7"), null, null, "attribute_key, created_time desc"))
            .thenThrow(failure)
        Mockito.mockStatic(Logger::class.java).use { logger ->
            Assert.assertTrue(UserAttributesService.getUserAttributesLists(database, 7L).isEmpty())

            logger.verify { Logger.error(failure, "Error while querying user attribute lists: ", failure.toString()) }
        }
    }

    @Test
    fun testDeleteAllDeletesEveryRow() {
        UserAttributesService.deleteAll(database)

        Mockito.verify(database).delete("attributes", null, null)
    }

    private fun stubRows(keys: Array<String>, values: Array<String?>) {
        Mockito.`when`(cursor.getColumnIndexOrThrow("attribute_key")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("attribute_value")).thenReturn(1)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, *Array(keys.size - 1) { true }, false)
        Mockito.`when`(cursor.getString(0)).thenReturn(keys[0], *keys.copyOfRange(1, keys.size))
        Mockito.`when`(cursor.getString(1)).thenReturn(values[0], *values.copyOfRange(1, values.size))
    }
}
