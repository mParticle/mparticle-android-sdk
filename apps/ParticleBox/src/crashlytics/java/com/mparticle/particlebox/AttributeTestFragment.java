package com.mparticle.particlebox;

import android.view.View;
import android.widget.Toast;

/**
 * Created by sdozor on 1/22/14.
 */
public class AttributeTestFragment extends MainAttributeTestFragment implements View.OnClickListener {


    @Override
    public void onClick(View v) {
        Toast.makeText(v.getContext(), "Not implemented for Crashlytics...", Toast.LENGTH_SHORT).show();
        switch (v.getId()) {
            case R.id.button:
                //
                break;
            case R.id.button2:
                //
                break;
        }
    }
}
