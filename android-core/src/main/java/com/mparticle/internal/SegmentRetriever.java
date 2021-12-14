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

    void fetchSegments(long timeout, final String endpointId, final SegmentListener listener) {
        new SegmentTask(timeout, endpointId, listener).execute();
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

            keys.delete(keys.length()-2, keys.length());
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
    private final static String MEMBERSHIP_QUERY_SELECTION = SegmentDatabase.SegmentMembershipTable.SEGMENT_ID+ " in %s and " + SegmentDatabase.SegmentMembershipTable.TIMESTAMP + " < %d";

    private void insertAudiences(JSONObject audiences) throws JSONException {
        SQLiteDatabase db = mAudienceDB.getWritableDatabase();
        JSONArray audienceList = audiences.getJSONArray(Constants.Audience.API_AUDIENCE_LIST);
        db.beginTransaction();
        boolean success = false;
        try {
            db.delete(SegmentDatabase.SegmentMembershipTable.TABLE_NAME, null, null);
            db.delete(SegmentDatabase.SegmentTable.TABLE_NAME, null, null);
            for (int i = 0; i < audienceList.length(); i++) {
                ContentValues audienceRow = new ContentValues();
                JSONObject audience = audienceList.getJSONObject(i);
                int id = audience.getInt(Constants.Audience.API_AUDIENCE_ID);
                String name = audience.getString(Constants.Audience.API_AUDIENCE_NAME);
                String endPointIds = audience.getJSONArray(Constants.Audience.API_AUDIENCE_ENDPOINTS).toString();
                audienceRow.put(SegmentDatabase.SegmentTable.SEGMENT_ID, id);
                audienceRow.put(SegmentDatabase.SegmentTable.NAME, name);
                audienceRow.put(SegmentDatabase.SegmentTable.ENDPOINTS, endPointIds);
                db.insert(SegmentDatabase.SegmentTable.TABLE_NAME, null, audienceRow);
                JSONArray memberships = audience.getJSONArray(Constants.Audience.API_AUDIENCE_MEMBERSHIPS);
                for (int j = 0; j < memberships.length(); j++) {
                    ContentValues membershipRow = new ContentValues();
                    membershipRow.put(SegmentDatabase.SegmentMembershipTable.SEGMENT_ID, id);
                    membershipRow.put(SegmentDatabase.SegmentMembershipTable.MEMBERSHIP_ACTION, memberships.getJSONObject(j).getString(Constants.Audience.API_AUDIENCE_ACTION));
                    membershipRow.put(SegmentDatabase.SegmentMembershipTable.TIMESTAMP, memberships.getJSONObject(j).optLong(Constants.Audience.API_AUDIENCE_MEMBERSHIP_TIMESTAMP, 0));
                    db.insert(SegmentDatabase.SegmentMembershipTable.TABLE_NAME, null, membershipRow);
                }
            }
            success = true;
        } catch (Exception e) {
            Logger.debug("Failed to insert audiences: " + e.getMessage());
        }finally {
            if (success) {
                db.setTransactionSuccessful();
            }
            db.endTransaction();
            db.close();
        }

    }
    class SegmentTask extends AsyncTask<Void, Void, SegmentMembership> {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String endpointId;
        SegmentListener listener;
        long timeout;
        SegmentTask(long timeout, String endpointId, SegmentListener listener) {
            this.timeout = timeout;
            this.endpointId = endpointId;
            this.listener = listener;
        }
        @Override
        protected SegmentMembership doInBackground(Void... params) {
            FutureTask<Boolean> futureTask1 = new FutureTask<Boolean>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    JSONObject audiences = mApiClient.fetchAudiences();
                    if (audiences != null) {
                        insertAudiences(audiences);
                    }
                    return audiences != null;
                }
            });

            executor.execute(futureTask1);
            try {
                futureTask1.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            executor.shutdown();
            return queryAudiences(endpointId);
        }

        @Override
        protected void onPostExecute(SegmentMembership segmentMembership) {
            listener.onSegmentsRetrieved(segmentMembership);
        }
    }
}
