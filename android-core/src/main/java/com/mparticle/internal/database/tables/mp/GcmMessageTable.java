package com.mparticle.internal.database.tables.mp;

public class GcmMessageTable extends MpIdDependentTable {
    public static final String APPSTATE = GcmMessageTableColumns.APPSTATE;
    public static final String CONTENT_ID = GcmMessageTableColumns.CONTENT_ID;

    @Override
    public String getTableName() {
        return GcmMessageTableColumns.TABLE_NAME;
    }

    protected interface GcmMessageTableColumns {
        String CONTENT_ID = "content_id";
        String CAMPAIGN_ID = "campaign_id";
        String TABLE_NAME = "gcm_messages";
        String PAYLOAD = "payload";
        String CREATED_AT = "message_time";
        String DISPLAYED_AT = "displayed_time";
        String EXPIRATION = "expiration";
        String BEHAVIOR = "behavior";
        String APPSTATE = "appstate";
        String MP_ID = MpIdDependentTable.MP_ID;
        int PROVIDER_CONTENT_ID = -1;
    }

    static String getAddMpIdColumnString(String defaulValue) {
        return MParticleDatabaseHelper.addIntegerColumnString(GcmMessageTableColumns.TABLE_NAME, GcmMessageTableColumns.MP_ID, defaulValue);
    }

    static final String CREATE_GCM_MSG_DDL =
            "CREATE TABLE IF NOT EXISTS " + GcmMessageTableColumns.TABLE_NAME + " (" + GcmMessageTableColumns.CONTENT_ID +
                    " INTEGER PRIMARY KEY, " +
                    GcmMessageTableColumns.PAYLOAD + " TEXT NOT NULL, " +
                    GcmMessageTableColumns.APPSTATE + " TEXT NOT NULL, " +
                    GcmMessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    GcmMessageTableColumns.EXPIRATION + " INTEGER NOT NULL, " +
                    GcmMessageTableColumns.BEHAVIOR + " INTEGER NOT NULL," +
                    GcmMessageTableColumns.CAMPAIGN_ID + " TEXT NOT NULL, " +
                    GcmMessageTableColumns.DISPLAYED_AT + " INTEGER NOT NULL, " +
                    GcmMessageTableColumns.MP_ID + " INTEGER " +
                    ");";
}
