package com.mparticle.internal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.SparseArray;

import com.mparticle.segmentation.Segment;
import com.mparticle.segmentation.SegmentListener;
import com.mparticle.segmentation.SegmentMembership;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


class SegmentRetriever {

    private final SegmentDatabase mAudienceDB;
    private final MParticleApiClient mApiClient;

    SegmentRetriever(SegmentDatabase audienceDB, MParticleApiClient apiClient) {
        mAudienceDB = audienceDB;
        mApiClient = apiClient;
    }


    SegmentMembership queryAudiences(String endpointId) {
        SQLiteDatabase db = mAudienceDB.getReadableDatabase();

        String selection = null;
        String[] args = null;
        if (endpointId != null && endpointId.length() > 0) {
            selection = SegmentDatabase.SegmentTable.ENDPOINTS + " like ?";
            args = new String[1];
            args[0] = "%\"" + endpointId + "\"%";
        }

        Cursor audienceCursor = db.query(SegmentDatabase.SegmentTable.TABLE_NAME,
                null,
                selection,
                args,
                null,
                null,
                AUDIENCE_QUERY);
        SparseArray<Segment> audiences = new SparseArray<Segment>();

        StringBuilder keys = new StringBuilder("(");
        if (audienceCursor.getCount() > 0) {
            while (audienceCursor.moveToNext()) {
                int id = audienceCursor.getInt(audienceCursor.getColumnIndexOrThrow(SegmentDatabase.SegmentTable.SEGMENT_ID));

                Segment segment = new Segment(id,
                        audienceCursor.getString(audienceCursor.getColumnIndexOrThrow(SegmentDatabase.SegmentTable.NAME)),
                        audienceCursor.getString(audienceCursor.getColumnIndexOrThrow(SegmentDatabase.SegmentTable.ENDPOINTS)));
                audiences.put(id, segment);
                keys.append(id);
                keys.append(", ");
            }
            audienceCursor.close();

            keys.delete(keys.length() - 2, keys.length());
            keys.append(")");

            long currentTime = System.currentTimeMillis();
            Cursor membershipCursor = db.query(false,
                    SegmentDatabase.SegmentMembershipTable.TABLE_NAME,
                    MEMBERSHIP_QUERY_COLUMNS,
                    String.format(MEMBERSHIP_QUERY_SELECTION,
                            keys.toString(),
                            currentTime),
                    null,
                    null,
                    null,
                    MEMBERSHIP_QUERY_ORDER,
                    null);


            ArrayList<Segment> finalSegments = new ArrayList<Segment>();
            int currentId = -1;
            while (membershipCursor.moveToNext()) {
                int id = membershipCursor.getInt(1);
                if (id != currentId) {
                    currentId = id;
                    String action = membershipCursor.getString(2);
                    if (action.equals(Constants.Audience.ACTION_ADD)) {
                        finalSegments.add(audiences.get(currentId));
                    }
                }
            }
            membershipCursor.close();

            db.close();
            return new SegmentMembership(finalSegments);
        } else {
            return new SegmentMembership(new ArrayList<Segment>());
        }

    }

    private final static String AUDIENCE_QUERY = SegmentDatabase.SegmentTable.SEGMENT_ID + " desc";
    private final static String MEMBERSHIP_QUERY_ORDER = SegmentDatabase.SegmentMembershipTable.SEGMENT_ID + " desc, " + SegmentDatabase.SegmentMembershipTable.TIMESTAMP + " desc";
    private final static String[] MEMBERSHIP_QUERY_COLUMNS = new String[]
            {
                    SegmentDatabase.SegmentMembershipTable.ID,
                    SegmentDatabase.SegmentMembershipTable.SEGMENT_ID,
                    SegmentDatabase.SegmentMembershipTable.MEMBERSHIP_ACTION
            };
    private final static String MEMBERSHIP_QUERY_SELECTION = SegmentDatabase.SegmentMembershipTable.SEGMENT_ID + " in %s and " + SegmentDatabase.SegmentMembershipTable.TIMESTAMP + " < %d";



}
