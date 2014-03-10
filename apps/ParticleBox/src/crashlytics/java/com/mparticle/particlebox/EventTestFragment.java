package com.mparticle.particlebox;

import android.view.View;
import android.widget.Toast;

/**
 * Created by sdozor on 1/7/14.
 */
public class EventTestFragment extends MainEventTestFragment implements View.OnClickListener {

    @Override
    public void onClick(View v) {
        super.onClick(v);
        Toast.makeText(v.getContext(), "Not implemented for Crashlytics...", Toast.LENGTH_SHORT).show();

    }
}
