package com.mparticle.rokt

interface RoktLayoutDimensionCallBack {
    fun onHeightChanged(height: Int)
    fun onMarginChanged(start: Int, top: Int, end: Int, bottom: Int)
}