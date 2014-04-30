package com.mparticle.particlebox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;


/**
 * Created by sdozor on 1/7/14.
 */
public class EventTestFragment extends MainEventTestFragment implements View.OnClickListener {

    private EasyTracker tracker;

    private static final String longName = "Zombie ipsum reversus ab viral inferno, nam rick grimes malum cerebro. De carne lumbering animata corpora quaeritis. Summus brains sit​​, morbo vel maleficia? De apocalypsi gorger omero undead survivor dictum mauris. Hi mindless mortuis soulless creaturas, imo evil stalking monstra adventus resi dentevil vultus comedat cerebella viventium. Qui animated corpse, cricket bat max brucks terribilem incessu zomby. The voodoo sacerdos flesh eater, suscitat mortuos comedere carnem virus. Zonbi tattered for solum oculi eorum defunctis go lum cerebro. Nescio brains an Undead zombies. Sicut malus putrid voodoo horror. Nigh tofth eliv ingdead.Zombie ipsum reversus ab viral inferno, nam rick grimes malum cerebro. De carne lumbering animata corpora quaeritis. Summus brains sit​​, morbo vel maleficia? De apocalypsi gorger omero undead survivor dictum mauris. Hi mindless mortuis soulless creaturas, imo evil stalking monstra adventus resi dentevil vultus comedat cerebella viventium. Qui animated corpse, cricket bat max brucks terribilem incessu zomby. The voodoo sacerdos flesh eater, suscitat mortuos comedere carnem virus. Zonbi tattered for solum oculi eorum defunctis go lum cerebro. Nescio brains an Undead zombies. Sicut malus putrid voodoo horror. Nigh tofth eliv ingdead.Zombie ipsum reversus ab viral inferno, nam rick grimes malum cerebro. De carne lumbering animata corpora quaeritis. Summus brains sit​​, morbo vel maleficia? De apocalypsi gorger omero undead survivor dictum mauris. Hi mindless mortuis soulless creaturas, imo evil stalking monstra adventus resi dentevil vultus comedat cerebella viventium. Qui animated corpse, cricket bat max brucks terribilem incessu zomby. The voodoo sacerdos flesh eater, suscitat mortuos comedere carnem virus. Zonbi tattered for solum oculi eorum defunctis go lum cerebro. Nescio brains an Undead zombies. Sicut malus putrid voodoo horror. Nigh tofth eliv ingdead.Zombie ipsum reversus ab viral inferno, nam rick grimes malum cerebro. De carne lumbering animata corpora quaeritis. Summus brains sit​​, morbo vel maleficia? De apocalypsi gorger omero undead survivor dictum mauris. Hi mindless mortuis soulless creaturas, imo evil stalking monstra adventus resi dentevil vultus comedat cerebella viventium. Qui animated corpse, cricket bat max brucks terribilem incessu zomby. The voodoo sacerdos flesh eater, suscitat mortuos comedere carnem virus. Zonbi tattered for solum oculi eorum defunctis go lum cerebro. Nescio brains an Undead zombies. Sicut malus putrid voodoo horror. Nigh tofth eliv ingdead.";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        tracker = EasyTracker.getInstance(activity);
    }

    @Override
    public void onClick(View v) {
       // super.onClick(v);

        switch (v.getId()) {
            case R.id.button:
                tracker.send(MapBuilder.createEvent(spinner.getSelectedItem().toString(),
                        viewEditText.getText().toString(),
                        "label", null).
                        build());
              //  GoogleAnalytics.getInstance(getActivity().getBaseContext()).dispatchLocalHits();
                break;
            case R.id.button2:
                if (tracker.get(Fields.SCREEN_NAME) == null || tracker.get(Fields.SCREEN_NAME).length() < 100){
                    tracker.set(Fields.SCREEN_NAME, longName);
                    Toast.makeText(getActivity(), "POST ENABLED.", Toast.LENGTH_SHORT).show();
                }else{
                    tracker.set(Fields.SCREEN_NAME, "Short Particle box screen");
                    Toast.makeText(getActivity(), "GET ENABLED.", Toast.LENGTH_SHORT).show();
                }

                tracker.send(MapBuilder
                                .createAppView()
                                .build()
                );
                break;
            case R.id.button3:
                tracker.send(MapBuilder
                        .createException(new StandardExceptionParser(getActivity(), null)              // Context and optional collection of package names
                                        // to be used in reporting the exception.
                                        .getDescription(Thread.currentThread().getName(),    // The name of the thread on which the exception occurred.
                                                new NullPointerException()),                                  // The exception.
                                false
                        )                                               // False indicates a fatal exception
                        .build());
                break;
            case R.id.button5:

                tracker.send(MapBuilder
                                .createTiming("resources",    // Timing category (required)
                                        Long.parseLong(timingLength.getText().toString()),       // Timing interval in milliseconds (required)
                                        "high scores",  // Timing name
                                        null)           // Timing label
                                .build()
                );
                break;
        }
    }
}
