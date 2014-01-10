package com.mparticle.droidparticles;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.mparticle.MParticleAPI;

/**
 * Created by sdozor on 1/7/14.
 */
public class EventTestFragment extends Fragment implements View.OnClickListener {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private Spinner spinner;
    private EditText editText;
    private CheckBox checkBox;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static EventTestFragment newInstance(int sectionNumber) {
        EventTestFragment fragment = new EventTestFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public EventTestFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_events, container, false);
        spinner = (Spinner)v.findViewById(R.id.spinner);
        v.findViewById(R.id.button).setOnClickListener(this);
        editText = (EditText)v.findViewById(R.id.edittext);
        checkBox = (CheckBox)v.findViewById(R.id.checkbox);
        spinner.setAdapter(new ArrayAdapter<MParticleAPI.EventType>(
                v.getContext(),
                android.R.layout.simple_list_item_1,
                MParticleAPI.EventType.values()));
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((ParticleActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onClick(View v) {
        if (checkBox.isChecked()){
            MParticleAPI.getInstance(v.getContext()).logScreenView(editText.getText().toString());
        }else{
            MParticleAPI.getInstance(v.getContext()).logEvent(editText.getText().toString(), (MParticleAPI.EventType)spinner.getSelectedItem());
        }
    }
}
