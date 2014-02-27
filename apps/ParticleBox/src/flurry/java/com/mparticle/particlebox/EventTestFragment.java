package com.mparticle.particlebox;

import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
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
                Map<String, String> articleParams = new HashMap<String, String>();
                articleParams.put("Category", spinner.getSelectedItem().toString()); // Capture author info
                articleParams.put("Label", eventLabel.getText().toString()); // Capture user status=
                articleParams.put("Value", eventValue.getText().toString());
                final String name = viewEditText.getText().toString();
                FlurryAgent.logEvent(name, articleParams);
                break;
            case R.id.button2:
                FlurryAgent.onPageView();
                FlurryAgent.logEvent(screenEditText.getText().toString());
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
                    FlurryAgent.onError(Integer.toString(errorEditText.hashCode()), errorEditText.getText().toString(), e);
                }

                break;
            case R.id.button5:
                Map<String, String> articleParams2 = new HashMap<String, String>();
                articleParams2.put("Category", timingCategory.getSelectedItem().toString()); // Capture author info
                articleParams2.put("Label", timingLabel.getText().toString()); // Capture user status=
                articleParams2.put("Value", eventValue.getText().toString());
                final String named = timingTitle.getText().toString();
                FlurryAgent.logEvent(named, articleParams2);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        FlurryAgent.endTimedEvent(named);
                    }
                }, Long.parseLong(timingLength.getText().toString()));
                break;
            case R.id.breadcrumbButton:
                Toast.makeText(v.getContext(), "Not implemented for Flurry...", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
