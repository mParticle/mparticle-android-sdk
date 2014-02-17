package com.mparticle.particlebox;

import android.view.View;
import android.widget.Toast;


/**
 * Created by sdozor on 1/22/14.
 */
public class TransactionTestFragment extends MainTransactionTestFragment implements View.OnClickListener {
    public TransactionTestFragment() {
        super();
    }


    @Override
    public void onClick(View v) {
        super.onClick(v);
        Toast.makeText(v.getContext(), "Not implemented for Bugsense...", Toast.LENGTH_SHORT).show();
    }
}
