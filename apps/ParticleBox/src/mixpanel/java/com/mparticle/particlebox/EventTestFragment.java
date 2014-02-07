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

import com.mparticle.MParticle;
import com.mparticle.particlebox.MainEventTestFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sdozor on 1/7/14.
 */
public class EventTestFragment extends MainEventTestFragment implements View.OnClickListener {


    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.button:
                break;
            case R.id.button2:
                break;
            case R.id.button3:
                break;
            case R.id.button4:
                break;
        }
    }
}
