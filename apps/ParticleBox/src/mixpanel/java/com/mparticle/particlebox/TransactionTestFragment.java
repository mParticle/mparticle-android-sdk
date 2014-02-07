package com.mparticle.particlebox;

import android.view.View;
import android.widget.EditText;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

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

        EasyTracker tracker = EasyTracker.getInstance(v.getContext());
        tracker.send(MapBuilder
                .createTransaction(transactionId.getText().toString(),
                        transactionAffiliation.getText().toString(),
                        Double.parseDouble(revenueAmount.getText().toString()),
                        Double.parseDouble(taxAmount.getText().toString()),
                        Double.parseDouble(shippingAmount.getText().toString()),
                        currencyCode.getText().toString()).
                        build());
    }
}
