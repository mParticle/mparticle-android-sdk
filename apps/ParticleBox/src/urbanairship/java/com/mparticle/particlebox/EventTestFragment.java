package com.mparticle.particlebox;

import android.view.View;
import android.widget.Toast;

/**
 * Created by sdozor on 1/7/14.
 */
public class EventTestFragment extends MainEventTestFragment implements View.OnClickListener {


    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.button:
            case R.id.button2:
            case R.id.button3:
            case R.id.button5:
                Toast.makeText(v.getContext(), "Not implemented for Urban Airship.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
