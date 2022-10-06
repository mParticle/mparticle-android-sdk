package com.mparticle

import android.app.Activity
import android.os.Bundle
import com.mparticle.test.R

class WebViewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_view_activity)
    }
}