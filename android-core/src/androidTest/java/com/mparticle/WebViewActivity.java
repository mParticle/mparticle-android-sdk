package com.mparticle;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.mparticle.test.R;

public class WebViewActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view_activity);

    }
}
