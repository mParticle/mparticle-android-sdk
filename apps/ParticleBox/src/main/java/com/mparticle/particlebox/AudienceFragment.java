package com.mparticle.particlebox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mparticle.AudienceListener;
import com.mparticle.MParticle;
import com.mparticle.com.mparticle.audience.Audiences;

/**
 * Created by sdozor on 2/25/14.
 */
public class AudienceFragment extends Fragment implements AudienceListener, View.OnClickListener {
    private static final String ARG_SECTION_NUMBER = "section_number";
    TextView audienceTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.audience_fragment, container, false);
        audienceTextView = (TextView)v.findViewById(R.id.audiences);
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
    public void onAudiencesRetrieved(Audiences audiences) {
        audienceTextView.setText(audiences.toString());
    }

    @Override
    public void onClick(View v) {
        MParticle.getInstance().getUserAudiences(1000, null, this);
    }
}
