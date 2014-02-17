package com.mparticle.particlebox;

import android.app.Activity;
import android.os.Bundle;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sdozor on 1/7/14.
 */
public class MainEventTestFragment extends Fragment implements View.OnClickListener {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    protected Spinner spinner, exceptionSpinner, timingCategory, handledExceptionSpinner;
    protected EditText viewEditText, screenEditText, errorEditText, unhandleErrorEditText, eventValue, eventLabel, timingTitle, timingLabel, timingLength;
    protected Button eventButton, screenButton, handledErrorsButton, unhandledErrorsButton, timingButton;


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MainEventTestFragment newInstance(int sectionNumber) {
        MainEventTestFragment fragment = new EventTestFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public MainEventTestFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("eventlabel", viewEditText.getText());
        outState.putCharSequence("screenname", viewEditText.getText());
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_events, container, false);

        spinner = (Spinner) v.findViewById(R.id.spinner);
        timingCategory = (Spinner) v.findViewById(R.id.timingCategory);
        exceptionSpinner = (Spinner) v.findViewById(R.id.spinner2);
        handledExceptionSpinner = (Spinner) v.findViewById(R.id.handledExceptionSpinner);
        eventLabel = (EditText) v.findViewById(R.id.eventLabel);
        eventValue = (EditText) v.findViewById(R.id.eventValue);

        timingCategory.setAdapter(new ArrayAdapter<MParticle.EventType>(
                v.getContext(),
                android.R.layout.simple_list_item_1,
                MParticle.EventType.values()));
        timingTitle = (EditText)v.findViewById(R.id.timingTitle);
        timingLabel = (EditText)v.findViewById(R.id.timingLabel);
        timingLength = (EditText)v.findViewById(R.id.timingLength);
        spinner.setAdapter(new ArrayAdapter<MParticle.EventType>(
                v.getContext(),
                android.R.layout.simple_list_item_1,
                MParticle.EventType.values()));
        spinner.setSelection(8);

        exceptionSpinner.setAdapter(new ArrayAdapter<String>(
                v.getContext(),
                android.R.layout.simple_list_item_1,
                v.getResources().getStringArray(R.array.exceptions)));
        handledExceptionSpinner.setAdapter(new ArrayAdapter<String>(
                v.getContext(),
                android.R.layout.simple_list_item_1,
                v.getResources().getStringArray(R.array.exceptions)));

        eventButton = (Button) v.findViewById(R.id.button);
        screenButton = (Button) v.findViewById(R.id.button2);
        timingButton = (Button) v.findViewById(R.id.button5);
        timingButton.setOnClickListener(this);
        eventButton.setOnClickListener(this);
        screenButton.setOnClickListener(this);
        handledErrorsButton = (Button) v.findViewById(R.id.button3);
        unhandledErrorsButton = (Button) v.findViewById(R.id.button4);
        handledErrorsButton.setOnClickListener(this);
        unhandledErrorsButton.setOnClickListener(this);
        viewEditText = (EditText) v.findViewById(R.id.edittext);
        viewEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                eventButton.setEnabled(viewEditText.getText().length() > 0);
            }
        });
        screenEditText = (EditText) v.findViewById(R.id.edittext2);
        screenEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                screenButton.setEnabled(screenEditText.getText().length() > 0);
            }
        });
        errorEditText = (EditText) v.findViewById(R.id.edittext3);
        errorEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                handledErrorsButton.setEnabled(errorEditText.getText().length() > 0);
            }
        });
        if (savedInstanceState != null) {
            viewEditText.setText(savedInstanceState.getCharSequence("eventlabel"));
        }

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
        String toastText = "Message logged.";
        switch (v.getId()) {
            case R.id.button:
                Map eventInfo = new HashMap<String, String>();
                eventInfo.put("Label", eventLabel.getText().toString());
                eventInfo.put("Value", eventValue.getText().toString());
                MParticle.getInstance().logEvent(viewEditText.getText().toString(),
                        (MParticle.EventType) spinner.getSelectedItem(), eventInfo);
                break;
            case R.id.button2:
                MParticle.getInstance().logScreen(screenEditText.getText().toString());
                break;
            case R.id.button3:
                try{
                    switch (handledExceptionSpinner.getSelectedItemPosition()) {
                        case 0:
                            npeRunnable.run();
                            break;
                        case 1:
                            ioobeRunnable.run();
                        }
                }catch(Exception e){
                    Map<String, String> attributes = new HashMap<String, String>();
                    attributes.put("Attribute", "Some Attribute");
                    MParticle.getInstance().logException(e, attributes, errorEditText.getText().toString());
                }
                break;
            case R.id.button4:
                toastText = "Crashing...";
                switch (exceptionSpinner.getSelectedItemPosition()) {
                    case 0:
                        v.postDelayed(npeRunnable, 2000);
                        break;
                    case 1:
                        v.postDelayed(ioobeRunnable, 2000);
                }
                break;
            case R.id.button5:
                Map timingEventInfo = new HashMap<String, String>();
                timingEventInfo.put("Label", timingLabel.getText().toString());
                MParticle.getInstance().logEvent(timingTitle.getText().toString(),
                        (MParticle.EventType) spinner.getSelectedItem(),
                        timingEventInfo,
                        Long.parseLong(timingLength.getText().toString()));

        }
        Toast.makeText(v.getContext(), toastText, 300).show();
    }

    protected Runnable npeRunnable = new Runnable() {

        @Override
        public void run() {
            String someString = null;
            someString.contains("");
        }
    };

    protected Runnable ioobeRunnable = new Runnable() {

        @Override
        public void run() {
            int[] someArray = new int[2];
            int someValue = someArray[500];
        }
    };

}
