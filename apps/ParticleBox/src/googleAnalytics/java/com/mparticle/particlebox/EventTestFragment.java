package com.mparticle.particlebox;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.mparticle.MParticle;
import com.mparticle.particlebox.MainEventTestFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sdozor on 1/7/14.
 */
public class EventTestFragment extends MainEventTestFragment implements View.OnClickListener {

    private EasyTracker tracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        tracker = EasyTracker.getInstance(activity);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.button:
                tracker.send(MapBuilder
                        .createEvent(spinner.getSelectedItem().toString(),
                                viewEditText.getText().toString(),
                                eventLabel.getText().toString(),
                                Long.parseLong(eventValue.getText().toString())).
                                build());
                break;
            case R.id.button2:
                tracker.set(Fields.SCREEN_NAME, screenEditText.getText().toString());
                tracker.send(MapBuilder
                        .createAppView()
                        .build());
                break;
            case R.id.button3:
                tracker.send(MapBuilder.createException(errorEditText.getText().toString(), false).build());
                break;
            case R.id.button4:
                tracker.send(MapBuilder.createTiming(
                        timingCategory.getSelectedItem().toString(),
                        Long.parseLong(timingLength.getText().toString()),
                        timingTitle.getText().toString(),
                        timingLabel.getText().toString()).build());
                break;
        }
    }
}
