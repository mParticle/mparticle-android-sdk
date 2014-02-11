package com.mparticle.particlebox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/22/14.
 */
public class MainAttributeTestFragment extends Fragment implements View.OnClickListener {

    protected EditText editText1, editText2, editText3, editText4;
    protected Spinner identitySpinner;
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
        v.findViewById(R.id.button).setOnClickListener(this);
        v.findViewById(R.id.button2).setOnClickListener(this);
        v.findViewById(R.id.button3).setOnClickListener(this);
        editText1 = (EditText) v.findViewById(R.id.edittext);
        editText2 = (EditText) v.findViewById(R.id.edittext2);
        editText3 = (EditText) v.findViewById(R.id.edittext3);
        editText4 = (EditText) v.findViewById(R.id.edittext4);

        identitySpinner = (Spinner) v.findViewById(R.id.spinner);

        identitySpinner.setAdapter(new ArrayAdapter<MParticle.IdentityType>(
                v.getContext(),
                android.R.layout.simple_list_item_1,
                MParticle.IdentityType.values()));

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
            case R.id.button3:
                MParticle.getInstance().setUserIdentity(editText4.getText().toString(), (MParticle.IdentityType)identitySpinner.getSelectedItem());
                Toast.makeText(v.getContext(), "User identity set.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
