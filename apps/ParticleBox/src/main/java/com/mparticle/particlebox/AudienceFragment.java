package com.mparticle.particlebox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mparticle.AudienceListener;
import com.mparticle.MParticle;
import com.mparticle.com.mparticle.audience.Audience;
import com.mparticle.com.mparticle.audience.AudienceMembership;

/**
 * Created by sdozor on 2/25/14.
 */
public class AudienceFragment extends Fragment implements AudienceListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String ARG_SECTION_NUMBER = "section_number";
    TextView  timeoutTextView;
    ListView audienceList;
    SeekBar seekbar;
    EditText endpointText;
    long time;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.audience_fragment, container, false);
        audienceList = (ListView)v.findViewById(R.id.audiences);
        timeoutTextView = (TextView)v.findViewById(R.id.timeoutText);
        seekbar = (SeekBar)v.findViewById(R.id.seekBar);
        endpointText = (EditText)v.findViewById(R.id.endpoint);
        seekbar.setOnSeekBarChangeListener(this);
        timeoutTextView.setText("Timeout: " + Integer.toString(seekbar.getProgress()) + " milliseconds");
        v.findViewById(R.id.button).setOnClickListener(this);
        return v;
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AudienceFragment newInstance(int sectionNumber) {
        AudienceFragment fragment = new AudienceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAudiencesRetrieved(AudienceMembership audienceMembership) {
        Toast.makeText(getActivity(), "Audiences returned in: " + (System.currentTimeMillis() - time) + " milliseconds.", Toast.LENGTH_SHORT ).show();
        StringBuilder builder = new StringBuilder();
        for (Audience audience : audienceMembership.getAudiences()){
            builder.append(audience.toString());
            builder.append("\n");
        }
        audienceList.setAdapter(new ArrayAdapter<Audience>(getActivity(), android.R.layout.simple_list_item_1, audienceMembership.getAudiences()));
    }

    @Override
    public void onClick(View v) {
        time = System.currentTimeMillis();
        Editable text = endpointText.getText();
        MParticle.getInstance().getUserAudiences(seekbar.getProgress(), text != null ? text.toString() : null, this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        timeoutTextView.setText("Timeout: " + Integer.toString(progress) + " milliseconds");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
