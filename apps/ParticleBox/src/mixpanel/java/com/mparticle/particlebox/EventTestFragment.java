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

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.mparticle.MParticle;
import com.mparticle.particlebox.MainEventTestFragment;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sdozor on 1/7/14.
 */
public class EventTestFragment extends MainEventTestFragment implements View.OnClickListener {

    private MixpanelAPI mixpanel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mixpanel =
                MixpanelAPI.getInstance(activity, "b66214bab597da0085dfc3bcc1e44929");

        mixpanel.getPeople().initPushHandling("217134478361");
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.button:
                try{
                    JSONObject props = new JSONObject();
                    props.put("Label", eventLabel.getText().toString());
                    props.put("Value", eventValue.getText().toString());
                    props.put("Category", spinner.getSelectedItem().toString());
                    mixpanel.track(viewEditText.getText().toString(), props);
                }catch (Exception e){

                }
                break;
            case R.id.button2:
                try{
                    JSONObject props = new JSONObject();
                    props.put("name", screenEditText.getText().toString());
                    mixpanel.track("Screen view", props);
                }catch (Exception e){

                }
                break;
            case R.id.button3:
                Toast.makeText(v.getContext(), "Not implemented for Mixpanel...", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button5:
                Toast.makeText(v.getContext(), "Not implemented for Mixpanel...", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
