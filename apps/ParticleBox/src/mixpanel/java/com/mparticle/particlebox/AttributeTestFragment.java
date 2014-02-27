package com.mparticle.particlebox;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/22/14.
 */
public class AttributeTestFragment extends MainAttributeTestFragment implements View.OnClickListener {
    private MixpanelAPI mixpanel;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mixpanel =
                MixpanelAPI.getInstance(activity, "b66214bab597da0085dfc3bcc1e44929");
    }
    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.button:
                //
                break;
            case R.id.button2:
                //
                break;
            case R.id.button3:
                mixpanel.identify(editText4.getText().toString());
                break;
        }
    }
}
