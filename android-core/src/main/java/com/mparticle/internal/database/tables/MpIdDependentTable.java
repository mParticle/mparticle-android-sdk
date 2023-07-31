package com.mparticle.internal.database.tables;

import android.content.ContentValues;

import com.mparticle.internal.database.MPDatabase;

public abstract class MpIdDependentTable {
    public static final String MP_ID = "mp_id";

    public abstract String getTableName();

    public void updateMpId(MPDatabase database, long oldMpId, long newMpId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MP_ID, newMpId);
        database.update(getTableName(), contentValues, MP_ID + " = ?", new String[]{String.valueOf(oldMpId)});
    }
}
