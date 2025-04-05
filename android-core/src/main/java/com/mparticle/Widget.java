package com.mparticle;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class Widget  extends FrameLayout {

    private RoktWidgetDimensionCallback dimensionCallback;
    private int lastHeight = 0;

    private static final int OUT_OF_SYNC_HEIGHT_DIFF = 1;

    public Widget(Context context) {
        super(context);
    }

    public Widget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Widget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDimensionCallback(RoktWidgetDimensionCallback callback) {
        this.dimensionCallback = callback;
    }

    // You can call this method manually when the height changes
    public void notifyHeightChanged(int newHeight) {
        if (Math.abs(lastHeight - newHeight) >= OUT_OF_SYNC_HEIGHT_DIFF) {
            lastHeight = newHeight;
            if (dimensionCallback != null) {
                dimensionCallback.onHeightChanged(newHeight);
            }
        }
    }

    // You can call this manually when padding/margin is updated
    public void notifyPaddingChanged(int left, int top, int right, int bottom) {
        if (dimensionCallback != null) {
            dimensionCallback.onMarginChanged(left, top, right, bottom);
        }
    }

    public interface RoktWidgetDimensionCallback {
        void onHeightChanged(int height);

        void onMarginChanged(int left, int top, int right, int bottom);
    }
}