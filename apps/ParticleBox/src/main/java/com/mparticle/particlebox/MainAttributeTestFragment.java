package com.mparticle.particlebox;

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

import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/22/14.
 */
public class MainAttributeTestFragment extends Fragment implements View.OnClickListener {

    protected Button button1, button2;
    protected EditText editText1, editText2, editText3;

    public MainAttributeTestFragment() {
        super();
    }

    private static final String ARG_SECTION_NUMBER = "section_number";

    public static MainAttributeTestFragment newInstance(int sectionNumber) {
        MainAttributeTestFragment fragment = new MainAttributeTestFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_attributes, container, false);
        button1 = (Button) v.findViewById(R.id.button);
        button2 = (Button) v.findViewById(R.id.button2);
        editText1 = (EditText) v.findViewById(R.id.edittext);
        editText2 = (EditText) v.findViewById(R.id.edittext2);
        editText3 = (EditText) v.findViewById(R.id.edittext3);
        editText1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                button1.setEnabled(editText1.getText().length() > 0);
            }
        });
        editText3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                button2.setEnabled(editText3.getText().length() > 0);
            }
        });

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                MParticle.getInstance().setUserAttribute(editText1.getText().toString(), editText2.getText().toString());
                Toast.makeText(v.getContext(), "User attribute set.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button2:
                MParticle.getInstance().setUserTag(editText3.getText().toString());
                Toast.makeText(v.getContext(), "User tag set.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
