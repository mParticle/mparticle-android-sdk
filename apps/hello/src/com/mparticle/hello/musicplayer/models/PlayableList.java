package com.mparticle.hello.musicplayer.models;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class PlayableList implements Parcelable {

	ArrayList<Playable> mList;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (mList != null) {
			dest.writeInt(mList.size());
			for (Playable p : mList) {
				p.writeToParcel(dest, flags);
			}
		} else {
			dest.writeInt(0);
		}
	}

    public static final Parcelable.Creator<PlayableList> CREATOR = new Parcelable.Creator<PlayableList>() {
		public PlayableList createFromParcel(Parcel in) {
		    return new PlayableList(in);
		}
		
		public PlayableList[] newArray(int size) {
		    return new PlayableList[size];
		}
	};
		
	private PlayableList(Parcel src) {
		int sz = src.readInt();
		mList = new ArrayList<Playable>(sz);
		if (mList == null) return;
		for (int i=0; i<sz; i++) {
			mList.add( Playable.CREATOR.createFromParcel( src ));
		}
	}
	
	public PlayableList() {
		mList = new ArrayList<Playable>(0);
	}
	
	public void add( Playable p ) {
		if (mList == null) {
			mList = new ArrayList<Playable>(0);
		}
		mList.add(p);
	}
	
	public int size() {
		if (mList == null) return 0;
		return mList.size();
	}
	
	public Playable get(int index) {
		if (mList == null) return null;
		return mList.get(index);
	}
	
	public PlayableList getClone() {
		Parcel p = Parcel.obtain();
		writeToParcel( p, 0 ); // write to a parcel
		p.setDataPosition(0); // rewind it
		PlayableList pl = PlayableList.CREATOR.createFromParcel( p );
		p.recycle();
		return pl;
	}
}
