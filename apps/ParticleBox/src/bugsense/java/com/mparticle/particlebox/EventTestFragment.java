package com.mparticle.particlebox;

import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.mparticle.MParticle;

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
            case R.id.button2:
            case R.id.button5:
                Toast.makeText(v.getContext(), "Not implemented for bugsense.", Toast.LENGTH_SHORT).show();
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
                    HashMap<String, String> attributes = new HashMap<String, String>();
                    attributes.put("Attribute", "Some Attribute");
                    MParticle.getInstance().logException(e, attributes, errorEditText.getText().toString());
                    BugSenseHandler.sendExceptionMap(attributes,e);
                }
                break;
        }
    }
}
