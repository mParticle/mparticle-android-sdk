package com.mparticle.internal.database.services;

import android.content.ContentValues;
import android.database.Cursor;

import com.mparticle.internal.Logger;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.tables.BreadcrumbTable;
import com.mparticle.internal.database.tables.UserAttributesTable;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class UserAttributesService extends UserAttributesTable {

    public static void insertAttribute(MPDatabase db, String key, String attributeValue, long time, boolean isList, long mpId) {
        ContentValues values = new ContentValues();
        values.put(UserAttributesTableColumns.MP_ID, mpId);
        values.put(UserAttributesTableColumns.ATTRIBUTE_KEY, key);
        values.put(UserAttributesTableColumns.ATTRIBUTE_VALUE, attributeValue);
        values.put(UserAttributesTableColumns.IS_LIST, isList);
        values.put(UserAttributesTableColumns.CREATED_AT, time);
        db.insert(UserAttributesTableColumns.TABLE_NAME, null, values);
    }

    public static int deleteAttributes(MPDatabase db, String key, long mpId) {
        String[] deleteWhereArgs = {key, String.valueOf(mpId)};
        return db.delete(UserAttributesTableColumns.TABLE_NAME, UserAttributesTableColumns.ATTRIBUTE_KEY + " = ? and " + UserAttributesTableColumns.MP_ID + " = ?", deleteWhereArgs);
    }

    public static TreeMap<String, String> getUserAttributesSingles(MPDatabase db, long mpId) {
        TreeMap<String, String> attributes = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        Cursor cursor = null;
        try {
            String[] args = {"1", String.valueOf(mpId)};

            cursor = db.query(UserAttributesTableColumns.TABLE_NAME, null, UserAttributesTableColumns.IS_LIST + " != ? and " + UserAttributesTableColumns.MP_ID + " = ?", args, null, null, UserAttributesTableColumns.ATTRIBUTE_KEY + ", " + UserAttributesTableColumns.CREATED_AT + " desc");
            int keyIndex = cursor.getColumnIndexOrThrow(UserAttributesTableColumns.ATTRIBUTE_KEY);
            int valueIndex = cursor.getColumnIndexOrThrow(UserAttributesTableColumns.ATTRIBUTE_VALUE);
            while (cursor.moveToNext()) {
                attributes.put(cursor.getString(keyIndex), cursor.getString(valueIndex));
            }
        } catch (Exception e) {
            Logger.error(e, "Error while querying user attributes: ", e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return attributes;
    }

    public static TreeMap<String, List<String>> getUserAttributesLists(MPDatabase db, long mpId) {
        TreeMap<String, List<String>> attributes = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        Cursor cursor = null;
        try {
            String[] args = {"1", String.valueOf(mpId)};
            cursor = db.query(UserAttributesTableColumns.TABLE_NAME, null, UserAttributesTableColumns.IS_LIST + " = ? and " + UserAttributesTableColumns.MP_ID + " = ?", args, null, null, UserAttributesTableColumns.ATTRIBUTE_KEY + ", " + UserAttributesTableColumns.CREATED_AT + " desc");
            int keyIndex = cursor.getColumnIndexOrThrow(UserAttributesTableColumns.ATTRIBUTE_KEY);
            int valueIndex = cursor.getColumnIndexOrThrow(UserAttributesTableColumns.ATTRIBUTE_VALUE);
            String previousKey = null;
            List<String> currentList = null;
            while (cursor.moveToNext()) {
                String currentKey = cursor.getString(keyIndex);
                if (!currentKey.equals(previousKey)) {
                    previousKey = currentKey;
                    currentList = new ArrayList<String>();
                    attributes.put(currentKey, currentList);
                }
                attributes.get(currentKey).add(cursor.getString(valueIndex));
            }
        } catch (Exception e) {
            Logger.error(e, "Error while querying user attribute lists: ", e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return attributes;
    }

    public static void deleteAll(MPDatabase db) {
        db.delete(UserAttributesTableColumns.TABLE_NAME, null, null);
    }
}
