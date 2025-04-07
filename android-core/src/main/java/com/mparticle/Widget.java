package com.mparticle;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class Widget extends FrameLayout {

    private static final int OUT_OF_SYNC_HEIGHT_DIFF = 1;
    private final int lastHeight = 0;

    public Widget(Context context) {
        super(context);
    }

    public Widget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Widget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}