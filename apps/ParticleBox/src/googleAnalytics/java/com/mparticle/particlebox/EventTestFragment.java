package com.mparticle.particlebox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by sdozor on 1/7/14.
 */
public class EventTestFragment extends MainEventTestFragment implements View.OnClickListener {

    private Tracker tracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        tracker = GoogleAnalytics.getInstance(activity).newTracker("UA-46924309-4");
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.button:
                tracker.send(new HitBuilders.EventBuilder(spinner.getSelectedItem().toString(),
                        viewEditText.getText().toString()).setLabel(
                        eventLabel.getText().toString()).setValue(
                        Long.parseLong(eventValue.getText().toString())).
                        build());
                break;
            case R.id.button2:
                tracker.send(new HitBuilders.AppViewBuilder().build());
                break;
            case R.id.button3:
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription(errorEditText.getText().toString()).setFatal(false).build());
                break;
            case R.id.button5:
                tracker.send(new HitBuilders.TimingBuilder(
                        timingCategory.getSelectedItem().toString(),
                        timingTitle.getText().toString(),
                        Long.parseLong(timingLength.getText().toString()))
                        .build());
                break;
        }
    }
}
