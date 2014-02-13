package com.mparticle.particlebox;

import android.view.View;
import android.widget.Toast;

import com.urbanairship.push.PushManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sdozor on 1/22/14.
 */
public class AttributeTestFragment extends MainAttributeTestFragment implements View.OnClickListener {

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.button:
                Toast.makeText(v.getContext(), "Not implemented for Urban Airship.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button2:
                Set<String> tags = new HashSet<String>();
                tags.add(editText3.getText().toString());
                PushManager.shared().setTags(tags);
                break;
            case R.id.button3:
                PushManager.shared().setAlias(editText4.getText().toString());
                Toast.makeText(v.getContext(), "UA user alias set.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
