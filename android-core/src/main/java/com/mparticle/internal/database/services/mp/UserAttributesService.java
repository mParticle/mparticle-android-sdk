package com.mparticle.internal.database.services.mp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.Logger;
import com.mparticle.internal.database.tables.mp.UserAttributesTable;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class UserAttributesService extends UserAttributesTable {
    public static int deleteAttributes(SQLiteDatabase database, String containerKey) {
        String[] deleteWhereArgs = {containerKey};
        return database.delete(UserAttributesTableColumns.TABLE_NAME, UserAttributesTableColumns.ATTRIBUTE_KEY + " = ?", deleteWhereArgs);
    }

    public static void insertAttribute(SQLiteDatabase db, String key, String attributeValue, long time, boolean isList) {
        ContentValues values = new ContentValues();
        values.put(UserAttributesTableColumns.ATTRIBUTE_KEY, key);
        values.put(UserAttributesTableColumns.ATTRIBUTE_VALUE, attributeValue);
        values.put(UserAttributesTableColumns.IS_LIST, isList);
        values.put(UserAttributesTableColumns.CREATED_AT, time);
        db.insert(UserAttributesTableColumns.TABLE_NAME, null, values);
    }

    public static int deleteOldAttributes(SQLiteDatabase db, String key) {
        String[] deleteWhereArgs = {key};
        return db.delete(UserAttributesTableColumns.TABLE_NAME, UserAttributesTableColumns.ATTRIBUTE_KEY + " = ?", deleteWhereArgs);
    }

    public static TreeMap<String, String> getUserAttributesSingles(SQLiteDatabase db) {
        TreeMap<String, String> attributes = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        Cursor cursor = null;
        try {
            String[] args =  {"1"};

            cursor = db.query(UserAttributesTableColumns.TABLE_NAME, null, UserAttributesTableColumns.IS_LIST + " != ?", args, null, null, UserAttributesTableColumns.ATTRIBUTE_KEY + ", "+ UserAttributesTableColumns.CREATED_AT +" desc");
            int keyIndex = cursor.getColumnIndex(UserAttributesTableColumns.ATTRIBUTE_KEY);
            int valueIndex = cursor.getColumnIndex(UserAttributesTableColumns.ATTRIBUTE_VALUE);
            while (cursor.moveToNext()) {
                attributes.put(cursor.getString(keyIndex), cursor.getString(valueIndex));
            }
        }catch (Exception e) {
            Logger.error(e, "Error while querying user attributes: ", e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return attributes;
    }

    public static TreeMap<String, List<String>> getUserAttributesLists(SQLiteDatabase db) {
        TreeMap<String, List<String>> attributes = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        Cursor cursor = null;
        try {
            String[] args =  {"1"};
            cursor = db.query(UserAttributesTableColumns.TABLE_NAME, null, UserAttributesTableColumns.IS_LIST + " = ?", args, null, null, UserAttributesTableColumns.ATTRIBUTE_KEY + ", "+ UserAttributesTableColumns.CREATED_AT +" desc");
            int keyIndex = cursor.getColumnIndex(UserAttributesTableColumns.ATTRIBUTE_KEY);
            int valueIndex = cursor.getColumnIndex(UserAttributesTableColumns.ATTRIBUTE_VALUE);
            String previousKey = null;
            List<String> currentList = null;
            while (cursor.moveToNext()) {
                String currentKey = cursor.getString(keyIndex);
                if (!currentKey.equals(previousKey)){
                    previousKey = currentKey;
                    currentList = new ArrayList<String>();
                    attributes.put(currentKey, currentList);
                }
                attributes.get(currentKey).add(cursor.getString(valueIndex));
            }
        }catch (Exception e) {
            Logger.error(e, "Error while querying user attribute lists: ", e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return attributes;
    }
}
