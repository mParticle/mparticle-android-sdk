package com.mparticle.hello.musicplayer.models;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Playable objects are any media (local or remote) that this app can play.
 * Playable lists are used for playlists (list of Playables, possibly filtered by source or genre).
 * Playable lists are used for playqueues (list of Playables that are queued for playing sequentially).
 * Playable extends Object to gain serializability
 */

public class Playable implements Parcelable {
	
	private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    
	long id; // reference used in listviews
	String title;
	String artist;
	String album;
    long duration;
    String imageUrl;
    long albumId;
	Bundle metaData; // additional misc. information
	String url; // local or remote path to the media
	
	@Override
	public int describeContents() {	
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		if (title==null) dest.writeString(""); else dest.writeString(title);
		if (artist==null) dest.writeString(""); else dest.writeString(artist);
		if (album==null) dest.writeString(""); else dest.writeString(album);
		dest.writeLong(duration);
		if (imageUrl==null) dest.writeString(""); else dest.writeString(imageUrl);
		dest.writeLong(albumId);
		if (metaData==null) dest.writeBundle(new Bundle()); else dest.writeBundle(metaData);
		if (url==null) dest.writeString(""); else dest.writeString(url);
	}
	
    public static final Parcelable.Creator<Playable> CREATOR = new Parcelable.Creator<Playable>() {
		public Playable createFromParcel(Parcel in) {
		    return new Playable(in);
		}
		
		public Playable[] newArray(int size) {
		    return new Playable[size];
		}
	};
		
	private Playable(Parcel src) {
		id = src.readLong();
		title = src.readString();
		artist = src.readString();
		album = src.readString();
		duration = src.readLong();
		imageUrl = src.readString();
		albumId = src.readLong();
		metaData = src.readBundle();
		url = src.readString();
	}

    public Playable(long id, String artist, String title, String album, long albumId, long duration) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.albumId = albumId;
        if (albumId >= 0) {
	        Uri imageUri = ContentUris.withAppendedId(sArtworkUri, albumId);
	    	this.imageUrl = imageUri.toString();
	    } else {
	        this.imageUrl = "";
	    }
        this.duration = duration;
    }

    public Playable(long id, String artist, String title, String album, String albumUrl, long duration) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.albumId = -1;
	    this.imageUrl = albumUrl;
        this.duration = duration;
    }

    public long getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setImageUrl(String url) {
    	imageUrl = url;
    }
    
    public String getImageUrl() {
    	return imageUrl;
    }
    
    public long getDuration() {
        return duration;
    }

    public Uri getURI() {
        return ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

}
