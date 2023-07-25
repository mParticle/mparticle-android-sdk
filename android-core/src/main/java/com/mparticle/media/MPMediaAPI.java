package com.mparticle.media;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class to interact with the mParticle Media APIs. Do not directly instantiate this.
 *
 * @see com.mparticle.MParticle#Media()
 */
public class MPMediaAPI {
    private final MediaCallbacks mCallbacks;
    private final Context mContext;
    private AtomicBoolean mAudioPlaying = new AtomicBoolean(false);

    private MPMediaAPI() {
        mContext = null;
        mCallbacks = null;
    }

    public MPMediaAPI(@Nullable Context context, @NonNull MediaCallbacks callbacks) {
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
    public void setAudioPlaying(boolean playing) {
        mAudioPlaying.set(playing);
        if (playing) {
            mCallbacks.onAudioPlaying();
        } else {
            mCallbacks.onAudioStopped();
        }
    }

    public boolean getAudioPlaying() {
        return mAudioPlaying.get();
    }
}
