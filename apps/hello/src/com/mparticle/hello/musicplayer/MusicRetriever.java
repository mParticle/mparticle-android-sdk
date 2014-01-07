/* This code was originally part of the Android Open Source sample apps: RandomMusicPlayer
 * It has been modified to remove the original user interface but preserves the ability
 * to run on early API's.  Some constants, package names, and globally available functions have been renamed
 * to be consistent with this app's naming conventions.
 * 
 * The original copyright was:
 *   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mparticle.hello.musicplayer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.mparticle.hello.musicplayer.models.Playable;
import com.mparticle.hello.musicplayer.models.PlayableList;

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a song, with its title and URI, upon
 * request.
 */
public class MusicRetriever {
    final String TAG = "MusicRetriever";

    ContentResolver mContentResolver;
    int m_nNowPlayingIndex = -1;
    boolean m_bPrepared;

    // the items (songs) we have queried
    PlayableList mItems = new PlayableList();

    public MusicRetriever(ContentResolver cr) {
        mContentResolver = cr;
    }

    /**
     * Loads music data. This method may take long, so be sure to call it asynchronously without
     * blocking the main thread.
     */
    public void prepare() {
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.i(TAG, "Querying media...");
        Log.i(TAG, "URI: " + uri.toString());

        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null,
                MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);
        Log.i(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
            return;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            Log.e(TAG, "Failed to move cursor to first row (no query results).");
            return;
        }

        Log.i(TAG, "Listing...");

        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int artColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

        Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
        Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));

        // add each song to mItems
        do {
            Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
            mItems.add(new Playable(
                    cur.getLong(idColumn),
                    cur.getString(artistColumn),
                    cur.getString(titleColumn),
                    cur.getString(albumColumn),
                    cur.getLong(artColumn),
                    cur.getLong(durationColumn)));
        } while (cur.moveToNext());
        m_bPrepared = true;
        Log.i(TAG, "Done querying media. MusicRetriever is ready.");
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    /** Returns the next item. If at end of list, starts over.
     * If there are no items available, returns null.. */
    public Playable getNextItem() {
    	if (!m_bPrepared) return null;
        if (mItems.size() <= 0) return null;
        if (m_nNowPlayingIndex >= (mItems.size()-1)) {
        	m_nNowPlayingIndex = 0;
        } else {
        	m_nNowPlayingIndex++;
        }
        return mItems.get(m_nNowPlayingIndex);
    }
    
    public PlayableList getList() {
    	if (!m_bPrepared || (mItems == null)) {
    		return null;
    	}
    	// clone the list
    	return mItems.getClone();
    }

    public void setNextItem(int next) {
    	if (mItems.size() == 0) {
    		m_nNowPlayingIndex = -1;
    	} else {
    		int nxt = next % mItems.size();
    		if (nxt == 0) {
    			nxt = mItems.size();
    		}
    		m_nNowPlayingIndex = nxt - 1;
    	}
    }
}
