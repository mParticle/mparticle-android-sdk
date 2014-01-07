package com.mparticle.hello.musicplayer.utils;

import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
    
    public static String formatSongDuration( long msecs ) {
    	// format msecs into hrs:mins:secs
    	int fract = (int)(msecs % 1000);
    	msecs /= 1000;
    	int secs = (int)(msecs % 60);
    	msecs /= 60;
    	int mins = (int)(msecs % 60);
    	int hrs = (int)(msecs / 60);
    	
    	String formatted = "";
    	if (hrs > 0) {
    		formatted = String.valueOf(hrs) + ":";
    	}
    	if ((hrs > 0) && (mins < 10)) {
    		formatted += "0";
    	}
    	formatted += String.valueOf(mins) + ":";
    	if ((formatted.length() > 0) && (secs < 10)) {
    		formatted += "0";
    	}
    	formatted += String.valueOf(secs);
    	return formatted;
    }
}