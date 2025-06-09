package com.mparticle.rokt

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class RoktEmbeddedView : FrameLayout {
    var dimensionCallBack: RoktLayoutDimensionCallBack? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}