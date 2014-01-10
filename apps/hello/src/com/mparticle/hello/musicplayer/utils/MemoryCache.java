package com.mparticle.hello.musicplayer.utils;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;

public class MemoryCache {
    private Map<String, SoftReference<Bitmap>> cache=Collections.synchronizedMap(new HashMap<String, SoftReference<Bitmap>>());
 
    public Bitmap get(String id){
    	id = fixKey(id);
        if(!cache.containsKey(id))
            return null;
        SoftReference<Bitmap> ref=cache.get(id);
        return ref.get();
    }
 
    public void put(String id, Bitmap bitmap){
    	id = fixKey(id);
        cache.put(id, new SoftReference<Bitmap>(bitmap));
    }
 
    public void clear() {
        cache.clear();
    }
    
    private String fixKey( String id ) {
    	if (id.startsWith("content://")) {
    		// convert to a usable file path
    		id = id.replace("://", "__");
    		id = id.replaceAll("/", "_");
    	}
    	return id;
    }
}