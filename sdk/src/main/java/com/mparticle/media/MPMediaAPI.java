package com.mparticle.media;

import android.content.Context;

/**
 * Utility class to interact with the MParticle Media APIs. Do not directly instantiate this.
 *
 * @see com.mparticle.MParticle#Media()
 *
 */
public class MPMediaAPI {
    private final MediaCallbacks mCallbacks;
    private final Context mContext;
    private boolean mAudioPlaying = false;

    private MPMediaAPI(){
        mContext = null;
        mCallbacks = null;
    }


    /**
     * @hide
     */
    public MPMediaAPI(Context context, MediaCallbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
    }

    /**
     * Use this method to inform the SDK that there is audio playing. In the case where a
     * user navigates away from your app, but your app is playing music in the background,
     * using this method will ensure that the mParticle SDK does not end the user's session prematurely.
     * A user's session will be considered active as long as audio is playing, so be sure to use this method
     * both to signal when audio starts, as well as when it pauses or stops.
     *
     * @param playing Is your app currently playing music for the user.
     */
    public void setAudioPlaying(boolean playing){
        mAudioPlaying = playing;
        if (playing){
            mCallbacks.onAudioPlaying();
        }else{
            mCallbacks.onAudioStopped();
        }
    }

    public boolean getAudioPlaying() {
        return mAudioPlaying;
    }
}
